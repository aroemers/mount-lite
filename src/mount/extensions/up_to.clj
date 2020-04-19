(ns mount.extensions.up-to
  "Loaded by default"
  {:clojure.tools.namespace.repl/load   false
   :clojure.tools.namespace.repl/unload false}
  (:require [mount.extensions :as extensions]))

(defn predicate-factory
  [{:keys [states start? up-to]}]
  (if up-to
    (let [[before after] (split-with (complement #{up-to}) (cond-> states (not start?) reverse))]
      (set (concat before (take 1 after))))
    identity))

(swap! extensions/predicate-factories conj predicate-factory)
