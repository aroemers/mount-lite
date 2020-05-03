(ns mount.lite-test
  (:require [clojure.test :as test :refer [deftest is testing]]
            [mount.extensions.basic :as basic]
            [mount.lite :as sut]
            [mount.protocols :as protocols]
            [reporting :as reporting])
  (:import clojure.lang.ExceptionInfo))

;;; Setup.

(reporting/nice-reporting)

(defn states [f]
  (def stopped (atom {}))

  (basic/with-only

    [(sut/defstate foo :start 1          :stop (swap! stopped assoc foo @foo))
     (sut/defstate bar :start (inc @foo) :stop (swap! stopped assoc bar @bar))
     (sut/defstate cux :start (inc @bar) :stop (swap! stopped assoc cux @cux))]

    (f)

    ;; TODO Should retreiving system keys be part of the API?
    (doseq [system-key (keys @mount.implementation.statevar/systems)]
      (sut/with-system-key system-key
        (sut/stop)))))

(test/use-fixtures :each states)


;;; Test cases.

(deftest test-state

  (testing "Macro `state` should create an IState implementation"
    (let [state (sut/state :start 1 :stop this)]
      (is (satisfies? protocols/IState state))

      (testing "which can start"
        (is (= 1 (protocols/start state))))

      (testing "which can stop, using implicit this"
        (is (= 1 (protocols/stop state)))))

    (testing "except when incorrect arguments are supplied"
      (is (thrown? AssertionError (eval `(sut/state :staart 1 :stoop 2)))))))


(deftest test-defstate

  (testing "Macro `defstate` should define a global state"

    (testing "without any expression"
      (is (var? (sut/defstate foo))))

    (testing "with a start expression"
      (is (var? (sut/defstate foo :start 1))))

    (testing "with a stop expression refering to itself"
      (is (var? (sut/defstate foo :start 1 :stop @foo))))

    (testing "except when incorrect arguments are supplied"
      (is (thrown? AssertionError (eval `(sut/defstate :staart 1 :stoop 2)))))

    (testing "with redefinitions considered equal"
      (let [current foo]
        (sut/defstate foo :start 'new :stop 'and-shiny)
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

  (testing "Calling `start` should"

    (testing "return the started states"
      (is (= (list foo bar cux) (sut/start)))

      (testing "but should not start them again"
        (is (= () (sut/start)))))

    (testing "start the states"
      (is (= [:started :started :started]
             [(protocols/status foo)
              (protocols/status bar)
              (protocols/status cux)])))

    (testing "make the started values available"
      (is (= [1 2 3] [@foo @bar @cux])))))


(deftest test-start-up-to

  (testing "Calling `start` with an argument should"

    (testing "return started states up to that argument"
      (is (= (list foo bar) (sut/start bar))))

    (testing "have started only the states up to that argument"
      (is (= [:started :started :stopped]
             [(protocols/status foo)
              (protocols/status bar)
              (protocols/status cux)])))

    (testing "error when a non-state argument is supplied"
      (is (thrown? AssertionError (sut/start :whut))))))


(deftest test-stop

  (sut/start)

  (testing "Calling `stop` should"

    (testing "return the stopped states"
      (is (= (list cux bar foo) (sut/stop))))

    (testing "stop the states"
      (is (= [:stopped :stopped :stopped]
             [(protocols/status foo)
              (protocols/status bar)
              (protocols/status cux)]))

      (testing "having executed the stop expression"
        (is (= {foo 1 bar 2 cux 3} @stopped))))

    (testing "make the started value unavailable"
      (is (thrown? ExceptionInfo @foo)))))


(deftest test-stop-up-to

  (sut/start)

  (testing "Calling `stop` with an argument should"

    (testing "return stopped states up to that argument"
      (is (= (list cux bar) (sut/stop bar))))

    (testing "have stopped only the states up to that argument"
      (is (= [:started :stopped :stopped]
             [(protocols/status foo)
              (protocols/status bar)
              (protocols/status cux)])))

    (testing "error when a non-state argument is supplied"
      (is (thrown? AssertionError (sut/stop :whut))))))


(deftest test-status

  (testing "Calling `status` should retuns a map with all states"

    (testing "stopped"

      (is (= {foo :stopped bar :stopped cux :stopped}
             (sut/status))))

    (testing "started"

      (sut/start)
      (is (= {foo :started bar :started cux :started}
             (sut/status))))

    (testing "mixed"

      (sut/stop bar)
      (is (= {foo :started bar :stopped cux :stopped}
             (sut/status))))))


(deftest test-with-system-key

  (testing "Using `with-system-key` should"

    (testing "be able to start multiple systems"

      (let [alice (sut/with-system-key :alice (sut/start))
            _     (sut/defstate foo :start 2)
            bob   (sut/with-system-key :bob (sut/start))]
        (is (= (list foo bar cux) alice bob))))

    (testing "should not have influenced the default system"

      (is (= (list foo bar cux) (sut/start))))

    (testing "be able to acces different states"

      (is (= [1 2]
             [(sut/with-system-key :alice @foo)
              (sut/with-system-key :bob @foo)])))

    (testing "be able to stop multiple systems"

      (let [alice (sut/with-system-key :alice (sut/stop))
            bob   (sut/with-system-key :bob (sut/stop))]
        (is (= (list cux bar foo) alice bob))))))


(deftest test-with-substitutes

  (testing "Using `with-substitutes` should"

    (let [sub (sut/state :start :sub-start :stop (swap! stopped assoc :sub :sub-stop))]

      (testing "not accept non-defstate keys"

        (is (thrown? AssertionError (sut/with-substitutes {:wrong sub}))))

      (testing "not accept non-state values"

        (is (thrown? AssertionError (sut/with-substitutes {foo :wrong}))))

      (testing "substitute the start logic"

        (sut/with-substitutes {foo sub}
          (protocols/start foo))

        (is (= :sub-start @foo)))

      (testing "substitute the stop logic"

        (protocols/stop foo)

        (is (= :sub-stop (-> stopped deref :sub)))))))


(deftest test-with-system-map

  (testing "Using `with-system-map` should"

    (testing "not accept non-defstate keys"

      (is (thrown? AssertionError (sut/with-system-map {:wrong 123}))))

    (testing "make the given value available as state value"

      (sut/with-system-map {foo 123}

        (is (= 123 @foo)))

      (testing "even when the defstate is started"

        (sut/start foo)

        (sut/with-system-map {foo 123}

          (is (= 123 @foo)))))

    (testing "not influence the state value outside its scope"

      (is (= 1 @foo)))))


(deftest test-compatibility-with-mount-lite-2

  (testing "Backwards compatibility ensures that"

    (testing "a var can be given to `start`"

      (is (= (list foo) (sut/start #'foo))))

    (testing "a var can be given to `stop`"

      (is (= (list foo) (sut/stop #'foo))))

    (testing "a vector of vars can be given to `with-substitutes`"

      (sut/with-substitutes [#'foo (sut/state :start 222)]
        (sut/start foo)

        (is (= 222 @foo))))))
