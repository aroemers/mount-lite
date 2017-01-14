(ns mount.lite-test.test-state-3
  (:require [mount.lite :refer (defstate)]
            [mount.lite-test.test-state-2 :refer (state-2)]))

(defstate state-3
  :start (str @state-2 " + state-3")
  :dependencies #{#'state-2})
