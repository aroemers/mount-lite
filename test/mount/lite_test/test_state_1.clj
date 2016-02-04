(ns mount.lite-test.test-state-1
  (:require [mount.lite :refer (defstate)])
  (:import [java.util.concurrent TimeUnit]))

(defstate state-1
  :start (if-let [in (eval 'mount.lite-test.test-par/in)]
           (do (.countDown in)
               (.await in 5 TimeUnit/SECONDS))
           "state-1")
  :stop state-1)
