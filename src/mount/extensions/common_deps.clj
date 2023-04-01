(ns mount.extensions.common-deps
  "A namespace used by graph-related extensions, such as explicit-deps
  and namespace-deps."
  (:require [mount.lite :as mount]
            [mount.utils :as utils]))

;; topsort-component and topsort adapted from loom
;; https://github.com/aysylu/loom

(defn topsort-component
  "Topological sort of a component of a (presumably) directed graph.
  Returns nil if the graph contains any cycles."
  ([successors start]
     (topsort-component successors start #{} #{}))
  ([successors start seen explored]
     (loop [seen seen
            explored explored
            result ()
            stack [start]]
       (if (empty? stack)
         result
         (let [v (peek stack)
               seen (conj seen v)
               us (remove explored (successors v))]
           (if (seq us)
             (when-not (some seen us)
               (recur seen explored result (conj stack (first us))))
             (recur seen (conj explored v) (conj result v) (pop stack))))))))

(defn topsort
  "Topological sort of a directed acyclic graph (DAG). Returns nil if
  g contains any cycles."
  ([g]
   (loop [seen #{}
          result ()
          [n & ns] (seq (keys g))]
     (if-not n
       result
       (if (seen n)
         (recur seen result ns)
         (when-let [cresult (topsort-component
                             #(get g %) n seen seen)]
           (recur (into seen cresult) (concat cresult result) ns))))))
  ([g start]
   (if start
     (topsort-component #(get g %) start)
     (topsort g))))

(defn ^:no-doc transitives
  "Filters and orders a given list of states in transitive dependency order
  based upon the given dependency graphs."
  [var graphs states]
  (let [var-kw       (when var (utils/var->keyword var))
        dependencies (reverse (topsort (:dependencies graphs) var-kw))
        dependents   (topsort (:dependents graphs) var-kw)
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
