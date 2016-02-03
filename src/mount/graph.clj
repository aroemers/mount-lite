(ns mount.graph
  "Namespace reponsible for deducing a dependency graph for
  a set of state vars."
  {:no-doc true}
  (:require [clojure.set :as set]
            [com.stuartsierra.dependency :as dep]
            [mount.dependency :as mydep]))

;;---TODO Incomplete, because ns-refers and ns-aliases is sadny not enough. Below is prove of concept.
(def ^:private ns-deps
  (let [ignore (into #{} (map find-ns) '[clojure.core])
        aliases-xf (comp (map second) (remove ignore))
        refers-xf (comp (map (fn [[_ var]] (.ns var))) (remove ignore))]
    (fn [ns]
      (set/union (into #{} aliases-xf (ns-aliases ns))
                 (into #{} refers-xf (ns-refers ns))))))

(defn- ns-graph [vars]
  (let [nss (into #{} (map #(.ns %)) vars)]
    (reduce (fn [g ns]
              (reduce (fn [g dep]
                        (dep/depend g ns dep))
                      g (ns-deps ns)))
            (dep/graph) nss)))

(defn- add-transitives [graph namespaces ns-vars var]
  (reduce (fn [g ns]
            (reduce (fn [g dep-ns-var]
                      (dep/depend g var dep-ns-var))
                    g
                    (get ns-vars ns)))
          graph namespaces))

(defn- add-same-ns [graph var vars]
  (reduce (fn [g ns-var]
            (cond-> g
              (and (not= var ns-var)
                   (< (-> ns-var meta :mount.lite/order)
                      (-> var meta :mount.lite/order)))
              (dep/depend var ns-var)))
          graph vars))

(defn var-graph
  "Create a dependency graph of the given state vars."
  [vars]
  (let [ns-graph (ns-graph vars)
        ns-vars (group-by #(.ns %) vars)
        graph (reduce mydep/add-node (dep/graph) vars)]
    (reduce (fn [g var]
              (if-let [deps (-> var meta :dependencies)]
                (reduce (fn [g dep] (dep/depend g var dep)) g deps)
                (let [ns (.ns var)]
                  (-> (add-transitives g (dep/transitive-dependencies ns-graph ns) ns-vars var)
                      (add-same-ns var (get ns-vars ns))))))
            graph vars)))
