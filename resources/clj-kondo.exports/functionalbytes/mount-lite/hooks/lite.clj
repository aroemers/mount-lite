(ns hooks.lite
  (:require [clojure.string :as str]))

(defn- wrap-stop-fn [expr]
  `(let [~'this nil] ~expr))

(defn- exprs-map [args]
  (let [hmap    (apply hash-map args)]
    (cond-> hmap
      (some-> hmap :stop str (str/includes? "this"))
      (update :stop wrap-stop-fn))))

(defmacro defstate [name & args]
  (let [args (cond->> args (string? (first args)) rest)
        args (cond->> args (map? (first args)) rest)
        hmap (exprs-map args)]
    (when-not (contains? hmap :start)
      (throw (ex-info "missing :start expression" {})))
    `(def ~name ~hmap)))

(defmacro state [& args]
  (let [hmap (exprs-map args)]
    (when-not (contains? hmap :start)
      (throw (ex-info "missing :start expression" {})))
    hmap))
