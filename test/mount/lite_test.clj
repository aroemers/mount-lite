(ns mount.lite-test
  (:require [clojure.test :refer :all]
            [mount.lite :refer :all]
            [mount.lite-test.test-state-1 :refer (state-1)]
            [mount.lite-test.test-state-2 :refer (state-2)]
            [mount.lite-test.test-state-3 :refer (state-3)]))

;;; Helper functions.

(defn- statusses [& vars]
  (map (comp :mount.lite/status meta) vars))

;;; Stop all states before and after every test, and reset on-reload.

(use-fixtures :each (fn [f] (stop) (on-reload :cascade) (f) (stop)))

;;; Tests

(deftest test-start-stop
  (is (= (start) [#'state-1 #'state-2 #'state-3]) "Start all states in correct order.")
  (is (= (statusses #'state-1 #'state-2 #'state-3) [:started :started :started]))
  (is (= state-3 "state-1 + state-2 + state-3") "States can use othes states correctly.")
  (is (= (stop) [#'state-3 #'state-2 #'state-1]) "Stop all states in correct order.")
  (is (= (statusses #'state-1 #'state-2 #'state-3) [:stopped :stopped :stopped])))

(deftest test-only-one
  (is (= (start (only #'state-1)) [#'state-1]) "Start only state 1.")
  (is (= (statusses #'state-1 #'state-2 #'state-3) [:started :stopped :stopped]) "Only state-1 is started.")
  (is (= (stop (only #'state-2 #'state-3)) []) "Stopping states 2 and 3 does nothing.")
  (is (= (stop) [#'state-1]) "Stopping all states stops state 1."))

(deftest test-only-two
  (is (= (start (only #'state-2 #'state-1)) [#'state-1 #'state-2]) "Start only states 1 and 2 with one option map.")
  (is (= (statusses #'state-1 #'state-2 #'state-3) [:started :started :stopped]) "Only states 1 and 2 are started.")
  (is (= (stop) [#'state-2 #'state-1]) "Stopping all states stops state 1 and 2.")
  (is (= (statusses #'state-1 #'state-2 #'state-3) [:stopped :stopped :stopped]) "All states are stopped.")
  (is (= (start (only #'state-2) (only #'state-1)) [#'state-1 #'state-2]) "Start only states 1 and 2 with two option maps.")
  (is (= (statusses #'state-1 #'state-2 #'state-3) [:started :started :stopped]) "Only states 1 and 2 are started.")
  (is (= (stop) [#'state-2 #'state-1]) "Stopping all states stops states 1 and 2."))

(deftest test-except-one
  (is (= (start (except #'state-2)) [#'state-1 #'state-3]) "Start state 1 and 3.")
  (is (= (statusses #'state-1 #'state-2 #'state-3) [:started :stopped :started]) "State 1 and 3 are started.")
  (is (= (stop (except #'state-3)) [#'state-1]) "Only state 1 is stopped.")
  (is (= (stop) [#'state-3]) "Only state 3 is stopped"))

(deftest test-up-to
  (is (= (start (up-to #'state-2)) [#'state-1 #'state-2]) "Start state 1 and 2")
  (is (= (statusses #'state-1 #'state-2 #'state-3) [:started :started :stopped]))
  (is (= (start) [#'state-3]) "Start state 3")
  (is (= (statusses #'state-1 #'state-2 #'state-3) [:started :started :started]))
  (is (= (stop (up-to #'state-2)) [#'state-3 #'state-2]) "Stop state 3 and 2")
  (is (= (statusses #'state-1 #'state-2 #'state-3) [:started :stopped :stopped]))
  (is (= (start (up-to #'state-3) (up-to #'state-2)) [#'state-2]) "Override up-to, start state 2")
  (is (= (statusses #'state-1 #'state-2 #'state-3) [:started :started :stopped])))

(deftest test-substitute-state
  (start (substitute #'state-1 (state :start "sub-1")))
  (is (= state-3 "sub-1 + state-2 + state-3") "State 1 is substituted by anonymous state.")
  (stop)
  (start)
  (is (= state-3 "state-1 + state-2 + state-3") "State 1 is back to its original."))

(deftest test-substitute-map
  (start (substitute #'state-2 {:start (constantly "sub-2")}))
  (is (= state-3 "sub-2 + state-3") "State 2 is substituted by map.")
  (stop)
  (start)
  (is (= state-3 "state-1 + state-2 + state-3") "State 2 is back to its original."))

(deftest test-on-reload-cascade
  (start)
  (is (= (statusses #'state-1 #'state-2 #'state-3) [:started :started :started]) "All states started")
  (require 'mount.lite-test.test-state-2 :reload)
  (is (= (statusses #'state-1 #'state-2 #'state-3) [:started :stopped :stopped]) "Both state 2 and 3 have stopped"))

(deftest test-on-reload-stop
  (start)
  (is (= (statusses #'state-1 #'state-2 #'state-3) [:started :started :started]) "All states started")
  (on-reload :stop)
  (require 'mount.lite-test.test-state-2 :reload)
  (is (= (statusses #'state-1 #'state-2 #'state-3) [:started :stopped :started]) "Only state 2 has stopped"))

(deftest test-on-reload-lifecycle
  (start)
  (is (= (statusses #'state-1 #'state-2 #'state-3) [:started :started :started]) "All states started")
  (on-reload :lifecycle)
  (in-ns 'mount.lite-test.test-state-2)
  (defstate state-2 :start "redef-2")
  (in-ns 'mount.lite-test)
  (is (= (statusses #'state-1 #'state-2 #'state-3) [:started :started :started]) "All states are still running")
  (is (= state-2 "state-1 + state-2") "State 2 still has original value")
  (stop)
  (start)
  (is (= state-2 "redef-2") "State 2 lifecycle was redefined")
  (require 'mount.lite-test.test-state-2 :reload))
