(ns mount.lite-test.test-state-1
  (:require [mount.lite :refer (defstate)]))

(defstate state-1 "docstring"
  :start "state-1"
  :stop @state-1
  :extra 'data
  :dependencies nil)
