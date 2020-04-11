![logo](doc/logo.png)

A library resembling [mount](https://github.com/tolitius/mount), but different on some key things:

* **Clojure only**, dereferencing states only.
* **Minimal API**, based on usage in several larger projects. Less turned out to be more.
* **Supports multiple system instances simultaneously**, enabling parallel testing for instance.
* **Easily extensible**, without touching the core. Several extensions are provided, such as [tools.namespace](https://github.com/clojure/tools.namespace#reloading-code-usage) integration and inferred dependency graphs.

## Getting started

Add `[functionalbytes/mount-lite "2.1.3"]` or `functionalbytes/mount-lite {:mvn/version "2.1.3"}` to your dependencies and make sure Clojars is one of your repositories.

You can find all the documentation about mount-lite, what makes it unique, and the API by clicking on the link below:

&gt;>> [**FULL DOCUMENTATION**](https://cljdoc.org/d/functionalbytes/mount-lite/) <<<

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

## Core development features

Next to above basic usage, the core provides three more concepts:

- **[Substitutes](https://cljdoc.org/d/functionalbytes/mount-lite/CURRENT/doc/substitutions)**: replace default defstate implementations with substitutes, for testing or REPL sessions.

- **[Start/stop "up-to"](https://cljdoc.org/d/functionalbytes/mount-lite/CURRENT/doc/start-up-to-stop-down-to)**: only start (or stop) the defstates sequence to a certain defstate.

- **[Multiple systems](https://cljdoc.org/d/functionalbytes/mount-lite/CURRENT/doc/multiple-systems-of-states)**: start multiple instances of defstate systems concurrently.

## Extensions

The core provides an easy [extension point](https://cljdoc.org/d/functionalbytes/mount-lite/CURRENT/doc/extension-point).
The following extensions are currently provided:

- **[Basic](https://cljdoc.org/d/functionalbytes/mount-lite/CURRENT/api/mount.extensions.basic)**: start only certain states, or all states except certain states.

- **[Data-driven](https://cljdoc.org/d/functionalbytes/mount-lite/CURRENT/api/mount.extensions.data-driven)**: start the states according to an EDN specification.

- **[Refresh](https://cljdoc.org/d/functionalbytes/mount-lite/CURRENT/api/mount.extensions.refresh)**: use mount-lite in combination with the "refresh" functionality of the [tools.namespace](https://github.com/clojure/tools.namespace#reloading-code-usage) library.

- **[Inferred dependencies](https://cljdoc.org/d/functionalbytes/mount-lite/CURRENT/api/mount.extensions.namespace-deps)**: only start (or stop) states based on an inferred dependency graph using the tools.namespace library.

- **[Explicit dependencies](https://cljdoc.org/d/functionalbytes/mount-lite/CURRENT/api/mount.extensions.explicit-deps)**: only start (or stop) states based on an explicitly declared dependency graph.

## Version 2.x

Version 2.x is breaking with the 0.9.x versions.
Version 2.0 introduced the ability to run multiple systems of states simultaneously.
This was an opportunity to get rid of the excess, based on experience in several larger projects.
It has become really "lite", especially the core, while providing an easy to understand extension point for more advanced features.
[This blog post](https://www.functionalbytes.nl/clojure/mount/mount-lite/2016/12/10/mount-lite-2.html) explains what has changed in version 2.0 in detail, and why.

Version 0.9.x will still be maintained, and can be found under the [1.x branch](https://github.com/aroemers/mount-lite/tree/1.x).

## License

Copyright © 2017-2020 Functional Bytes

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.

> 2.x branch: [![Circle CI](https://circleci.com/gh/aroemers/mount-lite/tree/2.x.svg?style=svg)](https://circleci.com/gh/aroemers/mount-lite/tree/2.x)
