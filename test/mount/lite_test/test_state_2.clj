(ns mount.lite-test.test-state-2
  (:require [mount.lite :refer (defstate)]
            [mount.lite-test.test-state-1 :refer (state-1)]))

(defstate state-2 [s " + state-2"
                   p (promise)]
  :start (str state-1 s)
  :stop (deliver p "delivery!"))
