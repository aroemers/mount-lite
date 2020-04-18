(ns mount.extensions.up-to
  {:clojure.tools.namespace.repl/load   false
   :clojure.tools.namespace.repl/unload false})

;; Loaded by default by the core.

(defn predicate-factory
  [{:keys [states start? up-to]}]
  (if up-to
    (let [[before after] (split-with (complement #{up-to}) (cond-> states (not start?) reverse))]
      (set (concat before (take 1 after))))
    identity))
