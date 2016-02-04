(ns mount.lite-test.test-par
  (:require [mount.lite :refer (defstate)])
  (:import [java.util.concurrent CountDownLatch TimeUnit]))

(def in nil)
(def out nil)

(defn set-latches [n]
  (alter-var-root #'in (constantly (CountDownLatch. n)))
  (alter-var-root #'out (constantly (CountDownLatch. n))))

(defn reset-latches []
  (alter-var-root #'in (constantly nil))
  (alter-var-root #'out (constantly nil)))

(defstate par
  :start (when in
           (.countDown in)
           (.await in 5 TimeUnit/SECONDS)))
