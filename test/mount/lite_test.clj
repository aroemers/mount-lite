(ns mount.lite-test
  (:require [clojure.test :refer [deftest use-fixtures is]]
            [mount.lite :refer [start stop status with-substitutes state]]
            [mount.lite-test.test-state-1 :refer (state-1)]
            [mount.lite-test.test-state-2 :refer (state-2)]
            [mount.lite-test.test-state-2-extra :as ts2e :refer (state-2-a state-2-b)]
            [mount.lite-test.test-state-3 :refer (state-3)])
  (:import [clojure.lang ExceptionInfo]))

;;; Stop all states before and after every test.

(use-fixtures :each (fn [f] (stop) (f) (stop)))

;;; Tests

(deftest test-start-stop
  (is (= (start) [#'state-1 #'state-2 #'state-2-a #'state-2-b #'state-3]) "Start all states in correct order.")
  (is (= (status) {#'state-1 :started #'state-2 :started #'state-2-a :started #'state-2-b :started #'state-3 :started}))
  (is (= @state-3 "state-1 + state-2 + state-3") "States can use othes states correctly.")
  (is (= (stop) [#'state-3 #'state-2-b #'state-2-a #'state-2 #'state-1]) "Stop all states in correct order.")
  (is (= (status) {#'state-1 :stopped #'state-2 :stopped #'state-2-a :stopped #'state-2-b :stopped #'state-3 :stopped})))

(deftest test-up-to
  (is (= (start #'state-2) [#'state-1 #'state-2]) "Start state 1 and 2")
  (is (= (status) {#'state-1 :started #'state-2 :started #'state-2-a :stopped #'state-2-b :stopped #'state-3 :stopped}))
  (is (= (start) [#'state-2-a #'state-2-b #'state-3]) "Start state 3")
  (is (= (status) {#'state-1 :started #'state-2 :started #'state-2-a :started #'state-2-b :started #'state-3 :started}))
  (is (= (stop #'state-2) [#'state-3 #'state-2-b #'state-2-a #'state-2]) "Stop state 3 and 2")
  (is (= (status) {#'state-1 :started #'state-2 :stopped #'state-2-a :stopped #'state-2-b :stopped #'state-3 :stopped})))

(deftest test-substitute-state
  (with-substitutes [#'state-1 (state :start "sub-1")] (start))
  (is (= @state-3 "sub-1 + state-2 + state-3") "State 1 is substituted by anonymous state.")
  (stop)
  (start)
  (is (= @state-3 "state-1 + state-2 + state-3") "State 1 is back to its original."))

(deftest test-falsy-start
  (is (state :start nil))
  (is (state :start false)))

(deftest test-substitute-map
  (with-substitutes [#'state-2 {:start-fn (fn [] "sub-2")}] (start))
  (is (= @state-3 "sub-2 + state-3") "State 2 is substituted by map.")
  (stop)
  (start)
  (is (= @state-3 "state-1 + state-2 + state-3") "State 2 is back to its original."))

(deftest test-start-error
  (is (thrown? ExceptionInfo
               (with-substitutes [#'state-1 (state :start (throw (ex-info "Boom!" {})))]
                 (start)))))

(deftest accessing-unstarted-throws-error
  (is (thrown? Error @state-1)))

(deftest test-status
  (is (= (status) {#'state-1 :stopped #'state-2 :stopped #'state-2-a :stopped #'state-2-b :stopped #'state-3 :stopped}))
  (start)
  (is (= (status) {#'state-1 :started #'state-2 :started #'state-2-a :started #'state-2-b :started #'state-3 :started})))

(deftest extra-data
  (is (= (:extra state-1) 'data)))

(deftest test-anonymous
  (let [stopped   (promise)
        anonymous (state :start 1 :stop (deliver stopped this))]
    (with-substitutes [#'state-1 anonymous]
      (start #'state-1)
      (stop))
    (is (and (realized? stopped) (= 1 @stopped)) "this is bound")))
