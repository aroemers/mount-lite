(ns mount.extensions.basic-test
  (:require [clojure.test :refer (deftest is use-fixtures)]
            [mount.extensions.basic :as basic]
            [mount.lite :refer [start stop status]]
            [mount.lite-test.test-state-1 :refer (state-1)]
            [mount.lite-test.test-state-2 :refer (state-2)]
            [mount.lite-test.test-state-3 :refer (state-3)]))

;;; Stop all states before and after every test.

(use-fixtures :each (fn [f] (stop) (f) (stop)))

;;; Actual tests

(deftest with-only-test
  (is (basic/with-only [#'state-1]
        (start)
        (status))
      {#'state-1 :started #'state-2 :stopped #'state-3 :stopped}))

(deftest with-except-test
  (is (basic/with-except [#'state-3]
        (start)
        (status))
      {#'state-1 :started #'state-2 :started #'state-3 :stopped}))
