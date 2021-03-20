(ns mount.extensions.autostart-test
  (:require [clojure.test :refer [deftest is testing]]
            [mount.extensions.autostart :as sut]
            [mount.extensions.explicit-deps :as deps]
            [mount.lite :as mount]))

(sut/defstate first
  :dependencies []
  :start 42)

(sut/defstate second
  :dependencies []
  :start 420)

(sut/defstate third
  :dependencies [#'first]
  :start (* 2 @first))

(defn in-state?
  [states]
  (= (zipmap [#'first #'second #'third] states) (mount/status)))

(deftest autostart-test
  (testing "with default autostart-fn"
    (= 84 @third)
    (is (in-state? [:started :started :started]))
    (mount/stop)
    (is (in-state? [:stopped :stopped :stopped])))
  (testing "with custom autostart-fn"
    (try
      (sut/set-autostart-fn! deps/start)
      (= 84 @third)
      (testing "explicit deps still only starts states in its deps tree"
        (is (in-state? [:started :stopped :started])))
      (mount/stop)
      (is (in-state? [:stopped :stopped :stopped]))
      (finally
        (sut/set-autostart-fn! mount/start)))))
