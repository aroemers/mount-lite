(ns mount.extensions.up-to)

(defn predicate-factory
  [{:keys [states start? up-to]}]
  (if up-to
    (let [[before after] (split-with (complement #{up-to}) (cond-> states (not start?) reverse))]
      (set (concat before (take 1 after))))
    identity))
