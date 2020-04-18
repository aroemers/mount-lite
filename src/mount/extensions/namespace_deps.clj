(ns mount.extensions.namespace-deps
  (:require [clojure.tools.namespace.dependency :as dependency]
            [clojure.tools.namespace.dir :as dir]
            [clojure.tools.namespace.track :as track]
            [mount.lite :as mount]))

(defn- predicate-factory
  [{:keys [states start? up-to]}]
  (if up-to
    (let [up-to-ns      (symbol (namespace up-to))
          graph         (::track/deps (dir/scan-all {}))
          transitive-fn (if start? dependency/transitive-dependencies dependency/transitive-dependents)
          transitive    (transitive-fn graph up-to-ns)
          predicate     (fn [state]
                          (or (transitive (symbol (namespace state)))
                              (= up-to-ns (symbol (namespace state)))))]
      (set (filter predicate states)))
    identity))

(defn start
  ([]
   (mount/start))
  ([up-to]
   (swap! mount/predicate-factories conj predicate-factory)
   (mount/start up-to)))

(defn stop
  ([]
   (mount/start))
  ([up-to]
   (swap! mount/predicate-factories conj predicate-factory)
   (mount/stop up-to)))
