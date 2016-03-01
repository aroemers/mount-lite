# Substitutions

## What substitutions are for

In the tests of your application, or whenever developing it, you may want to mock the lifecycle expressions of a `defstate`.
This way, the var will yield another value when started.

## Passing substitutions to (start)

To substitute a state, it must be passed to the `start` function using the `substitute` function (or with plain data, as described in the section about other start and stop options).
A substitute is not defined with `defstate`, but with the `state` macro, or with a plain map holding at least a 0-arity `:start` function.
For example:

```clj
(mount/start (substitute #'db (state :start (do (println "Starting fake DB") (atom {}))
                                     :stop  (println "Stopping fake DB"))))
;>> Starting fake DB
;=> (#'your.app/db)

db
;=> object[clojure.lang.Atom 0x2010a30b {:status :ready, :val {}}]

(mount/stop)
;>> Stopping fake DB
;=> (#'your.app/db #'your.app.config/config)
```

A substitution is only active for the current session.
Thus, starting the state again (after is has been stopped), without a substitute configured for it, will start it with the original definition.

Note that substitution states don't need to be inline and the `state` macro is also only for convenience.
For example, the following is also possible:

```clj
(def sub {:start (fn [] (atom {}))})

(mount/start (substitute #'db sub))
```
