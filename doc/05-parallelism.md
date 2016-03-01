# Parallelism

A unique feature of mount-lite is being able to start and stop the defstates in parallel to each other, wherever applicable.
It does this by calculating a dependency graph of all the states (just like it does with the `:up-to` option described in the section about start/stop options).
The defstates are started (or stopped) as eagerly as possible using a - user specified - number of threads.

> NOTE: To visualize the dependency graph of all the states, one can use the `dot` function.

Defstates by default depend on the other defstates in the same namespace defined above them, and on the defstates in the referenced namespaces (transitively).
So, the parallelism is normally to be gained on a namespace level.

## An example

The following example shows how parallelism works:

```clj
;; Make following defstate graph
;;
;;  core ----> mid1 -.
;;                    }-> end
;;             mid2 -'
;;
;; Note that the :dependencies metadata is normally an
;; optional and advanced option.

(defstate end
  :start (do (println "Starting 'end'  at 0")
             (let [start (System/currentTimeMillis)]
               (Thread/sleep 500)
               start)))

(defstate mid1
  {:dependencies #{#'end}}
  :start (do (println "Starting 'mid1' at"
                      (- (System/currentTimeMillis) end))
             (Thread/sleep 500)))

(defstate mid2
  {:dependencies #{#'end}}
  :start (do (println "Starting 'mid2' at"
                      (- (System/currentTimeMillis) end))
             (Thread/sleep 500)))

(defstate core
  {:dependencies #{#'mid1}}
  :start (do (println "Starting 'core' at"
                      (- (System/currentTimeMillis) end))
             (Thread/sleep 500)))

;; Test the parallelism:

(time (mount.lite/start (mount.lite/parallel 2)))
;>> Starting 'end'  at 0 ...
;>> Starting 'mid1' at 500 ...
;>> Starting 'mid2' at 501 ...
;>> Starting 'core' at 1006 ...
;>> "Elapsed time: 1791.079279 msecs"
;=> (#'end/end #'mid1/mid2 #'mid2/mid1 #'core/core)
```

In above example one can see that states `mid1` and `mid2` are started almost simultaneously.
Note that when one would stop above example in parallel, the states `core` and `mid2` will be stopped simultaneously.

## The :dependencies metadata

If you really want to get the most out of parallelism, you can declare the dependencies on a state by putting `:dependencies` in its metadata.
This way states don't necessarily depend on other states in the same namespace or referenced namespaces.
