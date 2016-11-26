(ns mount.lite
  "The core namespace providing the public API"
  (:import [clojure.lang IDeref]
           [java.util Map]))


;;; Private logic

;; (defonce ^:private on-reload-override (volatile! nil))

;; ;;; Reloading.

;; (defmulti ^:no-doc do-on-reload
;;   (fn [var] (or @on-reload-override (-> var meta ::current :on-reload) :cascade)))

;; (defmethod do-on-reload :stop [var]
;;   (stop (only var))
;;   :stopped)

;; (defmethod do-on-reload :cascade [var]
;;   (let [up-to' (filtered-vars :started {:up-to var})
;;         only'  (remove #(= (-> % meta ::current :on-cascade) :skip) up-to')]
;;     (stop {:only (conj only' var)}))
;;   :stopped)

;; (defmethod do-on-reload :lifecycle [var]
;;   (-> var meta ::status))

;; (defn on-reload
;;   "Get or set the on-reload override configuration. Default is nil,
;;   meaning the defstates themselves determine how they should respond
;;   to a redefinition. Default for defstates is :cascade, meaning all
;;   states `up-to` the reloaded state (inclusive) are stopped (except
;;   those with :on-cascade set to :skip)."
;;   ([] @on-reload-override)
;;   ([val] (vreset! on-reload-override val)))


;; ;;; Defining states.

;; (defmacro state
;;   "Make a state definition, useful for making test or mock states. Use with
;;   substitute function or :substitute key in start info. This is a convenience
;;   macro; a plain state-map can be used in the :substitute start option as well.

;;   Note that the following does not define a state var, and won't be recognized by
;;   start or stop: (def foo (state ...))."
;;   {:arglists '([& {:keys [start stop on-reload on-cascade]}])}
;;   [& {:keys [start stop bindings on-reload on-cascade] :as body :or {bindings []}}]
;;   (assert (contains? body :start) "state must contain a :start expression")
;;   (assert (vector? bindings) "bindings must be vector")
;;   (assert (even? (count bindings)) "bindings must have even number of elems")
;;   (let [valid-on-reload (set (keys (methods do-on-reload)))]
;;     (assert (or (nil? on-reload) (valid-on-reload on-reload))
;;             (str ":on-reload must be nil or in " valid-on-reload)))
;;   (assert (or (nil? on-cascade) (= on-cascade :skip)) ":on-cascade must be nil or :skip")
;;   (let [syms (mapv first (partition 2 bindings))]
;;     `{:start (fn [boundp# [& {:syms ~syms :or ~(apply hash-map bindings)}]]
;;                (deliver boundp# (vec (apply concat (zipmap '~syms ~syms))))
;;                                   ~start)
;;       :stop  (fn [[& {:syms ~syms :or ~(apply hash-map bindings)}]] ~stop)
;;       :on-reload  ~on-reload
;;       :on-cascade ~on-cascade
;;       :mount.lite/bindings-form '~bindings}))

;; (defmacro defstate
;;   "Define a state. At least a :start expression should be supplied.
;;    Optionally one can define a :stop expression."
;;   {:arglists '([name doc-string? attr-map? bindings? & {:as state-map}])}
;;   [name & args]
;;   (let [[name args] (name-with-attrs name args)
;;         body        (if (vector? (first args))
;;                       (apply hash-map :bindings (first args) (next args))
;;                       (apply hash-map args))
;;         current     (resolve name)]
;;     `(let [status# ~(if current
;;                       (if (-> current meta ::order)
;;                         `(do-on-reload ~current)
;;                         `(throw (ex-info (str "Cannot define defstate for existing var that is not "
;;                                               "a defstate") {:var ~current})))
;;                       :stopped)
;;            meta#   ~(if current
;;                       `(select-keys (meta ~current) [::order ::current])
;;                       `{::order (swap! @#'order + 10)})]
;;        ~(if current
;;           (when *compile-files*
;;             (throw (ex-info (str "Compiling already loaded defstate. "
;;                                  "Make sure user.clj is excluded from your build.")
;;                             {:var current})))
;;           `(defonce ~name (Unstarted. (var ~name))))
;;        (alter-meta! (var ~name) merge (state ~@(apply concat body)) meta#
;;                     {::status status# :redef true})
;;        (var ~name))))


;;; Internals

(defprotocol IState
  (start* [_])
  (stop* [_])
  (status* [_]))

(defn- throw-started
  [name]
  (throw (ex-info (format "state %s already started" name)
                  {:name name})))

(defn- throw-unstarted
  [name]
  (throw (ex-info (format "state %s not started (in this thread or parent threads)" name)
                  {:name name})))

(defn- throw-not-found
  [var]
  (throw (ex-info (format "var %s is not a state" var)
                  {:var var})))

(defrecord State [start-fn stop-fn name itl]
  IState
  (start* [this]
    (if (= :stopped (status* this))
      (.set itl (volatile! {::value   (start-fn)
                            ::stop-fn stop-fn}))
      (throw-started name)))

  (stop* [this]
    (if (= :started (status* this))
      (let [m (.get itl)]
        ((::stop-fn @m))
        (vreset! m nil))
      (throw-unstarted name))
    (.set itl nil))

  (status* [_]
    (let [m (.get itl)]
      (if (and m @m)
        :started
        :stopped)))

  IDeref
  (deref [this]
    (if (= :started (status* this))
      (::value @(.get itl))
      (throw-unstarted name))))

(prefer-method print-method Map IDeref)
(alter-meta! #'->State assoc :private true)
(alter-meta! #'map->State assoc :private true)

;;---TODO Replace this with clojure.spec?
(defn- name-with-attrs
  [name [arg1 arg2 & argx :as args]]
  (let [[attrs args] (cond (and (string? arg1) (map? arg2)) [(assoc arg2 :doc arg1) argx]
                           (string? arg1)                   [{:doc arg1} (cons arg2 argx)]
                           (map? arg1)                      [arg1 (cons arg2 argx)]
                           :otherwise                       [{} args])]
    [(with-meta name (merge (meta name) attrs)) args]))

(defn- var-status=
  [status]
  (fn [var]
    (= (-> var deref status*) status)))

(defn- find-index
  [elem coll]
  (first (keep-indexed (fn [i e]
                         (when (= e elem)
                           i))
                       coll)))

(defonce ^:private states (atom []))

(def ^:dynamic *substitutes* nil)


;;; Public API

(defmacro state
  "Create an anonymous state, useful for substituting. Supports three
  keyword arguments. A required :start expression, an optional :stop
  expression, and an optional :name for the state."
  [& {:keys [start stop name] :or {name "-anonymous-"}}]
  (if start
    `(#'map->State {:start-fn (fn [] ~start)
                    :stop-fn  (fn [] ~stop)
                    :itl      (InheritableThreadLocal.)
                    :name     ~name})
    (throw (ex-info "missing :start expression" {}))))

(defmacro defstate
  "Define a state. At least a :start expression should be supplied.
  Optionally one can define a :stop expression. Supports docstring and
  attribute map."
  [name & args]
  (let [[name args] (name-with-attrs name args)

        current     (resolve name)]
    (when (and current *compile-files*)
      ;;---TODO Check if this is still a problem
      (throw (ex-info (str "Compiling already loaded defstate. "
                           "Make sure user.clj is excluded from your build.")
                      {:var current})))
    ;;---TODO Add reloading behaviour
    `(do (defonce ~name (#'map->State {:itl (InheritableThreadLocal.)}))
         (let [local# (state ~@(concat [:name (str name)] args))]
           (alter-var-root (var ~name) merge (dissoc local# :itl)))
         (swap! @#'states #(vec (distinct (conj % (var ~name)))))
         (var ~name))))

(defn start
  "Start all the loaded defstates, or only the defstates up to the
  given state var. Only stopped defstates are started, and they are
  started in the context of the current thread."
  ([]
   (start (last @states)))
  ([up-to-var]
   (let [states @states]
     (if-let [index (find-index up-to-var states)]
       (let [vars (->> states (take (inc index)) (filter (var-status= :stopped)))]
         (doseq [var vars]
           (let [substitute (-> (get *substitutes* var)
                                (select-keys [:start-fn :stop-fn]))
                 state      (merge @var substitute)]
             (try
               (start* state)
               (catch Throwable t
                 (throw (ex-info (format "error while starting state %s" var)
                                 {:var var} t))))))
         vars)
       (throw-not-found up-to-var)))))

(defn stop
  "Stop all the loaded defstates, or only the defstates down to the
  given state var. Only started defstates are stopped, and they are
  stopped in the context of the current or parent thread where they
  were started."
  ([]
   (stop (first @states)))
  ([down-to-var]
   (let [states @states]
     (if-let [index (find-index down-to-var states)]
       (let [vars (->> states (drop index) (filter (var-status= :started)) (reverse))]
         (doseq [var vars]
           (try
             (stop* @var)
             (catch Throwable t
               (throw (ex-info (format "error while stopping state %s" var)
                               {:var var} t)))))
         vars)
       (throw-not-found down-to-var)))))

(defmacro with-substitutes
  "Given a vector with var-state pairs, an inner start function will
  use the :start expression of the substitutes for the specified
  vars. Nested `with-substitutes` are merged."
  [var-sub-pairs & body]
  `(let [merged# (merge *substitutes* (apply hash-map ~var-sub-pairs))]
     (binding [*substitutes* merged#]
       ~@body)))

(defn status
  "Retrieve status map for all states, or the given state vars."
  ([]
   (when (seq @states)
     (apply status @states)))
  ([& vars]
   (reduce (fn [m v] (assoc m v (-> v deref status*))) {} vars)))

(defmacro thread
  "Create a new thread and run the body. Useful for starting up
  multiple instances of state systems. Returns a map with the :thread
  and a :promise that will be set to the result of the body."
  [& body]
  `(let [p# (promise)]
     {:thead  (doto (Thread. (fn [] (deliver p# ~@body)))
                (.start))
      :result p#}))
