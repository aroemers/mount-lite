(ns mount.aot-test
  (:require [mount.lite :as mount :refer (defstate)]))

(defstate aot
  :start "started")

(defn -main [& args]
  (mount/start)
  (System/exit (compare aot "started")))
