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

(defn- do-action [todonext action-f put-f next-f deps-f var]
  (action-f var)
  (let [todonext' (swap! (fn [[todo done _]]
                           (let [todo' (disj todo var)
                                 done' (conj done var)]
                             [todo' done' (remove #(some todo' (dep-f %)) (next-f var))])))]
    (doseq [var (:next todonext')]
      (put-f var))))

(defn- action
  [vars action-f next-f deps-f threads]
  (let [graph    (graph/var-graph vars)
        nodes    (dep/nodes graph)
        next-f   (partial next-f graph)
        deps-f   (partial deps-f graph)
        initial  (filter #(empty? (deps-f %)) nodes)
        queue    (local-queue initial)
        take-f   #(.poll queue)
        put-f    #(.add queue %)
        todonext (atom {:todo (set nodes) :done [] :next nil})
        task-f   (partial do-action todonext action-f put-f next-f deps-f)
        done-f   #(empty? (:todo @todonext))]
    (work task-f take-f done-f threads)
    (seq (:done @todonext))))

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
