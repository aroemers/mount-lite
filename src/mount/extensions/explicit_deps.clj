(ns mount.extensions.explicit-deps
  "This extension offers more advanced up-to starting and down-to
  stopping, by declaring the dependencies of defstates explicitly. It
  will only start or stop the transitive dependencies or dependents.

  Using these functions, the defstates or substitute states *must*
  declare a `:dependencies` field, which may be nil."
  (:require [mount.extensions.common-deps :as common-deps]
            [mount.lite :as mount]
            [mount.utils :as utils]))

(defn build-graphs
  "Build two graphs of state keywords, represented as maps where the
  values are the dependencies or dependents of the keys."
  []
  (->> (for [kw  @mount/*states*
             var (let [var   (utils/resolve-keyword kw)
                       state (get mount/*substitutes* var (mount/properties @var))]
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

(defn start
  "Just like the core `start` with an `up-to-var`, but now only starts
  the explicit transitive dependencies of that state."
  [up-to-var]
  (common-deps/with-transitives up-to-var (build-graphs)
    (mount/start up-to-var)))

(defn stop
  "Just like the core `stop` with a `down-to-var`, but now only stops
  the explicit transitive dependents of that state."
  [down-to-var]
  (mount/with-substitutes []
    (common-deps/with-transitives down-to-var (build-graphs)
      (mount/stop down-to-var))))
