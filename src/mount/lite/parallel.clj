(ns mount.lite.parallel
  "Namepace responsible for starting and stopping a state var
  dependency graph using a Thread pool."
  {:no-doc true}
  (:require [clojure.set :as set]
            [clojure.tools.namespace.dependency :as dep]
            [mount.lite.graph :as graph])
  (:import [java.util.concurrent Executors TimeUnit]))

(defn- fixed-pool [t]
  (Executors/newFixedThreadPool t))

(defn- work-task [pool thrown task-f done-f task]
  (fn []
    (try
      (task-f task)
      (let [[done? forks] (done-f task)]
        (if-not done?
          (doseq [fork forks]
            (.submit pool (work-task pool thrown done-f task-f fork)))
          (.shutdown pool)))
      (catch Throwable t
        (.shutdown pool)
        (deliver thrown t)))))

(defn- work [threads task-f done-f tasks]
  (when (seq tasks)
    (let [pool   (fixed-pool threads)
          thrown (promise)]
      (doseq [task tasks]
        (.submit pool (work-task pool thrown task-f done-f task)))
      (.awaitTermination pool 24 TimeUnit/HOURS)
      (when (realized? thrown)
        (throw @thrown)))))

(defn- action [vars action-f next-f deps-f threads]
  (let [graph   (graph/var-graph vars)
        acc     (atom {:todo (set (dep/nodes graph)) :done () :next nil})
        done-f  (fn [var]
                   (let [acc' (swap! acc
                                     (fn [{:keys [todo done]}]
                                       (let [todo' (disj todo var)
                                             done' (cons var done)]
                                         {:todo todo' :done done'
                                          :next (remove #(some todo' (deps-f graph %))
                                                        (next-f graph var))})))]
                     [(empty? (:todo acc')) (:next acc')]))
        initial (filter #(empty? (deps-f graph %)) (dep/nodes graph))]
    (work threads action-f done-f initial)
    (reverse (:done @acc))))

(defn start
  "Start the given vars, in parallel where applicable, using the given
  start-f function and a maximum number of threads."
  [vars start-f threads]
  (let [next-f dep/immediate-dependents
        deps-f dep/immediate-dependencies]
    (action vars start-f next-f deps-f threads)))

(defn stop
  "Stop the given vars, in parallel where applicable, using the given
  start-f function and a maximum number of threads."
  [vars stop-f threads]
  (let [next-f dep/immediate-dependencies
        deps-f dep/immediate-dependents]
    (action vars stop-f next-f deps-f threads)))
