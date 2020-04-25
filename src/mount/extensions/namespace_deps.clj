(ns mount.extensions.namespace-deps
  "This extension offers more advanced up-to starting and down-to
  stopping, by calculating a dependency graph of defstates. It does
  this by looking at the namespace dependencies where the defstates
  are defined. Using this graph, mount-lite will only start or stop
  the transitive dependencies or dependents.

  For this extension to work, your project *must* include the
  org.clojure/tools.namespace library. This extension has been tested
  with version 0.2.11 of that library.

  To use this extension, simply use the start and stop functions in
  this namespace."
  {:clojure.tools.namespace.repl/load   false
   :clojure.tools.namespace.repl/unload false}
  (:require [clojure.tools.namespace.dependency :as dependency]
            [clojure.tools.namespace.dir :as dir]
            [clojure.tools.namespace.track :as track]
            [mount.extensions :as extensions]
            [mount.lite :as mount]))

;;; Internals.

(defn- predicate-factory
  [start? up-to]
  (if up-to
    (let [up-to-ns      (symbol (namespace up-to))
          graph         (::track/deps (dir/scan-all {}))
          transitive-fn (if start?
                          dependency/transitive-dependencies
                          dependency/transitive-dependents)
          transitive    (transitive-fn graph up-to-ns)]
      (fn [state]
        (or (transitive (symbol (namespace state)))
            (= up-to-ns (symbol (namespace state))))))))


;;; Public API.

(defn start
  "Like mount.lite/start, but when an up-to parameter is supplied, this
  extension's functionality is applied."
  ([]
   (mount/start))
  ([up-to]
   (extensions/with-predicate (predicate-factory true up-to)
     (mount/start up-to))))

(defn stop
  "Like mount.lite/stop, but when an up-to parameter is supplied, this
  extension's functionality is applied."
  ([]
   (mount/stop))
  ([up-to]
   (extensions/with-predicate (predicate-factory false up-to)
     (mount/stop up-to))))
