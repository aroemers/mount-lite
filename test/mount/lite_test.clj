(ns mount.lite-test
  (:require [clojure.test :refer [deftest testing is]]
            [mount.lite :as sut]
            [mount.protocols :as protocols]))

(deftest test-state
  (testing "state should create an IState implementation"
    (let [state (sut/state :start 1 :stop this)]
      (is (satisfies? protocols/IState state))

      (testing "which should start"
        (is (= (protocols/start state) 1)))

      (testing "which should stop, with access to this"
        (is (= (protocols/stop state) 1))))))
