(ns mount.extensions.explicit-deps-test
  (:require [clojure.test :refer (deftest is use-fixtures testing)]
            [mount.extensions.explicit-deps :as deps]
            [mount.lite :refer (start state status stop with-substitutes)]
            [mount.lite-test.test-state-1 :as ts1 :refer (state-1)]
            [mount.lite-test.test-state-2 :as ts2 :refer (state-2)]
            [mount.lite-test.test-state-2-extra :as ts2e]
            [mount.lite-test.test-state-3 :as ts3 :refer (state-3)]))

;;; Stop all states before and after every test.

(use-fixtures :each (fn [f] (stop) (f) (stop)))

;;; Tests.

(def dont-need-anyone (state :start "appeltaart" :dependencies nil))

(deftest build-graphs-test
  (testing "no substitutes used"
    (let [graph (deps/build-graphs)]
      (is (= graph
             {:dependencies {::ts1/state-1    #{}
                             ::ts2/state-2    #{::ts1/state-1}
                             ::ts2e/state-2-a #{::ts1/state-1}
                             ::ts2e/state-2-b #{::ts2e/state-2-a}
                             ::ts3/state-3    #{::ts2/state-2}}
              :dependents   {::ts1/state-1    #{::ts2/state-2 ::ts2e/state-2-a}
                             ::ts2/state-2    #{::ts3/state-3}
                             ::ts2e/state-2-a #{::ts2e/state-2-b}
                             ::ts2e/state-2-b #{}
                             ::ts3/state-3    #{}}}))))

  (testing "substitutes used"
    (let [graph (with-substitutes [#'state-2 dont-need-anyone]
                  (deps/build-graphs))]
      (is (-> graph :dependencies ::ts2/state-2) #{}))))

(deftest start-test
  (with-substitutes [#'state-2 dont-need-anyone]
    (deps/start #'state-3))
  (is (status) {#'state-1 :stopped #'state-2 :started #'state-3 :started}))

(deftest stop-test
  (with-substitutes [#'state-2 dont-need-anyone]
    (start))
  (deps/stop #'state-1)
  (is (status) {#'state-1 :stopped #'state-2 :started #'state-3 :started}))
