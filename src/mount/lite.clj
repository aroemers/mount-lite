(ns mount.lite
  "The core namespace providing the public API"
  (:require [clojure.set :as set]
            [clojure.tools.namespace.dependency :as dep]
            [mount.lite.graph :as graph]
            [mount.lite.parallel :as parallel])
  (:import [clojure.lang IDeref]
           [java.util Map]))

;;; Types

(deftype Unstarted [var] Object (toString [_] (str "State " var " is not started.")))

(alter-meta! #'->Unstarted assoc :private true)


;;; Private logic

#_(defonce ^:private order (atom 0))
(defonce ^:private on-reload-override (volatile! nil))
(defonce ^:private log-fn* (volatile! nil))

(defn- start* [var-state-map]
  (let [sorted (sort-by (comp ::order meta key) var-state-map)]
    (doseq [[var' state'] sorted]
      (try
        (if-let [start-fn (:start state')]
          (do (when-let [f @log-fn*] (f var' :starting))
              (alter-var-root var' (constantly (start-fn)))
              (when-let [f @log-fn*] (f var' :started)))
          (throw (IllegalArgumentException. "Missing :start expression.")))
        (catch Throwable t
          (throw (ex-info (str "Error while starting " var' ":") {:var var' :state state'} t))))
      (alter-meta! var' assoc ::status :started)
      (alter-meta! var' update-in [::current] merge state'))
    (map key sorted)))

(defn- stop* [vars]
  (let [sorted-vars (sort-by (comp - ::order meta) vars)]
    (doseq [var' sorted-vars
            :let [state' (meta var')]]
      (when-let [stop-fn (-> state' ::current :stop)]
        (try
          (when-let [f @log-fn*] (f var' :stopping))
          (stop-fn)
          (when-let [f @log-fn*] (f var' :stopped))
          (catch Throwable t
            (throw (ex-info (str "Error while stopping " var' ":") {:var var' :state state'} t)))))
      (alter-var-root var' (constantly (Unstarted. var')))
      (alter-meta! var' assoc ::status :stopped)
      (alter-meta! var' dissoc ::current))
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
    (some :bindings optss)   (assoc :bindings (apply conj {} (mapcat :bindings optss)))
    (some :up-to optss)      (assoc :up-to (last (keep :up-to optss)))
    (some :parallel optss)   (assoc :parallel (last (keep :parallel optss)))))

(defn- filtered-vars [status opts]
  (let [filtered  (-> (set (or (:only opts) (all-states)))
                      (set/difference (set (:except opts)))
                      (->> (filter #(= (-> % meta ::status) status))))]
    (if-let [upto (:up-to opts)]
      (let [graph (graph/var-graph (conj (set filtered) upto))
            deps (case status
                   :stopped (dep/transitive-dependencies graph upto)
                   :started (dep/transitive-dependents graph upto))]
        (cond-> deps (= (-> upto meta ::status) status) (conj upto)))
      filtered)))

(defn- var-state-map [vars opts]
  (into {} (for [var' vars] [var' (get (:substitute opts) var' (meta var'))])))

(defn- var-bindings-map [vsm opts]
  (let [bindings (:bindings opts)]
    (reduce-kv (fn [m var state]
                 (assoc m var
                        (merge (select-keys state [:on-reload :on-cascade])
                               {:start (let [start-fn (:start state)]
                                         (fn [] (if (::bindings-form state)
                                                  (let [boundp (promise)
                                                        val (start-fn boundp (get bindings var))]
                                                    (alter-meta! var assoc-in [::current :bindings]
                                                                 (when (realized? boundp) @boundp))
                                                    val)
                                                  (start-fn))))
                                :stop  (when-let [stop-fn (:stop state)]
                                         (fn [] (stop-fn (get bindings var))))})))
               {} vsm)))

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

(defn bindings
  "Creates or updates start option map, supplying binding vectors for
  the given defstate vars. Make sure the symbols in the binding
  vectors are quoted. Multiple uses of this function on the same
  option map are merged."
  {:arglists '([& var-binding-seq] [opts & var-binding-seq])}
  [& [opts-or-var & var-binding-seq]]
  (let [[opts var-binding-seq] (if (var? opts-or-var)
                                 [{} (cons opts-or-var var-binding-seq)]
                                 [opts-or-var var-binding-seq])]
    (let [bindings (apply hash-map var-binding-seq)]
      (update-in opts [:bindings] merge bindings))))

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

  :bindings - A map of defstate vars to binding maps. Multiples of
              these maps are merged.

  These option maps are easily created using the only, except, up-to and
  substitute functions."
  [& optss]
  (let [opts (merge-opts optss)
        vars (filtered-vars :stopped opts)
        vsm  (var-state-map vars opts)
        vbm  (var-bindings-map vsm opts)]
    (if-let [threads (:parallel opts)]
      (parallel/start vars #(start* {% (get vbm %)}) threads)
      (start* vbm))))

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

(defn dot
  "Retuns a Graphviz dot representation of the state dependencies.
  Currently no options available."
  [& {:as options}]
  (let [graph (graph/var-graph (all-states))
        builder (StringBuilder. "digraph {\n")]
    (doseq [state (dep/nodes graph)
            dep (dep/immediate-dependencies graph state)]
      (.append builder (str "  \"" (subs (str state) 2) "\" -> \"" (subs (str dep) 2) "\";\n")))
    (.append builder "}")
    (str builder)))


;;; Reloading.

(defmulti ^:no-doc do-on-reload
  (fn [var] (or @on-reload-override (-> var meta ::current :on-reload) :cascade)))

(defmethod do-on-reload :stop [var]
  (stop (only var))
  :stopped)

(defmethod do-on-reload :cascade [var]
  (let [up-to' (filtered-vars :started {:up-to var})
        only'  (remove #(= (-> % meta ::current :on-cascade) :skip) up-to')]
    (stop {:only (conj only' var)}))
  :stopped)

(defmethod do-on-reload :lifecycle [var]
  (-> var meta ::status))

(defn on-reload
  "Get or set the on-reload override configuration. Default is nil,
  meaning the defstates themselves determine how they should respond
  to a redefinition. Default for defstates is :cascade, meaning all
  states `up-to` the reloaded state (inclusive) are stopped (except
  those with :on-cascade set to :skip)."
  ([] @on-reload-override)
  ([val] (vreset! on-reload-override val)))

(defn log-fn
  "Get or set a log function, which is called whenever the status changes of a var.
  The function receives a var and a keyword, where the latter is one
  of :starting, :started, :stopping or :stopped. Default is nil, meaning no log
  function will be called."
  ([] @log-fn*)
  ([val] (vreset! log-fn* val)))


;;; Defining states.

(defmacro state
  "Make a state definition, useful for making test or mock states. Use with
  substitute function or :substitute key in start info. This is a convenience
  macro; a plain state-map can be used in the :substitute start option as well.

  Note that the following does not define a state var, and won't be recognized by
  start or stop: (def foo (state ...))."
  {:arglists '([& {:keys [start stop on-reload on-cascade]}])}
  [& {:keys [start stop bindings on-reload on-cascade] :as body :or {bindings []}}]
  (assert (contains? body :start) "state must contain a :start expression")
  (assert (vector? bindings) "bindings must be vector")
  (assert (even? (count bindings)) "bindings must have even number of elems")
  (let [valid-on-reload (set (keys (methods do-on-reload)))]
    (assert (or (nil? on-reload) (valid-on-reload on-reload))
            (str ":on-reload must be nil or in " valid-on-reload)))
  (assert (or (nil? on-cascade) (= on-cascade :skip)) ":on-cascade must be nil or :skip")
  (let [syms (mapv first (partition 2 bindings))]
    `{:start (fn [boundp# [& {:syms ~syms :or ~(apply hash-map bindings)}]]
               (deliver boundp# (vec (apply concat (zipmap '~syms ~syms))))
                                  ~start)
      :stop  (fn [[& {:syms ~syms :or ~(apply hash-map bindings)}]] ~stop)
      :on-reload  ~on-reload
      :on-cascade ~on-cascade
      :mount.lite/bindings-form '~bindings}))

(defmacro defstate
  "Define a state. At least a :start expression should be supplied.
   Optionally one can define a :stop expression."
  {:arglists '([name doc-string? attr-map? bindings? & {:as state-map}])}
  [name & args]
  (let [[name args] (name-with-attrs name args)
        body        (if (vector? (first args))
                      (apply hash-map :bindings (first args) (next args))
                      (apply hash-map args))
        current     (resolve name)]
    `(let [status# ~(if current
                      (if (-> current meta ::order)
                        `(do-on-reload ~current)
                        `(throw (ex-info (str "Cannot define defstate for existing var that is not "
                                              "a defstate") {:var ~current})))
                      :stopped)
           meta#   ~(if current
                      `(select-keys (meta ~current) [::order ::current])
                      `{::order (swap! @#'order + 10)})]
       ~(if current
          (when *compile-files*
            (throw (ex-info (str "Compiling already loaded defstate. "
                                 "Make sure user.clj is excluded from your build.")
                            {:var current})))
          `(defonce ~name (Unstarted. (var ~name))))
       (alter-meta! (var ~name) merge (state ~@(apply concat body)) meta#
                    {::status status# :redef true})
       (var ~name))))


;;; Internals

(defonce ^:private states (atom []))

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

(defrecord State [start-fn stop-fn name itl]
  IState
  (start* [this]
    (if (= :stopped (status* this))
      (.set itl {::value   (start-fn)
                 ::stop-fn stop-fn})
      (throw-started name)))
  (stop* [this]
    (if (= :started (status* this))
      ((::stop-fn (.get itl)))
      (throw-unstarted name))
    (.set itl nil))

  (status* [_]
    (if (.get itl)
      :started
      :stopped))

  IDeref
  (deref [this]
    (if (= :started (status* this))
      (::value (.get itl))
      (throw-unstarted name))))

(prefer-method print-method Map IDeref)


;;; Public API

(defmacro state
  [& {:keys [start stop name]
      :or   {name "-anonymous-"}}]
  `(map->State {:start-fn (fn [] ~start)
                :stop-fn  (fn [] ~stop)
                :itl      (InheritableThreadLocal.)
                :name     ~name}))

(defmacro defstate
  [name & args]
  (let [current (resolve name)]
    (when (and current *compile-files*)
      ;;---TODO Check if this is still a problem
      (throw (ex-info (str "Compiling already loaded defstate. "
                           "Make sure user.clj is excluded from your build.")
                      {:var current})))
    `(do (defonce ~name (map->State {:itl (InheritableThreadLocal.)}))
         (let [local# (state ~@(concat [:name (str name)] args))]
           (alter-var-root (var ~name) merge (dissoc local# :itl)))
         (swap! @#'states #(vec (distinct (conj % (var ~name)))))
         (var ~name))))

(defn start
  []
  (let [vars @states]
    (doseq [var   vars
            :let  [state @var]
            :when (= :stopped (status* state))]
      (start* state)
      vars)))

(defn stop
  []
  (let [vars (reverse @states)]
    (doseq [var   vars
            :let  [state @var]
            :when (= :started (status* state))]
      (stop* state)
      vars)))

(defn status
  "Retrieve status map for all states, or the given state vars."
  ([]
   (when (seq @states)
     (apply status @states)))
  ([& vars]
   (reduce (fn [m v] (assoc m v (-> v deref status*))) {} vars)))
