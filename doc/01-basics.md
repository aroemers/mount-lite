# The basics

## What is mount-lite for?

The mount-lite library was inspired by the [mount](https://github.com/tolitius/mount) library.
If you know that one, you already know what you can do with mount-lite, as mount-lite is based on the same premise.

The premise is to have a basic lifecycle around the stateful parts of your application.
The mount-lite library gives you an easy and non-intrusive way of adding such lifecycles.
Using this lifecycle, you can quickly bring up the application state, and bring it down again.
You do this in your application's entry point and shutdown sequence, but you will mostly use it within a REPL.
Inside the REPL you can quickly reset your application to use new and updated code.

The mount-lite library has some unique features and approach with respect to mount.
For more info on why mount-lite was created, see [this blog post](https://www.functionalbytes.nl/clojure/mount/mount-lite/2016/02/11/mount-lite.html).
Note that the blog post covers version 0.9 of mount-lite, and quite some things have changed with [version 2.0](https://functionalbytes.nl/clojure/mount/mount-lite/2016/12/10/mount-lite-2.html) and [version 3.0]().
This documentation covers version 3.0.

## Defining states

First, you need to require the `mount.lite` namespace.
Also require the namespaces which hold the states that the states in the current namespace depend upon.
This is how mount-lite figures out what states should be started before the states in the current namespace are started.

```clj
(ns my.app
  (:require [my.app.config :as config] ;; also has a defstate defined.
            [mount.lite :refer [defstate start stop]]
            ...))
```

Defining a global state is done with the `defstate` macro.
The simplest of such a global state definition, is one with a name and a `:start` expression (although that is optional as well).
In this example there is also a `:stop` expression.

```clj
(defstate db
  :start (db/open (get-in @config/config [:db :url]))
  :stop  (db/close @db))
;=> #'my.app/db
```

As you can see, the var #'my.app/db has been defined.

The `:start` expression above uses another global state - `my.app.config/config` - which it dereferences to get its value.
The `:stop` expression uses its own value in the same way.
This dereferencing is the way you get the value of a started state, anywhere in your application.

## Starting and stopping the states

To start all the global states, just call `(start)`.
A sequence of started global states is returned.
As stated earlier, the order in which the states are started is determined by their load order by the Clojure compiler.
Using `(stop)` stops all the started states in reverse order.

```clj
(start)
;=> (my.app.config/config my.app/db)

@db
;=> object[some.db.Object 0x12345678]

(stop)
;=> (my.app/db my.app.config/config)

@db
;=> ExceptionInfo: Cannot deref state my.app/db when not started (system :default)
```

To get an overview of the status of the defstates, you can call the `(status)` function.

Now you know the basics.
Go on, try it!
I will see you in 10 minutes.

## Metadata on defstate

The `defstate` macro supports docstrings and attribute maps, as well as meta data on the name of the defstate, just like `defn` does.
So a full `defstate` definition might look something like this:

```clj
(defstate ^:private db
  "My database state"
  {:attribute 'map}
  :start ...
  :stop ...)
```

## Design considerations

Consider the following in your design when using mount-lite:

1. Only use `defstate` in your application namespaces, not in library namespaces, and preferable in the outskirts of your application.
Having a global state does not mean you should forego on the good practice of passing state along as arguments to functions.
2. Only use `defstate` when either the stateful object needs some stop logic before the application can be reloaded/restarted, or whenever the state depends on another defstate.
In other cases, just use a def.
3. Try to use your `defstate` as if it were private.
Better yet, declare it as private.
This will keep you from refering to your state from every corner of your application.

## Further reading

Now that you know the basics, there is more to learn.
It is advised to learn about at least the [testing features]() and [partially starting/stopping]() the system.
You could also read up on how to start [multiple systems]() in parallel or mount-lite's [extensions]().
