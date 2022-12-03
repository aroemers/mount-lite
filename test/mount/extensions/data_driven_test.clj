(ns mount.extensions.data-driven-test
  (:require [clojure.test :refer (deftest is use-fixtures)]
            [mount.extensions.data-driven :as dd]
            [mount.lite :refer [state start stop status]]
            [mount.lite-test.test-state-1 :refer (state-1)]
            [mount.lite-test.test-state-2 :refer (state-2)]
            [mount.lite-test.test-state-3 :refer (state-3)]))

;;; Stop all states before and after every test.

(use-fixtures :each (fn [f] (stop) (f) (stop)))

;;; Actual tests

(def substitute (state :start "substituted"))

(deftest with-config-test
  (dd/with-config '{:only        [mount.lite-test.test-state-1/state-1
                                  mount.lite-test.test-state-2/state-2]
                    :except      [mount.lite-test.test-state-2/state-2]
                    :substitutes [mount.lite-test.test-state-1/state-1
                                  mount.extensions.data-driven-test/substitute]}
    (start))
  (is (status) {#'state-1 :started #'state-2 :stopped #'state-3 :stopped})
  (is @state-1 "substituted"))
