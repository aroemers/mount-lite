[![Clojars Project](https://img.shields.io/clojars/v/functionalbytes/mount-lite.svg)](https://clojars.org/functionalbytes/mount-lite)
[![Clojure CI](https://github.com/aroemers/mount-lite/workflows/Clojure%20CI/badge.svg?branch=3.x)](https://github.com/aroemers/mount-lite/actions?query=workflow%3A%22Clojure+CI%22)
[![cljdoc badge](https://cljdoc.org/badge/functionalbytes/mount-lite)](https://cljdoc.org/d/functionalbytes/mount-lite/CURRENT)
[![Clojars Project](https://img.shields.io/clojars/dt/functionalbytes/mount-lite?color=blue)](https://clojars.org/functionalbytes/mount-lite)
![Weight](https://img.shields.io/badge/weight-lite-brightgreen)

![logo](doc/logo.png)

A library resembling [mount](https://github.com/tolitius/mount), but with differences on some key points:

* **Clojure only**, dereferencing states only.
* **Minimal API**, based on usage in several larger projects. Less turned out to be more.
* **Supports parallel systems**, enabling parallel testing for example.
* **Supports plain system maps**, no need for start/stop/substitute fuss in basic unit tests.
* **Supports extensions**, some powerful ones are provided, such as data-driven system configs and tools.namespace integration.
* **Small, simple and composable implementation**, less magic and less code is less bugs.


## Getting started

Add [![Clojars Project](https://img.shields.io/clojars/v/functionalbytes/mount-lite.svg)](https://clojars.org/functionalbytes/mount-lite) to your dependencies.
Read the primer below for a quick start.
Next, read the extensive documentation to make the most of mount-lite at [![cljdoc badge](https://cljdoc.org/badge/functionalbytes/mount-lite)](https://cljdoc.org/d/functionalbytes/mount-lite/CURRENT).


## A primer

The basic behaviour is similar to the original mount library.
Instead of requiring the `mount.core` namespace, simply require `mount.lite`, and the other namespaces your state depends on.

```clj
(ns my.app
  (:require [mount.lite :as mount :refer [defstate]]
            [my.app.config :as config] ;; <-- also has a defstate.
            [some.db.lib :as dblib]))
```

Define a defstate var, including a `:start` and optionally a `:stop` lifecycle expression.
For example:

```clj
(defstate db
  :start (dblib/connect (get-in @config/config [:db :url]))
  :stop  (dblib/disconnect @db))
;=> #'my.app/db
```

Now you can start your system by calling `(start)`.
A sequence of started defstates is returned.
The order in which the states are started is determined by their load order in the Clojure compiler.
Calling `(stop)` stops all the states in reverse order.

```clj
(mount/start)
;=> (my.app.config/config
     my.app/db)

@db
;=> object[some.db.Object 0x12345678]

(mount/stop)
;=> (my.app/db
     my.app.config/config)

@db
;=> ExceptionInfo: Cannot deref state my.app/db when not started (system :default)
```

*These are the basics you need. Enjoy!* 🚀


## Other features

There is more to mount-lite than just the primer above.
Especially the next sections about partial systems and testing facilities are important.

- [Starting and stopping part of system](#starting-and-stopping-part-of-system)
- [Testing](#testing)
- [Multiple parallel systems](#multiple-parallel-systems)
- [Extensions](#extensions)
- [Metadata on defstate](#metadata-on-defstate)

### Starting and stopping part of system

A unique feature of mount-lite is that you can start or stop your system "up to" a certain defstate.
It will start all the dependencies for that state and then the specified state itself.
You do this by supplying that particular defstate to the `start` or `stop` function.
For example:

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

This feature is mainly used while developing in your REPL and for testing.


### Testing

Below you'll find a basic explanation of the testing features in mount-lite.
For a more thorough description, go to the documentation link shown earlier.

#### The system map

Mount-lite always included the "substitutes" feature of the original mount, be it implementated in slightly different way.
Version 3.0 still supports substituting as described further down below.
However it also added a new way for setting up a system in unit tests: the `with-system-map` macro.
It allows you to explicitly specify what values the defstates must have within the scope of this macro.
For example:

```clj
(deftest with-system-map-test
  (with-system-map {my.app/db mock-db}
    (is (= 1 (count-records-in-db)))))
```

In the example above, whenever the `your.app/db` defstate is consulted within the scope of the `with-system-map` expression, it returns the mock database.

#### Substituting

Substituting is the other testing facility, used for unit or integration tests on a more "live" system.
The example below shows how the former example would be written with substitutes.
It also shows that it adds more fluff to the actual test, and how you could start your system "up-to" the `my.app/db` defstate.

```clj
(deftest with-substitutes-test
  (with-substitutes {my.app.config/config (state :start (read-config "config.test.edn"))
                     my.app/db            (state :start (derby/embedded-db)
                                                 :stop  (derby/stop-db @my.app/db))}
    (try
      (start my.app/db)
      (is (= 1 (count-records-in-db)))
      (finally
        (stop)))))
```

Note that the two testing concepts can be used together in flexible ways.
For example, you can supply a partial system map, and start the rest of the - possibly substituted - states.
The states already in the system map will not be started in that case, as those are regarded already being started.

### Multiple parallel systems

Another unique feature of mount-lite is that supports running multiple systems in parallel.
This feature was added in mount-lite 2.0, and it has been improved upon in version 3.0.
Where the former version was based on spinning up a "session" thread, now you simply specify a system by its key, whatever thread you're in.
If not specified, the key is `:default`.

To specify a system key you use the `with-system-key` macro.
All mount-lite actions within the scope of that expression - such as starting, stopping, or consulting a defstate's value - will use the system identified by that key.
Note that this can be combined with the testing features discussed earlier as well.
For example:

```clj
(start)
;=> (my.app.config/config my.app/db)

(with-system-key :empty-db
  (with-substitutes {my.app/db my.app.integration-test/empty-db}
    (start)))
;=> (my.app.config/config my.app/db)

(count-records-in-db)
;=> 4321

(with-system-key :empty-db
  (count-records-in-db))
;=> 0
```

In the above example, two systems are running, where the only difference is the `my.app/db` defstate's value.

### Extensions

Since mount-lite 2.0 an extension point available to influence which states are started or stopped when calling `(start)` or `(stop)`.
Mount-lite 3.0 improves upon this by making it simpler.

Extensions can be supplied using the `mount.extensions/with-predicate` macro.
The predicate you supply receives each defstate that is about to be started or stopped.
All active predicates must agree to allow to start or stop it.

The following example is an anonymous extension that only starts states within the user namespace:

```clj
(with-predicate #(= "user" (namespace %))
  (start))
```

Several powerful extensions are provided out of the box:

1. [basic]() - explicitly influence which states are started or stopped.
2. [data-driven]() - specify how a system is started based on a pure data map.
3. [namespace-deps]() - uses a namespace dependency tree to determine which states should be started or stopped, instead of the default linear behaviour.
4. [refresh]() - wraps the [tools.namespace]() "refresh" functionality, by stopping updated states, refreshing and starting the stopped states again.


### Metadata on defstate

Lastly, it may be good to know that the `defstate` macro supports metadata on the name, a docstring and an attribute map.
For example, a full `defstate` could be defined as follows:

```clj
(defstate ^:private my-state
  "This is an example docstring."
  {:since "0.9"}
  :start ...
  :stop  ...)
```

## License

Copyright © 2017-2020 Functional Bytes

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
