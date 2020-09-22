(ns mount.extensions.common-deps
  "A namespace used by graph-related extensions, such as explicit-deps
  and namespace-deps."
  (:require [mount.lite :as mount]
            [mount.utils :as utils]))

(defn- transitive
  "Given a graph and a node, returns all the transitive nodes."
  [graph node]
  (letfn [(search [cur]
            (->> (get graph cur)
                 (map search)
                 (reduce into [cur])))]
    (search node)))

(defn ^:no-doc transitives
  "Filters and orders a given list of states in transitive dependency order
  based upon the given dependency graphs."
  [var graphs states]
  (let [var-kw       (utils/var->keyword var)
        dependencies (reverse (transitive (:dependencies graphs) var-kw))
        dependents   (transitive (:dependents graphs) var-kw)
        concatted    (distinct (concat dependencies dependents))]
    (filter (set states) concatted)))

(defn ^:no-doc with-transitives*
  "Calls 0-arity function `f`, while `*states*` has been bound to the
  transitive dependencies and dependents of the given state `var`."
  [var graphs f]
  (binding [mount/*states* (atom (transitives var graphs @mount/*states*))]
    (f)))

(defmacro with-transitives
  "Wraps the `body` having the `*states*` extension point bound to the
  transitive dependencies and dependents of the given state `var`. The
  dependencies are read from the given graphs map. The graphs map
  holds a `:dependencies` key and `:dependents` key. Both values must
  hold a defstate-to-defstates direct dependency graph. The defstates
  are represented by namespaced keywords."
  [var graphs & body]
  `(with-transitives* ~var ~graphs (fn [] ~@body)))
