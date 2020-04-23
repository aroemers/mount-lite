(ns mount.extensions.up-to
  "Extension supplying the default logic for mount-lite's up-to feature.
  Loaded by default."
  {:clojure.tools.namespace.repl/load   false
   :clojure.tools.namespace.repl/unload false}
  (:require [mount.extensions :as extensions]))

(defn- predicate-factory
  [{:keys [states start? up-to]}]
  (if up-to
    (let [ordered        (cond-> states (not start?) reverse)
          [before after] (split-with (complement #{up-to}) ordered)]
      (set (concat before (take 1 after))))
    identity))

(extensions/register-predicate-factory predicate-factory)
