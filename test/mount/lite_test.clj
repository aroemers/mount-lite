(ns mount.lite-test
  (:require [clojure.test :refer [deftest testing is]]
            [mount.lite :as sut]
            [mount.protocols :as protocols]
            mount.report))

(deftest test-state
  (testing "Macro `state` should create an IState implementation"
    (let [state (sut/state :start 1 :stop this)]
      (is (satisfies? protocols/IState state))

      (testing "which can start"
        (is (= 1 (protocols/start state))))

      (testing "which can stop, using implicit this"
        (is (= 1 (protocols/stop state)))))))


(deftest test-defstate
  (testing "Macro `defstate` should define a global state"

    (testing "without any expression"
      (is (var? (sut/defstate foo))))

    (testing "with a start expression"
      (is (var? (sut/defstate foo :start 1))))

    (testing "with a stop expression refering to itself"
      (is (var? (sut/defstate foo :start 1 :stop @foo))))

    (testing "with redefinitions considered equal"
      (sut/defstate foo :start 1 :stop 2)
      (let [current foo]
        (sut/defstate foo :start 2 :stop 3)
        (is (= current foo))))

    (testing "which can be dereferenced once started"
      (sut/defstate foo :start 1)
      (protocols/start foo)
      (try
        (is (= 1 @foo))
        (finally
          (protocols/stop foo))))

    (testing "without overriding started stop logic"
      (let [val (atom :untouched)]
        (sut/defstate foo :stop (reset! val :correct))
        (protocols/start foo)
        (sut/defstate foo :stop (reset! val :wrong))
        (protocols/stop foo)
        (is (= :correct @val))))))


(deftest test-start

  (sut/defstate foo :start 1 :stop (reset! val :correct))
  (protocols/stop foo)

  (testing "Calling `start`"

    (testing "should return the started states"
      (is (= (list foo) (sut/start))))

    (testing "should have started the states"
      (is (= 1 @foo)))))
