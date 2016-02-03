(ns mount.parallel
  "Namepace responsible for starting and stopping a state var
  dependency graph using a Thread pool."
  (:require [clojure.set :as set]
            [com.stuartsierra.dependency :as dep]
            [mount.graph :as graph])
  (:import [java.util.concurrent Executors TimeUnit]))

(defn- fixed-pool [t]
  (Executors/newFixedThreadPool t))

(defn- action [vars action-f next-f deps-f threads]
  (let [graph       (graph/var-graph vars)
        pool        (fixed-pool threads)
        thrown      (promise)
        todonext    (atom {:todo (set (dep/nodes graph)) :done () :next nil})
        mk-task-f   #(try (%) (catch Throwable e (deliver thrown e) (.shutdown pool)))
        next-task-f (fn next-task-f [var]
                      (let [todonext' (swap! todonext
                                             (fn [{:keys [todo done]}]
                                               (let [todo' (disj todo var)
                                                     done' (cons var done)]
                                                 {:todo todo' :done done'
                                                  :next (remove #(some todo' (deps-f graph %))
                                                                (next-f graph var))})))]
                        (if (and (empty? (:todo todonext')) (empty? (:next todonext')))
                          (.shutdown pool)
                          (doseq [next (:next todonext')]
                            (.submit pool (mk-task-f #(do (action-f next) (next-task-f next))))))))]
    (doseq [var (filter #(empty? (deps-f graph %)) (dep/nodes graph))]
      (.submit pool (mk-task-f #(do(action-f var) (next-task-f var)))))
    (.awaitTermination pool 24 TimeUnit/HOURS) ;;---TODO Make configurable?
    (when (realized? thrown)
      (throw @thrown))
    (reverse (:done @todonext))))

(defn start [vars start-f threads]
  (let [next-f dep/immediate-dependents
        deps-f dep/immediate-dependencies]
    (action vars start-f next-f deps-f threads)))

(defn stop [vars stop-f threads]
  (let [next-f dep/immediate-dependencies
        deps-f dep/immediate-dependents]
    (action vars stop-f next-f deps-f threads)))
