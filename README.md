![logo](doc/logo.png)

A library resembling [mount](https://github.com/tolitius/mount), but different on some key things:

* **Clojure only**, dereferncing states only.
* **Minimal API**, based on usage in several larger projects.
* **Supports multiple system instances simultaneously**, enabling parallel testing for instance.

The mount-lite library used to be larger in the 0.9 version.
It was still different from the original mount, with features such as a composable, data-driven API, parameterized states using bindings, and parallel starting and stopping.
Version 0.10 introduced the ability to run multiple state systems simultaneously, which was a good opportunity to get rid of the excess, based on experience in several larger projects.

You like it? Feel free to use it. Don't like it? The original Mount is great!

> NOTE: [This blog post](http://www.functionalbytes.nl/clojure/mount/mount-lite/2016/02/11/mount-lite.html) explains in more detail why mount-lite was created and what it offers.

> NOTE: A future blog post will explain what has changed in version 0.10, and why.

## Documentation

Put this in your dependencies `[functionalbytes/mount-lite "0.10.0-SNAPSHOT"]` and make sure Clojars is one of your repositories.
Also make sure you use Clojure 1.7+, as the library uses transducers and volatiles.

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
;=> ExceptionInfo: state db is not started (in this thread or parent thread.
```

*That's it, enjoy!*

## License

Copyright © 2016 Functional Bytes

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.

> Master branch: [![Circle CI](https://circleci.com/gh/aroemers/mount-lite/tree/master.svg?style=svg)](https://circleci.com/gh/aroemers/mount-lite/tree/master)
