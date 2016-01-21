(ns mount.lite
  (:require [clojure.set :as set]))

;;; Types

(deftype Unstarted [ns name]
  Object
  (toString [_] (str "State #" ns "/" name " is not started.")))

(alter-meta! #'->Unstarted assoc :private true)


;;; Private logic

(defonce ^:private order (atom -1))

(defn- ordered-var-metas
  [vars]
  (->> vars
       (map (juxt identity meta))
       (sort-by (comp ::order second))))

(defn- start*
  [vars substitutes]
  (let [var-metas (ordered-var-metas vars)]
    (doseq [[var' meta'] var-metas]
      (let [start-fn (if-let [substitute (get substitutes var')]
                       (let [original (select-keys meta' [::start ::stop ::stop-on-reload?])]
                         (alter-meta! var' merge {::substituted-original original} substitute)
                         (::start substitute))
                       (::start meta'))]
        (alter-var-root var' (constantly (start-fn)))
        (alter-meta! var' assoc ::status :started)))
    (map first var-metas)))

(defn- stop*
  [vars]
  (let [var-metas (reverse (ordered-var-metas vars))]
    (doseq [[var' meta'] var-metas]
      ((-> meta' ::stop))
      (alter-var-root var' (constantly (Unstarted. (-> meta' :ns str) (-> meta' :name str))))
      (alter-meta! var' merge (::substituted-original meta') {::status :stopped}))
    (map first var-metas)))

(defn- all-states
  []
  (->> (all-ns)
       (mapcat ns-interns)
       (map second)
       (filter (comp ::start meta))
       (set)))

(defn- merge-opts
  [optss]
  (cond-> {}
          (some :only optss) (assoc :only (set (mapcat :only optss)))
          (some :except optss) (assoc :except (set (mapcat :except optss)))
          (some :substitute optss) (assoc :substitute (apply conj {} (mapcat :substitute optss)))))

(defn- name-with-attributes ;; from https://github.com/clojure/tools.macro
  [name macro-args]
  (let [[docstring macro-args] (if (string? (first macro-args))
                                 [(first macro-args) (next macro-args)]
                                 [nil macro-args])
        [attr macro-args]      (if (map? (first macro-args))
                                 [(first macro-args) (next macro-args)]
                                 [{} macro-args])
        attr                   (if docstring
                                 (assoc attr :doc docstring)
                                 attr)
        attr                   (if (meta name)
                                 (conj (meta name) attr)
                                 attr)]
    [(with-meta name attr) macro-args]))


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

(defn start
  "Start all unstarted states (by default). One or more option maps may
  be supplied. These maps are merged. The maps may contain the following keys:

  :only - Collection of state vars that should be started, when not stopped.

  :except - Collection of state vars that should not be started.

  :substitute - A map of defstate vars to states (see state macro) whose info
                is used instead of the defstate vars state.

  These option maps are easily created using the only, except and
  substitute functions."
  [& optss]
  (let [opts (merge-opts optss)]
    (-> (set (or (:only opts) (all-states)))
        (set/difference (set (:except opts)))
        (->> (filter #(= (-> % meta ::status) :stopped)))
        (start* (:substitute opts)))))

(defn stop
  "Stop all started states (by default). One or more option maps may be
  supplied. These maps are merged. The maps may contain the following keys:

  :only - Collection of state vars that should be stopped, when not started.

  :except - Collection of state vars that should not be stopped.

  These option maps are easily created using the only and except functions."
  [& optss]
  (let [opts (merge-opts optss)]
    (-> (set (or (:only opts) (all-states)))
        (set/difference (set (:except opts)))
        (->> (filter #(= (-> % meta ::status) :started)))
        (stop*))))

(defmacro state
  "Make a state definition, useful for making test or mock states. Use with
  substitute function or :substitute key in start info. Note that the
  following does not define a state var, and won't be recognized by
  start or stop: (def foo (state ...))."
  {:arglists '([state-map] [state-map-sym] [& {:as state-map}])}
  [& args]
  (let [body (cond (map? (first args)) (first args)
                   (symbol? (first args)) (eval (first args))
                   :otherwise (apply hash-map args))]
    `{:mount.lite/start (fn [] ~(or (:start body) (throw (ex-info "Missing :start in state definition" body))))
      :mount.lite/stop (fn [] ~(:stop body))
      :mount.lite/stop-on-reload? ~(:stop-on-reload? body true)
      :mount.lite/status :stopped}))

(defmacro defstate
  "Define a state. At least a :start expression should be supplied. Optionally one
  can define a :stop expression and toggle whether the state should be stopped when
  redefined with the :stop-on-reload? key (defaults to true)."
  {:arglists '([name doc-string? attr-map? & {:as state-map}])}
  [name & args]
  (let [[name {:as body}] (name-with-attributes name args)
        current (resolve name)
        order (or (::order (meta current)) (swap! order inc))]
    (when (::stop-on-reload? (meta current))
      (stop (only current)))
    `(doto (def ~name (Unstarted. ~(str *ns*) ~(str name)))
       (alter-meta! merge (state ~@(apply concat body)) {:mount.lite/order ~order :redef true}))))