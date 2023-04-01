(ns mount.extensions.namespace-deps
  "This extension offers more advanced up-to starting and down-to
  stopping, by calculating a dependency graph of defstates. It does
  this by looking at the namespace dependencies where the defstates
  are defined. Using this graph, mount will only start or stop the
  transitive dependencies or dependents.

  Using these functions, your project *must* include the
  org.clojure/tools.namespace library. This extension has been tested
  with version 0.2.11, 0.3.1 and 1.1.0 of that library."
  {:clojure.tools.namespace.repl/load false
   :clojure.tools.namespace.repl/unload false}
  (:require [mount.extensions.common-deps :as common-deps]
            [mount.lite :as mount]))

(def ^:private deps-tracker {})

(def ^:private scan-all
  (try
    (require 'clojure.tools.namespace.dir)
    (resolve 'clojure.tools.namespace.dir/scan-all)
    (catch Exception _e)))

(def ^:private dependency
  (try
    (require 'clojure.tools.namespace.dependency)
    {:graph                   (resolve 'clojure.tools.namespace.dependency/graph)
     :transitive-dependencies (resolve 'clojure.tools.namespace.dependency/transitive-dependencies)
     :depend                  (resolve 'clojure.tools.namespace.dependency/depend)}
    (catch Exception _e)))

(defn- ns-graph->state-graph
  "Take two graphs with namespace-to-namespace `:dependencies` and
  `:dependents` in the `ns-deps` map, and a namespace-to-states map,
  and generate a state-to-state dependencies graph (where states are
  represented by keywords). The direction argument should be one of
  `:dependencies` or `:dependents`."
  [ns-deps ns-states]
  (let [{:keys [graph transitive-dependencies depend]} dependency
        g                                              (atom (graph))]
    (doseq [[ns states] ns-states
            dep-ns      (transitive-dependencies ns-deps ns)
            state       states
            dep-state   (get ns-states dep-ns)]
      (swap! g depend state dep-state))
    (doseq [[_ states] ns-states
            state      states
            dep-state  (take-while #(not= state %) states)]
      (swap! g depend state dep-state))
    @g))

(defn build-graphs
  "Build two graphs of state keywords, represented as maps where the
  values are the dependencies or dependents of the keys. See also
  `mount.extensions.common-deps/with-transitives`."
  []
  (when-not scan-all
    (throw (UnsupportedOperationException. "Could not find tools.namespace dependency")))
  (alter-var-root #'deps-tracker scan-all)
  (let [ns-deps   (:clojure.tools.namespace.track/deps deps-tracker)
        ns-states (group-by (comp symbol namespace) @mount/*states*)]
    (ns-graph->state-graph ns-deps ns-states)))

(defn start
  "Just like the core `start`, except with an `up-to-var`, it only
  starts the transitive dependencies of that state."
  ([]
   (start nil))
  ([up-to-var]
   (common-deps/with-transitives up-to-var (build-graphs)
     (mount/start up-to-var))))

(defn stop
  "Just like the core `stop`, except with a `down-to-var`, it only
  stops the transitive dependents of that state."
  ([]
   (stop nil))
  ([down-to-var]
   (common-deps/with-transitives down-to-var (build-graphs)
     (mount/stop down-to-var))))
