(ns mount.extensions.up-to)

(defn start-filter [up-to]
  (fn [states]
    (let [[before after] (split-with (complement #{up-to}) states)]
      (set (concat before (take 1 after))))))

(defn stop-filter [up-to]
  (fn [states]
    ((start-filter up-to) (reverse states))))
