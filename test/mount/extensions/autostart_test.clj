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
  :dependencies [#'first #'second]
  :start (+ @first @second))

(defn fully?
  [state]
  (= {#'first state #'second state #'third state} (mount/status)))

(deftest autostart-test
  (testing "with default autostart-fn"
    (= 462 @third)
    (is (fully? :started))
    (mount/stop)
    (is (fully? :stopped)))
  (testing "with custom autostart-fn"
    (try
      (sut/set-autostart-fn! deps/start)
      (= 462 @third)
      (is (fully? :started))
      (mount/stop)
      (is (fully? :stopped))
      (finally
        (sut/set-autostart-fn! mount/start)))))
