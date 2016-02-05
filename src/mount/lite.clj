(ns mount.lite
  "The core namespace providing the public API"
  (:require [clojure.set :as set]
            [clojure.tools.namespace.dependency :as dep]
            [mount.lite.graph :as graph]
            [mount.lite.parallel :as parallel]))

;;; Types

(deftype Unstarted [var] Object (toString [_] (str "State " var " is not started.")))

(alter-meta! #'->Unstarted assoc :private true)


;;; Private logic

(defonce ^:private order (atom 0))
(defonce ^:private on-reload* (atom :cascade))

(defmulti ^:no-doc do-on-reload
  (fn [var] @on-reload*))

(defn- defstate* [sym]
  (let [current (resolve sym)
        status  (if current (do-on-reload current) :stopped)
        meta'   (if current
                  (select-keys (meta current) [::order ::current-stop])
                  {::order (swap! order + 10)})
        var     (or current (intern *ns* sym))]
    (doto var
      (reset-meta! (merge meta' (meta sym) {::status status :redef true}))
      (alter-var-root (constantly (if current (deref current) (Unstarted. var)))))))

(defn- start* [var-state-map]
  (let [sorted (sort-by (comp ::order meta key) var-state-map)]
    (doseq [[var' state'] sorted]
      (try
        (if-let [start-fn (:start state')]
          (alter-var-root var' (constantly (start-fn)))
          (throw (IllegalArgumentException. "Missing :start expression.")))
        (catch Throwable t
          (throw (ex-info (str "Error while starting " var' ":") {:var var' :state state'} t))))
      (alter-meta! var' assoc ::status :started ::current-stop (:stop state')))
    (map key sorted)))

(defn- stop* [vars]
  (let [sorted-vars (sort-by (comp - ::order meta) vars)]
    (doseq [var' sorted-vars
            :let [state' (meta var')]]
      (when-let [stop-fn (::current-stop state')]
        (try
          (stop-fn)
          (catch Throwable t
            (throw (ex-info (str "Error while stopping " var' ":") {:var var' :state state'} t)))))
      (alter-var-root var' (constantly (Unstarted. var')))
      (alter-meta! var' assoc ::status :stopped)
      (alter-meta! var' dissoc ::current-stop))
    sorted-vars))

(def ^:private all-states
  (let [ignore (-> (into #{} (map find-ns)
                         '[clojure.core clojure.data clojure.edn clojure.inspector clojure.instant
                           clojure.java.browse clojure.java.io clojure.java.javadoc clojure.java.shell
                           clojure.main clojure.pprint clojure.reflect clojure.repl clojure.set
                           clojure.stacktrace clojure.string clojure.template clojure.test clojure.walk
                           clojure.xml clojure.zip])
                   (set/union (dep/transitive-dependencies (graph/ns-graph) *ns*)))
        xf     (comp (remove ignore) (mapcat ns-interns) (map second) (filter (comp ::order meta)))]
    (fn [] (into #{} xf (all-ns)))))

(defn- merge-opts [optss]
  (cond-> {}
    (some :only optss)       (assoc :only (set (mapcat :only optss)))
    (some :except optss)     (assoc :except (set (mapcat :except optss)))
    (some :substitute optss) (assoc :substitute (apply conj {} (mapcat :substitute optss)))
    (some :up-to optss)      (assoc :up-to (last (keep :up-to optss)))
    (some :parallel optss)   (assoc :parallel (last (keep :parallel optss)))))

(defn- filtered-vars [status opts]
  (let [filtered  (-> (set (or (:only opts) (all-states)))
                      (set/difference (set (:except opts)))
                      (->> (filter #(= (-> % meta ::status) status))))]
    (if-let [upto (:up-to opts)]
      (let [graph (graph/var-graph filtered)]
        (conj (case status
                :stopped (dep/transitive-dependencies graph upto)
                :started (dep/transitive-dependents graph upto))
              upto))
      filtered)))

(defn- var-state-map [vars opts]
  (into {} (for [var' vars] [var' (get (:substitute opts) var' (meta var'))])))

(defn- name-with-attrs [name [arg1 arg2 & argx :as args]]
  (let [[attrs args] (cond (and (string? arg1) (map? arg2)) [(assoc arg2 :doc arg1) argx]
                           (string? arg1)                   [{:doc arg1} (cons arg2 argx)]
                           (map? arg1)                      [arg1 (cons arg2 argx)]
                           :otherwise                       [{} args])]
    [(with-meta name (merge (meta name) attrs)) args]))


;;; Public API

(defn only
  "Creates or updates start/stop option map, only starting or stopping the given
  vars. Multiple uses of this function on the same option map are united."
  {:arglists '([& vars] [opts & vars])}
  [& [opts-or-var & vars]]
  (let [[opts vars] (if (var? opts-or-var)
                      [{} (conj (set vars) opts-or-var)]
                      [opts-or-var (set vars)])]
    (update-in opts [:only] set/union vars)))

(defn except
  "Creates or updates start/stop option map, starting or stopping all defstate
  vars, except the given vars. Multiple uses of this function on the same option map
  are united."
  {:arglists '([& vars] [opts & vars])}
  [& [opts-or-var & vars]]
  (let [[opts vars] (if (var? opts-or-var)
                      [{} (conj (set vars) opts-or-var)]
                      [opts-or-var (set vars)])]
    (update-in opts [:except] set/union vars)))

(defn substitute
  "Creates or updates start option map, supplying substitute states for the given
  defstate vars. Multiple uses of this function on the same option map are merged."
  {:arglists '([& var-state-seq] [opts & var-state-seq])}
  [& [opts-or-var & var-state-seq]]
  (let [[opts var-state-seq] (if (var? opts-or-var)
                               [{} (cons opts-or-var var-state-seq)]
                               [opts-or-var var-state-seq])]
    (let [substitutes (apply hash-map var-state-seq)]
      (update-in opts [:substitute] merge substitutes))))

(defn up-to
  "Creates or updates start/stop option map, starting or stopping only
  those vars that depend on the given var and itself. Multiple uses of
  this function on the same option map are overriding each other."
  {:arglists '([var] [opts var])}
  ([var]
   {:up-to var})
  ([opts var]
   (assoc opts :up-to var)))

(defn parallel
  "Creates or updates start/stop option map, starting or stopping
  independent vars in parallel using the given number of threads.
  Multiple uses of this function on the same option map are overriding
  each other."
  {:arglists '([threads] [opts threads])}
  ([threads]
   {:parallel threads})
  ([opts threads]
   (assoc opts :parallel threads)))

(defn start
  "Start all unstarted states (by default). One or more option maps
  can be supplied. The maps can contain the following keys, applied in
  the following order, and merged as specified:

  :only - Collection of state vars that should be started, when not stopped.
          Multiples of these collections are united.

  :except - Collection of state vars that should not be started. Multiples
            of these are united.

  :up-to - A defstate var until which the states are started. In case multiple
           option maps are supplied, only the last :up-to option is used.

  :parallel - The number of threads to use for parallel starting of the
              states. Default is nil, meaning the current thread will be
              used. In case multiple option maps are supplied, only the
              last :parallel option is used.

  :substitute - A map of defstate vars to state-maps (see state macro) whose
                info is used instead of the defstate vars' state. Multiples
                of these maps are merged.

  These option maps are easily created using the only, except, up-to and
  substitute functions."
  [& optss]
  (let [opts (merge-opts optss)
        vars (filtered-vars :stopped opts)
        vsm  (var-state-map vars opts)]
    (if-let [threads (:parallel opts)]
      (parallel/start vars #(start* {% (get vsm %)}) threads)
      (start* vsm))))

(defn stop
  "Stop all started states (by default). One or more option maps can be
  supplied. These maps are merged. The maps can contain the following keys,
  applied in the following order:

  :only - Collection of state vars that should be stopped, when not started.
          Multiples of these collections are united.

  :except - Collection of state vars that should not be stopped. Multiples
            of these are united.

  :up-to - A defstate var until which the states are stopped. In case multiple
           option maps are supplied, only the last :up-to option is used.

  :parallel - The number of threads to use for parallel stopping of the
              states. Default is nil, meaning the current thread will be
              used. In case multiple option maps are supplied, only the
              last :parallel option is used.

  These option maps are easily created using the only, except and parallel
  functions."
  [& optss]
  (let [opts (merge-opts optss)
        vars (filtered-vars :started opts)]
    (if-let [threads (:parallel opts)]
      (parallel/stop vars #(stop* [%]) threads)
      (stop* vars))))

(defn status
  "Retrieve status map for all states, or the given state vars."
  ([]
   (apply status (all-states)))
  ([& vars]
   (reduce (fn [m v] (assoc m v (-> v meta ::status))) {} vars)))


;;; Reloading.

(defmethod do-on-reload :stop [var]
  (stop (only var))
  :stopped)

(defmethod do-on-reload :cascade [var]
  (stop (up-to var))
  :stopped)

(defmethod do-on-reload :lifecycle [var]
  (-> var meta ::status))

(defn on-reload
  "Get or set the on-reload configuration. Default is :cascade,
  meaning all states `up-to` the reloaded state (inclusive) are
  stopped."
  ([] @on-reload*)
  ([val] (reset! on-reload* val)))


;;; Defining states.

(defmacro state
  "Make a state definition, useful for making test or mock states. Use with
  substitute function or :substitute key in start info. This is a convenience
  macro; a plain state-map can be used in the :substitute start option as well.

  Note that the following does not define a state var, and won't be recognized by
  start or stop: (def foo (state ...))."
  [& {:keys [start stop] :as body}]
  (assert (contains? body :start) "state must contain a :start expression")
  `{:start (fn [] ~start)
    :stop  (fn [] ~stop)})

(defmacro defstate
  "Define a state. At least a :start expression should be supplied.
  Optionally one can define a :stop expression."
  {:arglists '([name doc-string? attr-map? & {:as state-map}])}
  [name & args]
  (let [[name {:as body}] (name-with-attrs name args)
        var'              (defstate* name)]
    `(doto ~var' (alter-meta! merge (state ~@(apply concat body))))))
