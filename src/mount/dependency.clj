(ns mount.dependency
  "Extensions for the org.clojure/tools.namespace or
  com.stuartsierra/dependency library."
  {:no-doc true}
  (:require [clojure.set :as set]
            [clojure.tools.namespace.dependency :as dep]))

(defn add-node
  "Returns a new graph with the node added, if not already known."
  [graph node]
  (dep/->MapDependencyGraph
   (update-in (:dependencies graph) [node] (fnil identity #{}))
   (:dependents graph)))

(defn layer-sort
  "Returns a sequence of sets, where each set has dependencies on the
  former sets."
  [graph]
  (loop [found #{}
         layers []
         graph graph]
    (if-let [nodes (seq (dep/nodes graph))]
      (let [layer (set (filter #(every? found (dep/immediate-dependencies graph %)) nodes))]
        (recur (apply conj found layer)
               (conj layers layer)
               (reduce dep/remove-all graph layer)))
      layers)))
