(ns mount.reporting
  (:require  [clojure.test :as test]))


(defn nice-reporting []

  (defmethod clojure.test/report :pass [m]
    (test/with-test-out
      (test/inc-report-counter :pass)
      (when (seq test/*testing-contexts*)
        (println "✅ " (test/testing-contexts-str)))))

  (defmethod test/report :fail [m]
    (test/with-test-out
      (test/inc-report-counter :fail)
      (println "\n💥" (if (seq test/*testing-contexts*)
                        (test/testing-contexts-str)
                        (str "Failed in " (test/testing-vars-str m))))
      (when (seq test/*testing-contexts*)
        (println "  failed:" (test/testing-vars-str m)))
      (when-let [message (:message m)]
        (println message))
      (println "expected:" (pr-str (:expected m)))
      (println "  actual:" (pr-str (:actual m)) "\n"))))
