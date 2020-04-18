(ns mount.extensions.namespace-deps
  (:require [mount.lite :as mount]
            [clojure.tools.namespace.track :as track]
            [clojure.tools.namespace.dir :as dir]
            [clojure.tools.namespace.dependency :as dependency]))

(defn- state-filter [transitive-fn up-to]
  (let [up-to-ns   (symbol (namespace up-to))
        graph      (::track/deps (dir/scan-all {}))
        transitive (transitive-fn graph up-to-ns)]
    (fn [_]
      (fn [state]
        (or (transitive (symbol (namespace state)))
            (= up-to-ns (symbol (namespace state))))))))

(defn start
  ([]
   (mount/start))
  ([up-to]
   (mount/with-state-filter (state-filter dependency/transitive-dependencies up-to)
     (mount/start up-to))))

(defn stop
  ([]
   (mount/stop))
  ([up-to]
   (mount/with-state-filter (state-filter dependency/transitive-dependents up-to)
     (mount/stop up-to))))
