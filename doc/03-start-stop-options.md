# Start up to / stop down to

In many cases, calling just `(mount/start)` or `(mount/stop)` is enough.
But there may be circumstances, for instance in your tests, that you don't want to all of your global states.

The `start` and `stop` functions can take one argument, a var.
The var points to a defstate that should be started (or stopped), including all its dependencies (or dependents).
It allows you to easily start or stop only a part of your application.

> This "up-to" behaviour is unique to mount-lite.
> There used to be other options to influence what is started or stopped, but those turned out to anti-patterns and rarely used.
> If you need more options though, have a look at the "extension point" section.

An example:

```clj
(defstate a :start nil)
(defstate b :start nil)
(defstate c :start nil)

(start #'b)
;=> (#'user/a #'user/b)

(start)
;=> (#'user/c)

(stop #'b)
;=> (#'user/c #'user/b)
```
