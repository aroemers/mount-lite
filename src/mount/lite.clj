(ns mount.lite
  (:require [clojure.set :as set]))

;;; Types

(deftype Unstarted [var] Object (toString [_] (str "State " var " is not started.")))

(alter-meta! #'->Unstarted assoc :private true)


;;; Private logic

(defonce ^:private order (atom 0))

(defn- start* [var-state-map]
  (let [sorted (sort-by (comp ::order meta key) var-state-map)]
    (doseq [[var' state'] sorted]
      (if-let [start-fn (:start state')]
        (alter-var-root var' (constantly (start-fn)))
        (throw (ex-info (str "Missing :start in state or substitution for var " var') state')))
      (alter-meta! var' assoc
                   ::status            :started
                   ::current-stop      (:stop state')
                   ::current-on-reload (:on-reload state' :stop)))
    (map key sorted)))

(defn- stop* [vars]
  (let [sorted-vars (sort-by (comp - ::order meta) vars)]
    (doseq [var' sorted-vars
            :let [state' (meta var')]]
      (when-let [stop-fn (::current-stop state')] (stop-fn))
      (alter-var-root var' (constantly (Unstarted. var')))
      (alter-meta! var' assoc ::status :stopped)
      (alter-meta! var' dissoc ::current-stop ::current-on-reload))
    sorted-vars))

(defn ^:no-doc defstate*
  [sym body]
  (let [current    (resolve sym)
        on-reload  (some-> current meta ::current-on-reload)
        status     (some-> current meta ::status)
        new-status (if (= on-reload :lifecycle) status :stopped)
        order      (or (some-> current meta ::order) (swap! order + 10))]
    (when (not= on-reload :lifecycle)
      (when (and (= on-reload :stop) (= status :started))
        (stop* [current]))
      (let [var (intern *ns* sym)]
        (alter-var-root var (constantly (Unstarted. var)))))
    (doto (or current (resolve sym))
      (alter-meta! merge body (meta sym) {::order order ::status new-status :redef true}))))

(def ^:private all-states
  (let [nss (into #{} (map find-ns)
                  '[clojure.core clojure.data clojure.edn clojure.inspector clojure.instant
                    clojure.java.browse clojure.java.io clojure.java.javadoc clojure.java.shell
                    clojure.main clojure.pprint clojure.reflect clojure.repl clojure.set
                    clojure.stacktrace clojure.string clojure.template clojure.test clojure.walk
                    clojure.xml clojure.zip])
        xf (comp (remove nss) (mapcat ns-interns) (map second) (filter (comp ::order meta)))]
    (fn [] (into #{} xf (all-ns)))))

(defn- merge-opts [optss]
  (cond-> {}
    (some :only optss)       (assoc :only (set (mapcat :only optss)))
    (some :except optss)     (assoc :except (set (mapcat :except optss)))
    (some :substitute optss) (assoc :substitute (apply conj {} (mapcat :substitute optss)))
    (some :up-to optss)      (assoc :up-to (last (map :up-to optss)))))

(defn- filtered-vars [status opts]
  (let [up-to-comparator (case status :stopped <= :started >=)
        up-to-order      (some-> (:up-to opts) meta ::order)]
    (-> (set (or (:only opts) (all-states)))
        (set/difference (set (:except opts)))
        (->> (filter #(= (-> % meta ::status) status)))
        (cond->> up-to-order (filter #(up-to-comparator (-> % meta ::order) up-to-order))))))

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
  "Creates or updates start/stop option map, starting or stopping only those
  vars up to the given var. Multiple uses of this function on the same option
  map are overriding each other."
  {:arglists '([var] [opts var])}
  ([var]
   {:up-to var})
  ([opts var]
   (assoc opts :up-to var)))

(defn start
  "Start all unstarted states (by default). One or more option maps can
  be supplied. These maps are merged. The maps can contain the following keys,
  applied in the following order:

  :only - Collection of state vars that should be started, when not stopped.

  :except - Collection of state vars that should not be started. The except

  :up-to - A defstate var until which the states are started. In case multiple
           option maps are supplied, only the last :up-to option is used.

  :substitute - A map of defstate vars to state-maps (see state macro) whose
                info is used instead of the defstate vars' state.

  These option maps are easily created using the only, except, up-to and
  substitute functions."
  [& optss]
  (let [opts (merge-opts optss)]
    (start* (var-state-map (filtered-vars :stopped opts) opts))))

(defn stop
  "Stop all started states (by default). One or more option maps can be
  supplied. These maps are merged. The maps can contain the following keys,
  applied in the following order:

  :only - Collection of state vars that should be stopped, when not started.

  :except - Collection of state vars that should not be stopped.

  :up-to - A defstate var until which the states are stopped. In case multiple
           option maps are supplied, only the last :up-to option is used.

  These option maps are easily created using the only and except functions."
  [& optss]
  (let [opts (merge-opts optss)]
    (stop* (filtered-vars :started opts))))

(defmacro state
  "Make a state definition, useful for making test or mock states. Use with
  substitute function or :substitute key in start info. This is a convenience
  macro; a plain state-map can be used in the :substitute start option as well.

  Note that the following does not define a state var, and won't be recognized by
  start or stop: (def foo (state ...))."
  [& {:as body}]
  (assert (:start body) "state must contain a :start expression")
  (assert (#{:stop :lifecycle} (:on-reload body :stop)) ":on-reload must be :stop or :lifecycle")
  `{:start     (fn [] ~(:start body))
    :stop      (fn [] ~(:stop body))
    :on-reload ~(:on-reload body :stop)})

(defmacro defstate
  "Define a state. At least a :start expression should be supplied. Optionally one
  can define a :stop expression and toggle whether the state should be stopped when
  redefined with the :stop-on-reload? key (defaults to true)."
  {:arglists '([name doc-string? attr-map? & {:as state-map}])}
  [name & args]
  (let [[name {:as body}] (name-with-attrs name args)]
    `(defstate* '~name (state ~@(apply concat body)))))
