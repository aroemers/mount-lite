# The basics

## What is mount-lite for?

The mount-lite library was inspired by [mount](https://github.com/tolitius/mount).
If you know that library, you already know what you can do with it, as mount-lite is based on the same premise.

This premise is to have a basic lifecycle around the stateful parts of your application.
Using this lifecycle, you can quickly bring up the application state, and bring it down again, before reloading (parts of) your application.
The mount and mount-lite libraries give you an easy and non-intrusive way of adding such lifecycles to the stateful parts of your application.

The mount-lite library has some unique features and approach with respect to mount.
For more info on why mount-lite was created, see [this blog post](http://www.functionalbytes.nl/clojure/mount/mount-lite/2016/02/11/mount-lite.html).

## Defining states

First, require the `mount.lite` namespace.
Also require the namespaces which hold the states that the states in the current namespace depend upon.

```clj
(ns your.app
  (:require [mount.lite :refer (defstate) :as mount]
            [your.app.config :as config] ;; <-- Also has a defstate defined.
            [some.db.lib :as db]))
```

Defining a state is done with the  `defstate` macro.
It defines a var, and looks a lot like a `defn`.
There are variour options to the `defstate` macro, but the most important ones are the lifecycle functions.
The simplest of a global state definition is one with a name and a `:start` expression.
In this example we also supply a `:stop` expression.

```clj
(defstate db
  :start (db/start (get-in config/config [:db :url]))
  :stop (db/stop db))
;=> #'your.app/db
```

As you can see, the var `#'your.app/db` has been defined.
It has been initialised with an `mount.lite.Unstarted` type.

## Starting and stopping the states

To start all `defstate`s, just call `(mount/start)`.
A sequence of started state vars is returned.
The order in which the states are started is determined by their load order by the Clojure compiler.
Using `(mount/stop)` stops all the started defstates in reverse order.

```clj
db
;=> #[mount.lite.Unstarted "State #'your.app/db is unstarted"]

(mount/start)
;=> (#'your.app.config/config #'your.app/db)

db
;=> object[some.db.Object 0x12345678]

(mount/stop)
;=> (#'your.app/db #'your.app.config/config)
```

> NOTE: The order in which the defstates are started or stopped when [parallelism](#parallelism) is used, may be different than their load order.

> NOTE: To get an overview of the status of the defstates, you can use the `status` function.

*Now you know the basics. Go on, try it! I will see you in 10 minutes.*

## Meta data on defstate

The `defstate` macro supports document strings and attribute maps, as well as meta data on the name of the `defstate`, just like `defn`.
So, a full `defstate` might look something like this (with the options we have discussed so far):

```clj
(defstate ^:private db
  "My database state"
  {:attribute 'map}
  :start (db/start (get-in config/config [:db :url]))
  :stop (do (println "Stopping db...") (db/stop db)))
```

## Design considerations

Consider the following in your design when using mount-lite (or mount for that matter):

* Only use `defstate` in your application namespaces, not in library namespaces.

* Only use `defstate` when either the stateful object needs some stop logic before it can be reloaded, or whenever the state depends on another `defstate`.
  In other cases, just use a def.

## Further reading

Now that you know the basics, it is advised to learn about the more advanced features of mount-lite. This includes reloading behaviour (and how to influence it), how to declare substitutes (e.g. mocks for testing) for `defstate` vars, how to start or stop only parts of your system, and about parallelism.
