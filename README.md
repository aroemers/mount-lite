# Mount lite

I like [Mount](https://github.com/tolitius/mount), a lot. But

* I wanted a composable and data-driven API (see [this mount issue](https://github.com/tolitius/mount/issues/19)
  and [this presentation](https://www.youtube.com/watch?v=3oQTSP4FngY)).
* I had my own ideas about how to handle redefinition of states.
* I don't need ClojureScript support (and its CLJC mode, I use the [rmap library](https://github.com/aroemers/rmap) for lazily loading systems),
* I don't need suspending (or [other](https://github.com/tolitius/mount/issues/16)
  [features](https://github.com/tolitius/mount/blob/dc5c89b3e9a47601242fbc79846460812f81407d/src/mount/core.cljc#L301)) -
  I'd like a library like this to be minimal.

Mount Lite is **Clojure only**, has a **flexible data-driven** API, **substitutions** are well supported
(and cleaner in my opinion, but Mount may [get there](https://github.com/tolitius/mount/issues/45) as well),
states **stop automatically and cascadingly on redefinition**, and states can be started and stopped **in parallel**. That's it.

You like it? Feel free to use it. Don't like it? The original Mount is great!

## Table of contents

* [Usage](#usage)
  * [Global states, starting and stopping](#global-states-starting-and-stopping)
  * [Reloading, cascading and tools.namespace](#reloading-cascading-and-toolsnamespace)
  * [Substitute states](#substitute-states)
  * [Only, except and other start/stop options](#only-except-and-other-startstop-options)
  * [Parallelism](#parallelism)
  * [Bindings](#bindings)
* [License](#license)

## Usage

Put this in your dependencies `[functionalbytes/mount-lite "0.9.4"]` and make sure Clojars is one of your repositories.
Also make sure you use Clojure 1.7+, as the library uses transducers and volatiles.
Read on for a description of the library functions, or go straight to the [API docs](http://aroemers.github.io/mount-lite/index.html).

> NOTE: Clojure 1.8 - with its direct linking - is safe to use as well.

### Global states, starting and stopping

First, require the `mount.lite` namespace:

```clj
(ns your.app
  (:require [mount.lite :refer (defstate state only except substitute) :as mount]
            [your.app.config :as config] ;; Also has a defstate defined.
            [some.db.lib :as db]))
```

The simplest of a global state definition is one with a name and a `:start` expression. In this example we also supply a
`:stop` expression.

```clj
(defstate db
  :start (db/start (get-in config/config [:db :url]))
  :stop (do (println "Stopping DB...") (db/stop db)))
;=> #'your.app/db
```

> Consider the following in your design when using Mount:
>
> * Only use defstate in your application namespaces, not in library namespaces.
> * Only use defstate when either the state needs some stop logic before it can be reloaded,
>   or whenever the state depends on another defstate. In other cases, just use a def.

To start all global states, just use `start`. A sequence of started state vars is returned. The order in which the
states are started is determined by their load order by the Clojure compiler. Using `stop` stops all the states in
reverse order.

```clj
(mount/start)
;=> (#'your.app.config/config #'your.app/db)

db
;=> object[some.db.Object 0x12345678]
```

Also note that documents strings and attribute maps are supported. So a full `defstate` might look something like this:

```clj
(defstate ^:private db
  "My database state"
  {:attribute 'map}
  :start (db/start (get-in config/config [:db :url]))
  :stop (do (println "Stopping db...") (db/stop db)))
```

*Now you know the basics. Go on, try it! I will see you in 10 minutes.*

### Reloading, cascading and tools.namespace

Whenever you redefine a global state var - when reloading the namespace for instance - by default that state and all the states depending on that state will be stopped automatically (in reverse order). We call this a cascading stop. For example:

```clj
(defstate a :start 1 :stop (println "Stopping a"))
(defstate b :start 2 :stop (println "Stopping b"))
(defstate c :start 3 :stop (println "Stopping c"))

(start)
;=> (#'user/a #'user/b #'user/c)

(defstate b :start 22 :stop (println "Stopping bb"))
;;> Stopping c
;;> Stopping b

(start)
;=> (#'user/b #'user/c)
```

This cascading is great to work with, and in combination with the [tools.namespace](https://github.com/clojure/tools.namespace) library it can really shine. Whenever you make sure your namespaces with `defstate` definitions have `{:clojure.tools.namespace.repl/unload false}` as metadata, calling `(clojure.tools.namespace.repl/refresh :after 'mount.lite/start)` will only stop the required states (in correct order) and restart them.

> NOTE: If you want your namespaces to be unloaded when using `c.t.n.r/refresh`, make sure you call `(stop)` beforehand.

Still, there may be cases where you don't want this cascading stop behaviour. To alter this behaviour, one can set a different mode via the `on-reload` function. Given no arguments, it returns the current mode. Given an argument (a keyword), you can set the reload behaviour to one the following modes:

* `:cascade` - This is the default, as described above.
* `:stop` - This will stop only the state that is being redefined.
* `:lifecycle` -  This will only redefine the lifecycle functions, and keep the state running as is (including the accompanying `:stop` expression). I.e, it is only after a (re)start that the redefinition will be used.

> NOTE: _Up to_ is actually an option for the `start` and `stop` functions, as described [further below](#only-except-and-other-startstop-options).

> NOTE: Extra modes can be added by adding a method to the `do-on-reload` multimethod.

### Substitute states

Whenever you want to mock a global state when testing, you can define anonymous `state` maps, and pass this to the
`start` function using the `substitute` function (or with plain data, as described in the next section).

```clj
(mount/start (substitute #'db (state :start (do (println "Starting fake DB") (atom {}))
                                     :stop (println "Stopping fake DB"))))
;>> Starting fake DB
;=> (#'your.app/db)

db
;=> object[clojure.lang.Atom 0x2010a30b {:status :ready, :val {}}]

(mount/stop)
;>> Stopping fake DB
;=> (#'your.app/db #'your.app.config/config)
```

After a substituted state is stopped, it is brought back to its original definition. Thus, starting the state var again,
without a substitute configured for it, will start the original definition.

Note that substitution states don't need to be inline and the `state` macro is also only for convenience.
For example, the following is also possible:

```clj
(mount/start (substitute #'db {:start (constantly (atom {}))}))
```

### Only, except and other start/stop options

The `start` and `stop` functions can take one or more option maps (as we have done already actually, with the
substitutions above). The combination of these option maps make up a single options map, influencing what global states
should be started or stopped, and, as we have seen already, which states should be substituted (in case of `start`).

These option maps support six keys, and are applied in the following order:

* `:only` - A collection of the state vars that should be started or stopped (if not already having that status).

* `:except` - A collection of the state vars that should not be started or stopped.

* `:up-to` - A defstate var that should be started (or stopped), including all its dependencies (or dependents). This is unique to mount lite.

* `:substitute` - A map of state vars to substitute states, only applicable for `start`.

* `:parallel` - The number of threads to use for parallel starting or stopping of states. Default is nil, meaning the
  current thread will be used. Parallelism is unique to mount lite and explained in the next [section](#parallelism).

* `:bindings` - A map of state vars to binding maps. This is a more advanced feature, explained in the section about [bindings](#bindings).

The functions `only`, `except`, `up-to`, `substitute` and `parallel` create or update such option maps, as a convenience. These functions can
be threaded, if that's your style, but you don't need to, as both `start` and `stop` take multiples of these option
maps. For example, these groups of expressions mean the same:

```clj
(mount/start {:only [#'db]})
(mount/start (only #'db))

(mount/start {:except [#'db] :substitute {#'your.app.config/config my-fake-config}})
(mount/start (-> (except #'db) (substitute #'your.app.config/config my-fake-config)))

(mount/start {:only [#'db]} {:only [#'your.app.config/config]})
(mount/start (only #'db) (only #'your.app.config/config))
(mount/start (-> (only #'db) (only #'your.app.config/config)))
(mount/start (only #'db #'your.app.config/config))
```

While the functions offer a convenient, readable and composable API, all of it is data driven. Your (test) configuration
can be stored anywhere, such as your `user.clj` file or in an EDN data resource.

The following shows how `up-to` works:

```clj
(defstate a :start nil)
(defstate b :start nil)
(defstate c :start nil)

(start (up-to #'b))
;=> (#'user/a #'user/b)

(start)
;=> (#'user/c)

(stop (up-to #'b))
;=> (#'user/c #'user/b)
```

### Parallelism

A unique feature of Mount Lite is being able to start and stop the defstates parallel to each other, wherever applicable.
It does this by calculating a dependency graph (using [tools.namespace](https://github.com/clojure/tools.namespace) of all the states,
and starts (or stops) them as eagerly as possible using a - user specified - number of threads. Note that this same graph is also
used for the `up-to` feature.

States default to depend on other states in the same namespace defined above them, so the parallelism is normally to
be gained on a namespace level. The following example shows how parallelism works:


```clj
;; Helper defs:

(def timer (atom 0))

(defn starter [v]
  (println "Starting" v "at" (- (System/currentTimeMillis) @timer) "...")
  (Thread/sleep 500))

;; Make following state namespaces:
;;
;;  core ----> mid1 -.
;;                    }-> end
;;             mid2 -'
;;
(ns end (:require mount.lite))
(mount.lite/defstate end :start (user/starter "end"))

(ns mid1 (:require end))
(mount.lite/defstate mid1 :start (user/starter "mid1"))

(ns mid2 (:require end))
(mount.lite/defstate mid2 :start (user/starter "mid2"))

(ns core (:require mid1))
(mount.lite/defstate core :start (user/starter "core"))

;; Test the parallelism:

(use mount.lite)
(do (reset! user/timer (System/currentTimeMillis))
    (time (start (parallel 2))))
;>> Starting end at 48 ...
;>> Starting mid1 at 550 ...
;>> Starting mid2 at 584 ...
;>> Starting core at 1055 ...
;>> "Elapsed time: 1564.301291 msecs"
;=> (#'end/end #'mid1/mid1 #'mid2/mid2 #'core/core)
```

In above example one can see that states `mid1` and `mid2` are started almost simultaneously. Also note that when
one would stop above example, the states `core` and `mid2` will be stopped in parallel.

> NOTE: If you really want to get the most out of parallelism, you can declare the dependencies
> on a state by putting `:dependencies` in its metadata. This way states don't necessarily depend on other states
> in the same namespace or referenced namespaces.

### Bindings

It is generally best to define the `defstate`s in application namespaces, not in the more general (library) namespaces. This is because the `:start` and `:stop` expressions are tightly coupled to their environment, including references to other states. This is fine though, as you as the application writer have full control over your states, and resources should be at the periphery of the application anyway.

Yet, in the rare situations where you need a looser coupling between the `:start`/`:stop` expressions and their environment, mount-lite has a unique feature called bindings. When defining a `defstate`, one can optionally supply a `:bindings` vector, next to the `:start` and `:stop` expressions. This vector declares the bindings that can be used by the `:start`/`:stop` expressions, and their defaults. For example:

```clj
(defstate incrementer
  :start    (fn [n] (+ n i))
  :stop     (println "stopping incrementer of" i)
  :bindings [i 10])
```

When the `incrementer` state is started normally, it will become a function that increments the argument by 10. However, one can start the `incrementer` with different bindings, like so:

```clj
(mount/start (bindings #'incrementer {'i 20})
;=> (#'incrementer)

(incrementer 5)
;=> 25

(stop)
;>> stopping 20 incrementer
;=> (#'incrementer)
```

As can be seen, the bindings that were used when starting the state are also used when stopping the state.

This bindings feature can be used as a kind of dependency injection or for passing configuration parameters. Yet, at the current time of writing, my opinion is to use this feature sparingly. Substitutions are normally sufficient and using bindings a lot might hint towards a design flaw.

*Whatever your style or situation, enjoy!*

## License

Copyright © 2016 Functional Bytes

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.

> Master branch: [![Circle CI](https://circleci.com/gh/aroemers/mount-lite/tree/master.svg?style=svg)](https://circleci.com/gh/aroemers/mount-lite/tree/master)
