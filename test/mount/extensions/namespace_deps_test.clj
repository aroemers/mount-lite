(ns mount.extensions.namespace-deps-test
  (:require [clojure.test :as test :refer (deftest is)]
            [mount.extensions.namespace-deps :as sut]
            [mount.lite :as mount]
            [mount.lite-test.test-state-1 :as ts1 :refer (state-1)]
            [mount.lite-test.test-state-2 :as ts2 :refer (state-2)]
            [mount.lite-test.test-state-2-extra :as ts2-extra :refer (state-2-a state-2-b)]
            [mount.lite-test.test-state-3 :as ts3 :refer (state-3)]))

;;; Stop all states before and after every test.

(test/use-fixtures :each (fn [f] (mount/stop) (f) (mount/stop)))


;;; Tests.

(deftest build-graphs-test
  (is (= {:dependencies {::ts2/state-2         #{::ts1/state-1}
                         ::ts2-extra/state-2-a #{::ts1/state-1}
                         ::ts2-extra/state-2-b #{::ts2-extra/state-2-a ::ts1/state-1}
                         ::ts3/state-3         #{::ts2-extra/state-2-b ::ts2/state-2 ::ts2-extra/state-2-a ::ts1/state-1}}
          :dependents   {::ts1/state-1         #{::ts2-extra/state-2-b ::ts2/state-2 ::ts2-extra/state-2-a ::ts3/state-3}
                         ::ts2/state-2         #{::ts3/state-3}
                         ::ts2-extra/state-2-a #{::ts2-extra/state-2-b ::ts3/state-3}
                         ::ts2-extra/state-2-b #{::ts3/state-3}}}
         (into {} (sut/build-graphs)))))

(deftest start-test
  (is (= (sut/start #'state-2) [#'state-1 #'state-2]))
  (mount/stop)
  (is (= (sut/start #'state-2-b) [#'state-1 #'state-2-a #'state-2-b])))

(deftest stop-test
  (mount/start)
  (is (= (sut/stop #'state-2) [#'state-3 #'state-2]))
  (mount/start)
  (is (= (sut/stop #'state-2-b) [#'state-3 #'state-2-b])))
