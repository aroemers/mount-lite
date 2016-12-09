# The basics

## What is mount-lite for?

The mount-lite library was inspired by [mount](https://github.com/tolitius/mount).
If you know that library, you already know what you can do with it, as mount-lite is based on the same premise.

This premise is to have a basic lifecycle around the stateful parts of your application.
Using this lifecycle, you can quickly bring up the application state, and bring it down again, before or after reloading (parts of) your application.
The mount and mount-lite libraries give you an easy and non-intrusive way of adding such lifecycles to the stateful parts of your application.

The mount-lite library has some unique features and approach with respect to mount.
For more info on why mount-lite was created, see [this blog post](http://www.functionalbytes.nl/clojure/mount/mount-lite/2016/02/11/mount-lite.html).

That blog post covers the 0.9.x version, and quite some things have changed with version 2.0.
In short, the API has been simplified and the feature to have multiple systems of states simultaneously has been added.
See [this blog post](http://www.functionalbytes.nl/clojure/mount/mount-lite/2016/12/09/mount-lite-2.html) for more info on what has changed, and why.

This documentation covers the functionality of version 2.x.
Version 0.9.x is still supported though, and its documentation can be found in the source repository.


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
The simplest of such a global state definition, is one with a name and a `:start` expression.
In this example we also supply a `:stop` expression.

```clj
(defstate db
  :start (db/start (get-in @config/config [:db :url]))
  :stop (db/stop @db))
;=> #'your.app/db
```

As you can see, the var `#'your.app/db` has been defined.
It currently is in the `:stopped` status.

The `:start` expression above uses another global state - `config/config` - which it dereferences to get its value.
The `:stop` expression uses its own value in the same way.

## Starting and stopping the states

To start all the global states, just call `(mount/start)`.
A sequence of started global state vars is returned.
The order in which the states are started is determined by their load order by the Clojure compiler.
Using `(mount/stop)` stops all the started defstates in reverse order.

```clj
@db
;=> ExceptionInfo: state is not started

(mount/start)
;=> (#'your.app.config/config #'your.app/db)

@db
;=> object[some.db.Object 0x12345678]

(mount/stop)
;=> (#'your.app/db #'your.app.config/config)
```

> NOTE: To get an overview of the status of the defstates, you can use the `status` function.

*Now you know the basics. Go on, try it! I will see you in 10 minutes.*

## Meta data on defstate

The `defstate` macro supports docstrings and attribute maps, as well as meta data on the name of the `defstate`, just like `defn`.
So, a full `defstate` might look something like this:

```clj
(defstate ^:private db
  "My database state"
  {:attribute 'map}
  :start ...
  :stop ...)
```

## Design considerations

Consider the following in your design when using mount-lite (or mount for that matter):

* Only use `defstate` in your application namespaces, not in library namespaces, and preferable in the outskirts of your application.
  Having a global state does not mean you should forego on the good practice of passing state along as arguments to functions.

* Only use `defstate` when either the stateful object needs some stop logic before the application can be reloaded/restarted, or whenever the state depends on another `defstate`.
  In other cases, just use a def.

## Further reading

Now that you know the basics, it is advised to learn about at least one other feature of mount-lite: [substituting](02-substitutions.html). You could also read on how to [start up to or stop down](03-start-stop-options.html) to a certain state, or how to start [multiple systems of states](04-systems-of-states.html) simultaneously.
