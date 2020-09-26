(ns mount.lite
  "The core namespace providing the public API"
  {:clojure.tools.namespace.repl/load   false
   :clojure.tools.namespace.repl/unload false}
  (:require [mount.utils :as utils])
  (:import [clojure.lang IDeref IRecord]
           [java.util Map]))

;;;; Internals.

;;; The state protocol.

(defprotocol ^:no-doc IState
  (start* [_])
  (stop* [_])
  (status* [_])
  (properties [_]))

;;; The state protocol implementation.

(defn new-session
  []
  (gensym "mount-session-"))

(defonce ^:private default-session (new-session))

(defonce ^:dynamic *session* default-session)

(defn- default-session?
  []
  (= *session* default-session))

(defn- throw-started
  [name]
  (throw (Error. (format "state %s already started %s" name
                         (if (default-session?) "" "in this session")))))

(defn- throw-unstarted
  [name]
  (throw (Error. (format "state %s not started %s" name
                         (if (default-session?) "" "in this session")))))

(defn- throw-not-found
  [var]
  (throw (Error. (format "var %s is not a state" var))))

(defrecord State [start-fn stop-fn name sessions]
  IState
  (start* [this]
    (if (= :stopped (status* this))
      (let [value (start-fn)]
        (swap! sessions assoc *session* (assoc (dissoc this :sessions) ::value value)))
      (throw-started name)))

  (stop* [this]
    (let [value   (deref this)
          stop-fn (get-in @sessions [*session* :stop-fn])]
      (stop-fn value)
      (swap! sessions dissoc *session*)))

  (status* [_]
    (if (get @sessions *session*)
      :started
      :stopped))

  (properties [this]
    (-> this
        (merge (get @sessions *session*))
        (dissoc ::value :sessions)))

  IDeref
  (deref [this]
    (if (= :started (status* this))
      (get-in @sessions [*session* ::value])
      (throw-unstarted name))))

(prefer-method print-method Map IDeref)
(prefer-method print-method IRecord IDeref)
(alter-meta! #'->State assoc :private true)
(alter-meta! #'map->State assoc :private true)

;;; Utility functions

(defn- var-status=
  [status]
  (fn [var]
    (= (-> var deref status*) status)))

(defn- prune-states
  [states]
  (filter utils/resolve-keyword states))


;;; Global state.

(defonce
  ^{:dynamic true
    :doc "Atom keeping track of defined states (by namespaced
    keywords) internally. Declared public and dynamic here, as an
    extension point to influence which states are started or stopped.
    Do not fiddle with the root binding."}
  *states* (atom ()))

(defonce
  ^{:dynamic true
    :doc "Can be bound to a map with vars as keys and State records as
    values. The :start and :stop expressions of the State value will
    be used when the corresponding var key is started. The
    `with-substitutes` macro offers a nicer syntax."}
  *substitutes* nil)


;;;; Public API

(defmacro state
  "Create an anonymous state, useful for substituting. Supports three
  keyword arguments. A required :start expression, an optional :stop
  expression, and an optional :name for the state."
  [& {:keys [start stop name] :or {name "-anonymous-"} :as fields}]
  (if (contains? fields :start)
    `(#'map->State (merge ~(dissoc fields :start :stop :name)
                          {:start-fn (fn [] ~start)
                           :stop-fn  (fn [~'this] ~stop)
                           :name     ~name}))
    (throw (ex-info "missing :start expression" {}))))

(defmacro defstate
  "Define a state. At least a :start expression should be supplied.
  Optionally one can define a :stop expression. Supports docstring and
  attribute map."
  [name & args]
  (let [[name args] (utils/name-with-attrs name args)
        current     (resolve name)]
    `(do (defonce ~name (#'map->State {:sessions (atom nil)}))
         (let [local# (state :name ~(str name) ~@args)
               var#   (var ~name)
               kw#    (utils/var->keyword var#)]
           (alter-var-root var# (fn [{sessions# :sessions}]
                                  (assoc local# :sessions sessions#)))
           (swap! *states* #(distinct (concat % [kw#])))
           var#))))

(defn start
  "Start all the loaded defstates, or only the defstates up to the
  given state var. Only stopped defstates are started. They are
  started in the context of the current session."
  ([]
   (start nil))
  ([up-to-var]
   (let [states (map utils/resolve-keyword (swap! *states* prune-states))]
     (when-let [up-to (or up-to-var (last states))]
       (if-let [index (utils/find-index up-to states)]
         (let [vars (->> states (take (inc index)) (filter (var-status= :stopped)))]
           (doseq [var vars]
             (let [substitute (some-> (get *substitutes* var) (dissoc :sessions))
                   state      (merge @var substitute)]
               (try
                 (start* state)
                 (catch Throwable t
                   (throw (ex-info (format "error while starting state %s" var)
                                   {:var var} t))))))
           vars)
         (throw-not-found up-to-var))))))

(defn stop
  "Stop all the loaded defstates, or only the defstates down to the
  given state var. Only started defstates are stopped. They are
  stopped in the context of the current session."
  ([]
   (stop nil))
  ([down-to-var]
   (let [states  (map utils/resolve-keyword (swap! *states* prune-states))]
     (when-let [down-to (or down-to-var (first states))]
       (if-let [index (utils/find-index down-to states)]
         (let [vars (->> states (drop index) (filter (var-status= :started)) (reverse))]
           (doseq [var vars]
             (try
               (stop* @var)
               (catch Throwable t
                 (throw (ex-info (format "error while stopping state %s" var)
                                 {:var var} t)))))
           vars)
         (throw-not-found down-to-var))))))

(defn status
  "Retrieve status map for all states."
  []
  (let [vars (map utils/resolve-keyword (swap! *states* prune-states))]
    (reduce (fn [m v]
              (assoc m v (-> v deref status*)))
            {} vars)))

(defmacro with-substitutes
  "Given a vector with var-state pairs, an inner start function will
  use the :start expression of the substitutes for the specified
  vars. Nested `with-substitutes` are merged."
  [var-sub-pairs & body]
  `(let [merged# (merge *substitutes* (apply hash-map ~var-sub-pairs))]
     (binding [*substitutes* merged#]
       ~@body)))

(defn with-session*
  "Creates a new binding context with a new system of states. All states are
  initially in the stopped status in this context, regardless of the status of
  the states in the caller. This new session will be automatically conveyed to
  any futures or agents spawned within it; to ensure that the session is
  propagated to any _threads_ spawned within this session, use
  clojure.core/bound-fn or clojure.core/bound-fn* when creating the thread."
  [f]
  (binding [*session* (new-session)]
    (f)))

(defmacro with-session
  "Creates a new binding context with a new system of states. All states are
  initially in the stopped status in this context, regardless of the status of
  the states in the caller. This new session will be automatically conveyed to
  any futures or agents spawned within it; to ensure that the session is
  propagated to any _threads_ spawned within this session, use
  clojure.core/bound-fn or clojure.core/bound-fn* when creating the thread."
  [& body]
  `(with-session* (fn [] ~@body)))
