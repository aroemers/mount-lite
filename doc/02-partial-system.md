# Start/stop system partially

## The "up-to" feature

In many cases, calling just `(start)` or `(stop)` is enough, as it start your entire system.
But there may be circumstances, for instance in your tests, that you don't want to start all of your global states.

Therefore the `start` and `stop` functions can take one optional "up-to" argument, a global state.
It is that state that is started (or stopped), including all its dependencies (or dependents).
It allows you to easily start or stop only a part of your system.

Note that the default behaviour is that the dependencies are calculated based on the sequence of `defstates` definitions as registered in mount-lite.
In effect, it might start (or stop) a bit more than the given state really depends on.
If you need more graph-like behaviour, have a look at the [namespace-deps extension](api/extensions/namespace-deps).

This "up-to" behaviour is unique to mount-lite.
There used to be other options to influence what is started or stopped, but those turned out to complicate the API and were rarely used.
If however you want more control, have a look at the [basic extension](api/extensions/basic) or how to create your own extionsion using mount-lite its [extension point](extending-mount).

## Example source code

```clj
(defstate a)
(defstate b)
(defstate c)

(start b)
;=> (user/a user/b)

(start)
;=> (user/c)

(stop b)
;=> (user/c user/b)

(stop)
;=> (user/a)
```
