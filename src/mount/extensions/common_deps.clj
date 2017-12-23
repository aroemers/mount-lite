(ns mount.extensions.common-deps
  "A namespace used by graph-related extensions, such as explicit-deps
  and namespace-deps."
  (:require [mount.lite :as mount]
            [mount.utils :as utils]))

(defn- transitive
  "Given a graph and a node, returns all the transitive nodes."
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
  [var graphs f]
  (let [var-kw       (utils/var->keyword var)
        dependencies (transitive (:dependencies graphs) var-kw)
        dependents   (transitive (:dependents graphs) var-kw)
        concatted    (set (concat dependencies dependents))]
    (binding [mount/*states* (atom (filter (conj concatted var-kw) @mount/*states*))]
      (f))))

(defmacro with-transitives
  "Wraps the `body` having the `*states*` extension point bound to the
  transitive dependencies and dependents of the given state `var`. The
  dependencies are read from the given graphs map. The graphs map
  holds a `:dependencies` key and `:dependents` key. Both values must
  hold a defstate-to-defstates direct dependency graph. The defstates
  are represented by namespaced keywords."
  [var graphs & body]
  `(with-transitives* ~var ~graphs (fn [] ~@body)))
