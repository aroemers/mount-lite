(ns mount.lite-test.test-state-2-extra
  (:require [mount.lite :refer (defstate)]
            [mount.lite-test.test-state-1 :refer (state-1)]))

(defstate state-2-a
  :start (str @state-1 " + state-2-a")
  :dependencies [#'state-1])

(defstate state-2-b
  :start (str @state-2-a " + state-2-b")
  :dependencies [#'state-2-a])
