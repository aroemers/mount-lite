(ns mount.extensions.namespace-deps
  {:clojure.tools.namespace.repl/load   false
   :clojure.tools.namespace.repl/unload false}
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

(swap! mount/predicate-factories conj predicate-factory)


;;; Legacy, for compatibility with mount-lite 2.

(defn start
  ([]
   (mount/start))
  ([up-to]
   (mount/start up-to)))

(defn stop
  ([]
   (mount/start))
  ([up-to]
   (mount/stop up-to)))
