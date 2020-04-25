(ns mount.extensions.up-to
  "Extension supplying the standard logic for mount-lite's up-to feature.
  Used by default."
  {:clojure.tools.namespace.repl/load   false
   :clojure.tools.namespace.repl/unload false
   :no-doc                              true})

(defn predicate-factory
  [states start? up-to]
  (let [ordered        (cond-> states (not start?) reverse)
        [before after] (split-with (complement #{up-to}) ordered)]
    (set (concat before (take 1 after)))))
