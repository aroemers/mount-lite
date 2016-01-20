(ns mount.lite
  (:require [clojure.set :as set]))

;;; Types

(deftype Unstarted [ns name]
  Object
  (toString [_] (str "State #" ns "/" name " is not started.")))

(ns-unmap *ns* '->Unstarted)


;;; Private logic

(def ^:private order (atom -1))

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
                       (do (reset-meta! var' (merge {::substituted-original meta'} substitute))
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
      (alter-var-root var' (constantly (Unstarted. (str (.ns var')) (str (.sym var')))))
      (reset-meta! var' (merge (::substituted-original meta' meta') {::status :stopped})))
    (map first var-metas)))

(defn- all-states
  []
  (->> (all-ns)
       (mapcat ns-interns)
       (map second)
       (filter (comp ::start meta))
       (set)))


;;; Public API

(defn only
  "Creates or updates start/stop input, only started or stopping the given
  vars. Multiple uses of this function on the same input are united."
  {:arglists '([& vars] [input & vars])}
  [& [input-or-var & vars]]
  (let [[input vars] (if (var? input-or-var)
                       [{} (conj (set vars) input-or-var)]
                       [input-or-var (set vars)])]
    (update-in input [:only] set/union vars)))

(defn except
  "Creates or updates start/stop input, starting or stopping all defstate
  vars, except the given vars. Multiple uses of this function on the same input
  are united."
  {:arglists '([& vars] [input & vars])}
  [& [input-or-var & vars]]
  (let [[input vars] (if (var? input-or-var)
                       [{} (conj (set vars) input-or-var)]
                       [input-or-var (set vars)])]
    (update-in input [:except] set/union vars)))

(defn substitute
  "Creates or updates start input, supplying substitute states for the given
  defstate vars. Multiple uses of this function on the same input are merged."
  {:arglists '([& var-state-seq] [input & var-state-seq])}
  [& [input-or-var & var-state-seq]]
  (let [[input var-state-seq] (if (var? input-or-var)
                                [{} (cons input-or-var var-state-seq)]
                                [input-or-var var-state-seq])]
    (let [substitutes (apply hash-map var-state-seq)]
      (update-in input [:substitute] merge substitutes))))

(defn start
  "Start all unstarted states (by default). One or more configuration maps may
  be supplied. These maps are merged. The maps may contain the following keys:

  :only - Collection of state vars that should be started, when not stopped.

  :except - Collection of state vars that should not be started.

  :substitute - A map of defstate vars to states (see state macro) whose info
                is used instead of the defstate vars state.

  These configurations are easily created using the only, except and
  substitute functions."
  [& inputs]
  (let [conf (apply merge inputs)]
    (-> (set (or (:only conf) (all-states)))
        (disj (set (:except conf)))
        (->> (filter #(= (-> % meta ::status) :stopped)))
        (start* (:substitute conf)))))

(defn stop
  "Stop all started states (by default). One or more configuration maps may be
  supplied. These maps are merged. The maps may contain the following keys:

  :only - Collection of state vars that should be stopped, when not started.

  :except - Collection of state vars that should not be stopped.

  These configurations are easily created using the only and except functions."
  [& inputs]
  (let [conf (apply merge inputs)]
    (-> (set (or (:only conf) (all-states)))
        (disj (set (:except conf)))
        (->> (filter #(= (-> % meta ::status) :started)))
        (stop*))))

(defmacro state
  "Make a state definition, useful for making test or mock states. Use with
   substitute function or :substitute key in start info."
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
  "Define a state. At least a :start value should be supplied. Optionally one
  can define a :stop value and toggle whether the state should be stopped when
  redefined with the :stop-on-reload? key."
  [name & {:as body}] ;;--- TODO support docstring and meta data
  (let [current (resolve name)
        order (or (::order (meta current)) (swap! order inc))]
    (when (::stop-on-reload? (meta current))
      (stop (only current)))
    `(doto (def ^:redef ~name (Unstarted. ~(str *ns*) ~(str name)))
       (reset-meta! (merge (state ~@(apply concat body)) {:mount.lite/order ~order})))))