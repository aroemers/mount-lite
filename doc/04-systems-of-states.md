# Multiple systems of states

The 2.0 of mount-lite version introduced the ability to run multiple systems of states simultaneously.
In other words, a single `defstate` can be started multiple times in different contexts.

The mount-lite library does this in a transparent way, i.e. there is no difference in how the library is used, whether this feature is utilized or not.
This is in contrast with the [yurt](https://github.com/tolitius/yurt) library from the original `mount`.

When you just `(start)` your states, you start them in the "root" context.
The entire application accesses those states as normal.
By using the `with-session` macro, a new thread is spawned that executes the body of that macro.
All global states are initially in the stopped status in this thread, regardless of the status in the thread that spawns this new session.
By calling `(start)` in the body of this session, one effectively creates a parallel system of states.
The spawned thread and its subthreads will automatically use the states that are started within this thread.
The states themselves and the code that uses them does not change a bit.

Exiting the spawned thread - i.e. when the body is done - automatically stops all states in this session.

## Example

For example, below we are use the `with-session` macro to spin up a new thread and execute its body.
The example starts up two systems of states, and the use of [`CountDownLatch`](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/CountDownLatch.html)es ensures the systems run concurrently when printing the value of the `foo` state.

```clj
;; Define a simple state
(defstate foo
  :start 1
  :stop (println "stopping"))

;; Define latches for synchronizing threads
(def started (CountDownLatch. 2))
(def stopping (CountDownLatch. 2))

;; Start the states in the "root" session.
(start)

;; Spawn a new session thread.
(with-session
  ;; Use substitutes to alter the standard defstate behaviour
  (with-substitutes [#'foo (state :start 2 :stop (println "sub stopping"))]
    ;; Start the states in this new session.
    (start))
  ;; Set this thread as started and wait for the other to have started.
  (doto started .countDown .await)
  ;; Spawn a subthread that prints the value of foo, to demonstrate
  ;; subthreads have access to the session just as well.
  (thread
    (println @foo)
    ;; Ready to stop
    (.countDown stopping))
  ;; Wait for the root session to be ready to stop as well.
  (.await stopping)
  ;; This session stops automatically.
  )

;; Set this thread to started and wait for the other to have started.
(doto started .countDown .await)
;; Print the value of foo in this context.
(println @foo)
;; Signal to stop and wait for the other to signal the same.
(doto stopping .countDown .await)

;; Stop the root session.
(stop)
```

The printed output is shown below.
As you can see, two different values are printed, and subthreads use running states from parent threads.
The order in which `1` and `2` are printed, or the order in which `stopping` and `sub stopping` are printed, might of course be swapped.

```
1
2
stopping
sub stopping
```

## Watch out for ThreadPools

By spawning a new thread with a new system of states using the `with-session` macro, the subthreads created from that session automatically use the system of states from that session.
But, if you use threads in your session that were created _outside_ of your session, those threads use the system of states from the (possibly default root) session in which they were created.
So, do keep in mind that `future`s and `agent`s use global ThreadPools from the Clojure runtime.
There are simple ways to work around this though, and mount-lite may even provide utilities for this at a later stage.
