(ns mount.extensions.basic
  (:require [mount.lite :as mount]))

(defmacro with-only
  [states & body]
  `(mount/with-state-filter (fn [_] (set ~states))
     ~@body))

(defmacro with-except
  [states & body]
  `(mount/with-state-filter (fn [_] (complement (set ~states)))
     ~@body))

(defn ns-states
  [& nss]
  (let [ns-strs (set (map str nss))]
    (filter (comp ns-strs namespace :name)
            (keys (mount/status)))))
