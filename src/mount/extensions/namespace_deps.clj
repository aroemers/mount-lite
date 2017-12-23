(ns mount.extensions.namespace-deps
  "This extension offers more advanced up-to starting and down-to
  stopping, by calculating a dependency graph of defstates. It does
  this by looking at the namespace dependencies where the defstates
  are defined. Using this graph, mount will only start or stop the
  transitive dependencies or dependents.

  Using these functions, your project *must* include the
  org.clojure/tools.namespace library. This extension has been tested
  with version 0.2.11 of that library."
  {:clojure.tools.namespace.repl/load false
   :clojure.tools.namespace.repl/unload false}
  (:require [mount.extensions.common-deps :as common-deps]
            [mount.lite :as mount]))

(def ^:private deps-tracker {})

(def ^:private scan-all
  (try
    (require 'clojure.tools.namespace.dir)
    (resolve 'clojure.tools.namespace.dir/scan-all)
    (catch Exception e)))

(defn- ns-graph->state-graph
  "Take a namespace-to-namespace dependencies graph, and a
  namespace-to-states map, and generate a state-to-state dependencies
  graph (where states are represented by keywords)."
  [ns-graph ns-states]
  (let [empty-graph (zipmap @mount/*states* (repeat #{}))]
    (reduce-kv (fn [graph ns ns-deps]
                 (into graph (for [state (get ns-states ns)]
                               [state (set (apply concat (keep ns-states ns-deps)))])))
               empty-graph
               ns-graph)))

(defn build-graphs
  "Build two graphs of state keywords, represented as maps where the
  values are the dependencies or dependents of the keys."
  []
  (when-not scan-all
    (throw (UnsupportedOperationException. "Could not find tools.namespace dependency")))
  (alter-var-root #'deps-tracker scan-all)
  (let [ns-deps   (:clojure.tools.namespace.track/deps deps-tracker)
        ns-states (group-by (comp symbol namespace) @mount/*states*)]
    {:dependencies (ns-graph->state-graph (:dependencies ns-deps) ns-states)
     :dependents   (ns-graph->state-graph (:dependents ns-deps) ns-states)}))

(defn start
  "Just like the core `start` with an `up-to-var`, but now only starts
  the explicit transitive dependencies of that state."
  [up-to-var]
  (common-deps/with-transitives up-to-var (build-graphs)
    (mount/start up-to-var)))

(defn stop
  "Just like the core `stop` with a `down-to-var`, but now only stops
  the explicit transitive dependents of that state."
  [down-to-var]
  (common-deps/with-transitives down-to-var (build-graphs)
    (mount/stop down-to-var)))
