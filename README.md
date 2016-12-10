![logo](doc/logo.png)

A library resembling [mount](https://github.com/tolitius/mount), but different on some key things:

* **Clojure only**, dereferencing states only.
* **Minimal API**, based on usage in several larger projects. Less turned out to be more.
* **Supports multiple system instances simultaneously**, enabling parallel testing for instance.

You like it? Feel free to use it. Don't like it? The original mount is great!

## Version 2.0

Version 2.0 is breaking with the 0.9.x versions.
This new version introduced the ability to run multiple systems of states simultaneously.
This was an opportunity to get rid of the excess, based on experience in several larger projects.
It has become really "lite".
[This blog post](http://www.functionalbytes.nl/clojure/mount/mount-lite/2016/12/10/mount-lite-2.html) explains what has changed in version 2.0 in detail, and why.

Version 0.9.x will still be maintained, and can be found under the [1.x branch](https://github.com/aroemers/mount-lite/tree/1.x).

## Getting started

Add `[functionalbytes/mount-lite "2.0.0-SNAPSHOT"]` to your dependencies and make sure Clojars is one of your repositories.

You can find all the documentation about mount-lite, what makes it unique, and the API by clicking on the link below:

[GO TO DOCUMENTATION](http://aroemers.github.io/mount-lite/index.html).

## A primer

Require the `mount.lite` namespace, and other namespaces you depend on.

```clj
(ns your.app
  (:require [mount.lite :refer (defstate) :as mount]
            [your.app.config :as config] ;; <-- Also has a defstate.
            [some.db.lib :as db]))
```

Define a defstate var, including the `:start` and `:stop` lifecycle expressions.

```clj
(defstate db
  :start (db/start (get-in @config/config [:db :url]))
  :stop  (db/stop @db))
;=> #'your.app/db
```

Then start all defstates, use `(start)`.
A sequence of started state vars is returned.
The order in which the states are started is determined by their load order by the Clojure compiler.
Calling `(stop)` stops all the states in reverse order.

```clj
(mount/start)
;=> (#'your.app.config/config #'your.app/db)

@db
;=> object[some.db.Object 0x12345678]

(mount/stop)
;=> (#'your.app/db #'your.app.config/config)

@db
;=> ExceptionInfo: state db is not started
```

*That's it, enjoy!*

## License

Copyright © 2016 Functional Bytes

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.

> Master branch: [![Circle CI](https://circleci.com/gh/aroemers/mount-lite/tree/master.svg?style=svg)](https://circleci.com/gh/aroemers/mount-lite/tree/master)
