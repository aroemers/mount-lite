(ns mount.extensions.autostart-test
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [mount.extensions.autostart :as sut]
            [mount.extensions.explicit-deps :as deps]
            [mount.lite :as mount]))

;;; Define extra states and clean them up afterwards.

(use-fixtures :each
  (fn [f]
    (mount/stop)

    (let [before-states @mount/*states*]

      (sut/defstate state-a
        :dependencies []
        :start 42)

      (sut/defstate state-b
        :dependencies []
        :start 420)

      (sut/defstate state-c
        :dependencies [#'state-a]
        :start (* 2 @state-a))

      (try
        (f)
        (finally
          (mount/stop)
          (reset! mount/*states* before-states))))))


;;; Actual tests

(defn is-status
  [statuses]
  (is (= statuses (select-keys (mount/status) (keys statuses)))))

(deftest autostart-test
  (testing "with default autostart-fn"
    (is (= 84 @state-c))
    (is-status {#'state-a :started #'state-b :started #'state-c :started})
    (mount/stop)
    (is-status {#'state-a :stopped #'state-b :stopped #'state-c :stopped}))

  (testing "with custom autostart-fn (explicit-deps)"
    (try
      (sut/set-autostart-fn! deps/start)
      (is (= 84 @state-c))
      (is-status {#'state-a :started #'state-b :stopped #'state-c :started})
      (mount/stop)
      (is-status {#'state-a :stopped #'state-b :stopped #'state-c :stopped})
      (finally
        (sut/set-autostart-fn! mount/start)))))
