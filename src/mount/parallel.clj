(ns mount.parallel
  (:require [clojure.set :as set]
            [com.stuartsierra.dependency :as dep]
            [mount.graph :as graph])
  (:import [java.util.concurrent ConcurrentLinkedQueue]))

;;---TODO Improve simplistic prove of concept worker
(defn- local-queue
  ([] (ConcurrentLinkedQueue.))
  ([xs] (ConcurrentLinkedQueue. xs)))

(defn- work [task-f take-f done-f threads]
  (->> #(future (loop [task (take-f)]
                  (when-not (done-f)
                    (if task
                      (task-f task)
                      (Thread/sleep 50))
                    (recur (take-f)))))
       (repeatedly threads)
       (doall)
       (run! deref)))

(defn- do-action [todo done action-f put-f next-f deps-f var]
  (action-f var)
  (send todo (fn [vars]
               (swap! done conj var)
               (let [updated (disj vars var)]
                 (doseq [dependent (next-f var)]
                   (when-not (some updated (deps-f dependent))
                     (put-f dependent)))
                 updated)))
  (await todo))

(defn- action
  [vars action-f next-f deps-f threads]
  (let [graph   (graph/var-graph vars)
        nodes   (dep/nodes graph)
        next-f  (partial next-f graph)
        deps-f  (partial deps-f graph)
        initial (filter #(empty? (deps-f %)) nodes)
        queue   (local-queue initial)
        take-f  #(.poll queue)
        put-f   #(.add queue %)
        todo    (agent (set nodes))
        done    (atom [])
        task-f  (partial do-action todo done action-f put-f next-f deps-f)
        done-f  #(empty? @todo)]
    (work task-f take-f done-f threads)
    (seq @done)))

(defn start
  [vars start-f threads]
  (let [next-f dep/immediate-dependents
        deps-f dep/immediate-dependencies]
    (action vars start-f next-f deps-f threads)))

(defn stop
  [vars stop-f threads]
  (let [next-f dep/immediate-dependencies
        deps-f dep/immediate-dependents]
    (action vars stop-f next-f deps-f threads)))

(comment
  (do
    ;; Make following state namespaces:
    ;;
    ;;         . mid1 .
    ;;  core -{        } end
    ;;         ` mid2 '
    ;;
    (ns end (:require [mount.lite :refer (defstate)]))
    (defstate end
      :start (do (println "Starting end...") (Thread/sleep 500) (println "Started end."))
      :stop (do (println "Stopping end...") (Thread/sleep 500) (println "Stopped end.")))

    (ns mid1 (:require [mount.lite :refer (defstate)] [end :as end]))
    (defstate mid1
      :start (do (println "Starting mid1...") (Thread/sleep 500) (println "Started mid1."))
      :stop (do (println "Stopping mid1...") (Thread/sleep 500) (println "Stopped mid1.")))

    (ns mid2 (:require [mount.lite :refer (defstate)] [end :as end]))
    (defstate mid2
      :start (do (println "Starting mid2...") (Thread/sleep 500) (println "Started mid2."))
      :stop (do (println "Stopping mid2...") (Thread/sleep 500) (println "Stopped mid2.")))

    (ns core (:require [mount.lite :refer (defstate)] [mid1 :as mid1]))
    (defstate core
      :start (do (println "Starting core...") (Thread/sleep 500) (println "Started core."))
      :stop (do (println "Stopping core...") (Thread/sleep 500) (println "Stopped core."))))


  ;; Check startup time, should be below 2000 msecs, and starting mid1 and mid2 in
  ;; parallel.
  (time (mount.lite/start (parallel 2)))
  ;;>> "Elapsed time: 1614.474151 msecs"
  ;;=> (#'end/end #'mid1/mid1 #'mid2/mid2 #'core/core)

  ;; Check stop, should stop core and mid2 in parallel.
  (mount.lite/stop (parallel 2))


  ;; Check different behaviour of up-to, where mid2 should be left out, as it is not
  ;; needed for #'core/core
  (mount.lite/start (up-to #'core/core))
  ;;=> (#'end/end #'mid1/mid1 #'core/core)

  )
