(ns ^:no-doc mount.utils)

(defn name-with-attrs
  [name [arg1 arg2 & argx :as args]]
  (let [[attrs args] (cond (and (string? arg1) (map? arg2)) [(assoc arg2 :doc arg1) argx]
                           (string? arg1)                   [{:doc arg1} (cons arg2 argx)]
                           (map? arg1)                      [arg1 (cons arg2 argx)]
                           :otherwise                       [{} args])]
    [(with-meta name (merge (meta name) attrs)) args]))

(defn find-index
  [elem coll]
  (first (keep-indexed (fn [i e]
                         (when (= e elem)
                           i))
                       coll)))

(defn keyword->symbol
  [kw]
  (symbol (namespace kw) (name kw)))

(defn resolve-keyword
  [kw]
  (resolve (keyword->symbol kw)))

(defn var->keyword
  [^clojure.lang.Var var]
  (keyword (str (.ns var)) (str (.sym var))))
