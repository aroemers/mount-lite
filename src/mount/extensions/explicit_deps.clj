(ns mount.extensions.explicit-deps
  "This extension offers more advanced up-to starting and down-to
  stopping, by declaring the dependencies of defstates explicitly. It
  will only start or stop the transitive dependencies or dependents."
  (:require [mount.lite :as mount]
            [mount.utils :as utils]))

(defn build-graphs
  "Build two graphs of state keywords, represented as maps where the
  values are the dependencies or dependents of the keys."
  []
  (->> (for [kw  @mount/*states*
             var (let [var   (utils/resolve-keyword kw)
                       state (get mount/*substitutes* var (mount/as-started @var))]
                   (if (contains? state :dependencies)
                     (:dependencies state)
                     (throw (ex-info (format "state %s is missing a :dependencies field" state)
                                     {:state state}))))]
         (let [var-kw (utils/var->keyword var)]
           {:dependencies [kw var-kw]
            :dependents   [var-kw kw]}))
       (reduce (fn [a {:keys [dependencies dependents]}]
                 (-> a
                     (update-in [:dependencies (first dependencies)] conj (second dependencies))
                     (update-in [:dependents (first dependents)] conj (second dependents))))
               (let [base-map (zipmap @mount/*states* (repeat #{}))]
                 {:dependencies base-map
                  :dependents   base-map}))))

(defn- transitive
  [graph node]
  (loop [unexpanded (graph node)
         expanded #{}]
    (if-let [[node & more] (seq unexpanded)]
      (if (contains? expanded node)
        (recur more expanded)
        (recur (concat more (graph node))
               (conj expanded node)))
      expanded)))

(defn ^:no-doc with-transitives*
  "Calls 0-arity function `f`, while `*states*` has been bound to the
  transitive dependencies and dependents of the given state `var`."
  [var f]
  (let [var-kw       (utils/var->keyword var)
        graphs       (build-graphs)
        dependencies (transitive (:dependencies graphs) var-kw)
        dependents   (transitive (:dependents graphs) var-kw)
        concatted    (set (concat dependencies dependents))]
    (binding [mount/*states* (atom (filter (conj concatted var-kw) @mount/*states*))]
      (f))))

(defmacro with-transitives
  "Wraps the `body` having the `*states*` extension point bound to the
  transitive dependencies and dependents of the given state `var`.

  Make sure you wrap this with `with-substitutes` if applicable, in
  case you want `with-transitives` to use the `:dependencies` of the
  substitute states."
  [var & body]
  `(with-transitives* ~var (fn [] ~@body)))

(defn start
  "Just like the core `start` with an `up-to-var`, but now only starts
  the explicit transitive dependencies of that state."
  [up-to-var]
  (with-transitives up-to-var
    (mount/start up-to-var)))

(defn stop
  "Just like the core `stop` with a `down-to-var`, but now only stops
  the explicit transitive dependents of that state."
  [down-to-var]
  (mount/with-substitutes []
    (with-transitives down-to-var
      (mount/stop down-to-var))))
