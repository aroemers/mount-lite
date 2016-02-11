(ns mount.lite.util
  "Utility namespace"
  {:no-doc true})

(defn memoize-1
  "Memoize only the last result of f."
  [f]
  (let [mem-args (volatile! ::init)
        mem-val (volatile! nil)]
    (fn [& args]
      (if (= args @mem-args)
        @mem-val
        (do (vreset! mem-args args)
            (vreset! mem-val (apply f args)))))))
