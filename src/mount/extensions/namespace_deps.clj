(ns mount.extensions.namespace-deps
  "This extension offers more advanced up-to starting and down-to
  stopping, by calculating a dependency graph of defstates. It does
  this by looking at the namespace dependencies where the defstates
  are defined. Using this graph, mount-lite will only start or stop
  the transitive dependencies or dependents.

  Using these functions, your project *must* include the
  org.clojure/tools.namespace library. This extension has been tested
  with version 0.2.11 of that library."
  {:clojure.tools.namespace.repl/load   false
   :clojure.tools.namespace.repl/unload false}
  (:require [clojure.tools.namespace.dependency :as dependency]
            [clojure.tools.namespace.dir :as dir]
            [clojure.tools.namespace.track :as track]
            [mount.extensions :as extensions]
            [mount.lite :as mount]))

(defn- predicate-factory
  [{:keys [states start? up-to]}]
  (if up-to
    (let [up-to-ns      (symbol (namespace up-to))
          graph         (::track/deps (dir/scan-all {}))
          transitive-fn (if start?
                          dependency/transitive-dependencies
                          dependency/transitive-dependents)
          transitive    (transitive-fn graph up-to-ns)
          predicate     (fn [state]
                          (or (transitive (symbol (namespace state)))
                              (= up-to-ns (symbol (namespace state)))))]
      (set (filter predicate states)))
    identity))

(extensions/register-predicate-factory predicate-factory)


;;; Legacy, for compatibility with mount-lite 2.

(defn start
  "Legacy function, simply forwarding to mount.lite/start."
  ([]
   (mount/start))
  ([up-to]
   (mount/start up-to)))

(defn stop
  "Legacy function, simply forwarding to mount.lite/stop."
  ([]
   (mount/stop))
  ([up-to]
   (mount/stop up-to)))
