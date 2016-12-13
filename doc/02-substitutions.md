# Substitutions

## What substitutions are for

In the tests of your application, or when developing it, you may want to mock the lifecycle expressions of a `defstate`.
This way, the global state will result in another value when started.

## Setting substitutions for _(start)_

To substitute a state, the call to the `start` function must be wrapped by the `with-substitutes` macro.
A substitute is not defined with `defstate`, but with the `state` macro.
For example:

```clj
(mount/with-substitutes [#'db (state :start (do (println "Starting fake DB") (atom {}))
                                     :stop  (println "Stopping fake DB"))]
  (start))
;>> Starting fake DB
;=> (#'your.app/db)

@db
;=> object[clojure.lang.Atom 0x2010a30b {:status :ready, :val {}}]

(mount/stop)
;>> Stopping fake DB
;=> (#'your.app/db #'your.app.config/config)
```

A substitution is only active for the current session.
Thus, starting the state again (after is has been stopped), without a substitute configured for it, will start it with the original definition.

Note that substitution states don't need to be inline.
And the `with-substitutes` wrappers can be nested.
For example, the following is also possible:

```clj
(def sub (state :start {}))

(mount/with-substitutes [#'db sub]
  ...
  (mount/with-substitutes [#'config sub]
    ...
    (mount/start)
    ...))
```
