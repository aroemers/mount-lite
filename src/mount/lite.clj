(ns mount.lite
  "The core namespace providing the public API"
  (:import [clojure.lang IDeref IRecord]
           [java.util Map]))


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
(prefer-method print-method IRecord IDeref)
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
   (when-let [s (seq @states)]
     (apply status s)))
  ([& vars]
   (reduce (fn [m v] (assoc m v (-> v deref status*))) {} vars)))

(defmacro thread
  "Create a new thread and run the body. Useful for starting up
  multiple instances of state systems. Returns a map with the :thread
  and a :promise that will be set to the result of the body."
  [& body]
  `(let [p# (promise)]
     {:thead  (doto (Thread. (fn [] (deliver p# (do ~@body))))
                (.start))
      :result p#}))
