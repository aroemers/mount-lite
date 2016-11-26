(ns mount.lite-test.test-state-1
  (:require [mount.lite :refer (defstate)])
  (:import [java.util.concurrent TimeUnit]))

(defstate state-1
  :start "state-1"
  :stop @state-1)
