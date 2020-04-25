[![Clojars Project](https://img.shields.io/clojars/v/functionalbytes/mount-lite.svg)](https://clojars.org/functionalbytes/mount-lite)
[![Clojure CI](https://github.com/aroemers/mount-lite/workflows/Clojure%20CI/badge.svg?branch=3.x)](https://github.com/aroemers/mount-lite/actions?query=workflow%3A%22Clojure+CI%22)
[![cljdoc badge](https://cljdoc.org/badge/functionalbytes/mount-lite)](https://cljdoc.org/d/functionalbytes/mount-lite/CURRENT)

![logo](doc/logo.png)

A library resembling [mount](https://github.com/tolitius/mount), but with differences on some key points:

* **Clojure only**, dereferencing states only.
* **Minimal API**, based on usage in several larger projects. Less turned out to be more.
* **Supports multiple systems simultaneously**, enabling parallel testing for example.
* **Supports plain system maps**, no need for start/stop/substitute fuss in your unit tests.
* **Supports extensions**, some powerful ones are provided, such as data-driven system configs and tools.namespace integration.
* **Small, simple and composable implementation**, less magic and less code is less bugs.


## Getting started

Add [![Clojars Project](https://img.shields.io/clojars/v/functionalbytes/mount-lite.svg)](https://clojars.org/functionalbytes/mount-lite) to your dependencies.

You can find all the documentation about mount-lite, its unique features, and how to use the API by clicking on the link below:

[![cljdoc badge](https://cljdoc.org/badge/functionalbytes/mount-lite)](https://cljdoc.org/d/functionalbytes/mount-lite/CURRENT)


## A primer

The basic behaviour is similar to the original mount library.
Instead of requiring the `mount.core` namespace, simply require `mount.lite`, and the other namespaces your state depends on.

```clj
(ns your.app
  (:require [mount.lite :as mount :refer [defstate]]
            [your.app.config :as config] ;; <-- also has a defstate.
            [some.db.lib :as dblib]))
```

Define a defstate var, including a `:start` and optionally a `:stop` lifecycle expression.
For example:

```clj
(defstate db
  :start (dblib/start (get-in @config/config [:db :url]))
  :stop  (dblib/stop @db))
;=> #'your.app/db
```

Now you can start your system by calling `(start)`.
A sequence of started defstates is returned.
The order in which the states are started is determined by their load order in the Clojure compiler.
Calling `(stop)` stops all the states in reverse order.

```clj
(mount/start)
;=> (your.app.config/config
     your.app/db)

@db
;=> object[some.db.Object 0x12345678]

(mount/stop)
;=> (your.app/db
     your.app.config/config)

@db
;=> ExceptionInfo: Cannot deref state your.app/db when not started (system :default)
```

*These are the basics you need. Enjoy!*


## Starting and stopping part of system

A unique feature of mount-lite is that you can start your system up to a certain defstate.
It will start all the dependencies for that defstate and then the specified defstate itself.
The same goes for stopping the system up to a certain defstate, stopping the dependents for that defstate and then the defstate itself.
You do this by supplying that particular defstate to the `start` or `stop` function.
For example, let's imagine three defstates: `state-a`, `state-b` and `state-c`, the latter depending on the former:

```clj
(start state-b)
;=> (user/state-a user/state-b)

(start)
;=> (user/state-c)

(stop state-b)
;=> (user/state-c user/state-b)

(stop)
;=> (user/state-a)
```

This feature is mainly used while developing in your REPL and for testing.


## Testing

Below you'll find a basic explanation of the testing features in mount-lite.
For a more thorough description, go to the documentation link shown earlier.

### The system map

The mount-lite library always included the "substitutes" feature of the original mount library, albeit implementated in slightly different way.
Mount-lite 3.0 still supports substituting, as described further down below.
However version 3.0 added a new way for setting up the system in unit tests: the `with-system-map` macro.
It allows you to explicitly specify what values the defstates must have within the scope of this macro.
For example:

```clj
(deftest with-system-map-test
  (with-system-map {your.app/db mock-db}
    (is (= 1 (count-records-in-db)))))
```

In the example above, whenever the `your.app/db` defstate is consulted within the scope of the `with-system-map` expression, it returns the mock database.

### Substituting

Substituting is still supported, and also still desired for unit or integration tests on a more "live" system.
The example below shows how the former example would be written with substitutes.
It also shows that it adds more fluff to the actual test, and how you could start your system "up-to" the `db` defstate.

```clj
(deftest with-substitutes-test
  (with-substitutes {your.app.config/config (state :start (read-config "config.test.edn"))
                     your.app/db            (state :start (derby/embedded-db)
                                                   :stop  (derby/stop-db @your.app/db))}
    (try
      (start your.app/db)
      (is (= 1 (count-records-in-db)))
      (finally
        (stop)))))
```

Note that these two concepts can be used together in flexible ways.
For example, you can supply a partial system map, and start the rest of the - possibly and/or partially substituted - defstates.
The states already in the system map will not be started in that case.


## Multiple systems

Another unique feature of mount-lite is that supports running multiple systems in parallel.
This feature was added in mount-lite 2.0, and it has been improved upon in version 3.0.
Where the former version was based on spinning up a "session" thread, now you simply specify a system by its key, whatever thread you're in.
If not specified, the key is `:default`.

To specify a key, you use the `with-system-key` macro.
All mount-lite actions within the scope of that expression - such as starting, stopping, or consulting a defstate's value - will use the system identified by that key.
Note that this can be combined with the testing features discussed earlier as well.
For example:

```clj
(start)
;=> (your.app.config/config your.app/db)

(with-system-key :empty-db
  (with-substitutes {your.app/db your.app.integration-test/empty-db}
    (start)))
;=> (your.app.config/config your.app/db)

(count-records-in-db)
;=> 4321

(with-system-key :empty-db
  (count-records-in-db))
;=> 0
```

In the above example, two systems are running, where the only difference is the `your.app/db` defstate's value.


## License

Copyright © 2017-2020 Functional Bytes

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
