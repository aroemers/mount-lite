(ns mount.extensions
  (:require [mount.extensions.up-to :as up-to]))

(defonce predicate-factories (atom #{up-to/predicate-factory}))

(defn state-filter
  [states start? up-to]
  (let [info {:states states :start? start? :up-to up-to}]
    (apply every-pred (map #(% info) @predicate-factories))))
