(ns mount.lite-test
  (:require [clojure.test :refer :all]
            [mount.lite :refer :all]
            [mount.lite-test.test-state-1 :refer (state-1)]
            [mount.lite-test.test-state-2 :refer (state-2)]
            [mount.lite-test.test-state-3 :refer (state-3)]))

;;; Helper functions.

(defn- statusses [& vars]
  (map (comp status* deref) vars))

(defmacro throws [& body]
  `(is (try ~@body false
            (catch Throwable t#
              t#))))

;;; Stop all states before and after every test, and reset on-reload.

(use-fixtures :each (fn [f] (stop) #_(on-reload nil) (f) (stop)))

;;; Tests

(deftest test-start-stop
  (is (= (start) [#'state-1 #'state-2 #'state-3]) "Start all states in correct order.")
  (is (= (statusses #'state-1 #'state-2 #'state-3) [:started :started :started]))
  (is (= @state-3 "state-1 + state-2 + state-3") "States can use othes states correctly.")
  (is (= (stop) [#'state-3 #'state-2 #'state-1]) "Stop all states in correct order.")
  (is (= (statusses #'state-1 #'state-2 #'state-3) [:stopped :stopped :stopped])))

(deftest test-up-to
  (is (= (start #'state-2) [#'state-1 #'state-2]) "Start state 1 and 2")
  (is (= (statusses #'state-1 #'state-2 #'state-3) [:started :started :stopped]))
  (is (= (start) [#'state-3]) "Start state 3")
  (is (= (statusses #'state-1 #'state-2 #'state-3) [:started :started :started]))
  (is (= (stop #'state-2) [#'state-3 #'state-2]) "Stop state 3 and 2")
  (is (= (statusses #'state-1 #'state-2 #'state-3) [:started :stopped :stopped])))

(deftest test-substitute-state
  (with-substitutes [#'state-1 (state :start "sub-1")] (start))
  (is (= @state-3 "sub-1 + state-2 + state-3") "State 1 is substituted by anonymous state.")
  (stop)
  (start)
  (is (= @state-3 "state-1 + state-2 + state-3") "State 1 is back to its original."))

(deftest test-substitute-map
  (with-substitutes [#'state-2 {:start-fn (fn [] "sub-2")}] (start))
  (is (= @state-3 "sub-2 + state-3") "State 2 is substituted by map.")
  (stop)
  (start)
  (is (= @state-3 "state-1 + state-2 + state-3") "State 2 is back to its original."))

#_(deftest test-on-cascade-skip
  (start)
  (require 'mount.lite-test.test-state-1 :reload)
  (is (= (statusses #'state-1 #'state-2 #'state-3) [:stopped :started :stopped]) "State 2 has :on-cascade :skip"))

#_(deftest test-on-reload-lifecycle
  (start)
  (require 'mount.lite-test.test-state-3 :reload)
  (is (= (statusses #'state-1 #'state-2 #'state-3) [:started :started :started]) "State 3 has :on-reload :lifecycle"))

#_(deftest test-on-reload-cascade
  (start)
  (is (= (statusses #'state-1 #'state-2 #'state-3) [:started :started :started]) "All states started")
  (require 'mount.lite-test.test-state-2 :reload)
  (is (= (statusses #'state-1 #'state-2 #'state-3) [:started :stopped :stopped]) "Both state 2 and 3 have stopped"))

#_(deftest test-on-reload-stop-override
  (start)
  (is (= (statusses #'state-1 #'state-2 #'state-3) [:started :started :started]) "All states started")
  (on-reload :stop)
  (require 'mount.lite-test.test-state-2 :reload)
  (is (= (statusses #'state-1 #'state-2 #'state-3) [:started :stopped :started]) "Only state 2 has stopped"))

#_(deftest test-on-reload-lifecycle-override
  (start)
  (is (= (statusses #'state-1 #'state-2 #'state-3) [:started :started :started]) "All states started")
  (on-reload :lifecycle)
  (in-ns 'mount.lite-test.test-state-2)
  (defstate state-2 :start "redef-2")
  (in-ns 'mount.lite-test)
  (is (= (statusses #'state-1 #'state-2 #'state-3) [:started :started :started]) "All states are still running")
  (is (= state-2 "state-1 + state-2") "State 2 still has original value")
  (stop)
  (start)
  (is (= state-2 "redef-2") "State 2 lifecycle was redefined")
  (require 'mount.lite-test.test-state-2 :reload))

(deftest test-start-error
  (throws (with-substitutes [#'state-1 (state :start (throw (ex-info "Boom!" {})))]
            (start))))

#_(deftest test-unmapped
  (ns-unmap 'mount.lite-test.test-state-3 'state-3)
  (is (= [#'state-1 #'state-2] (start)))
  (require 'mount.lite-test.test-state-3 :reload))

(deftest test-status
  (is (= {#'state-1 :stopped #'state-2 :stopped #'state-3 :stopped}))
  (start)
  (is (= {#'state-1 :started #'state-2 :started #'state-3 :started})))
