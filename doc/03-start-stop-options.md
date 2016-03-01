# Start/stop options

In many cases, calling just `(mount/start)` or `(mount/stop)` is enough.
Still, it is good to know that there are various ways to influence what states are started/stopped and how.
The section about substitutions already showed one of these options.

## Option maps

The `start` and `stop` functions can take one or more option maps.
The combination of these option maps make up a single options map.
This is the data-driven behaviour that makes mount-lite (also) different from mount, as the options are easily composable.
These option maps currently support six keys, applied in the following order:

* `:only` - A collection of the defstate vars that should be started or stopped.

* `:except` - A collection of the defstate vars that should _not_ be started or stopped.

* `:up-to` - A defstate var that should be started (or stopped), including all its dependencies (or dependents). This is unique to mount lite.

* `:substitute` - A map of defstate vars to substitute states, only applicable for `start`. See the substitutes section for more info on this.

* `:parallel` - The number of threads to use for parallel starting or stopping of states.
  Default is nil, meaning the current thread will be used.
  Parallelism is unique to mount-lite and explained in its own section.

* `:bindings` - A map of defstate vars to binding maps. This is a more advanced feature, explained in the section about bindings.

## Option map builders

The API of mount-lite offers the functions `only`, `except`, `up-to`, `substitute`, `parallel` and `bindings`.
These create or update above option maps, as a convenience.
They can be threaded, if that's your style, but you don't need to, as both `start` and `stop` take multiples of these option maps.
For example, these groups of expressions mean the same:

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

While the functions offer a convenient, readable and composable API, remember that all of it is data driven.
Your (test) options can be stored anywhere, such as your `user.clj` file or in an EDN data resource.

## The :up-to option

Although not very difficult to understand, the `:up-to` option can use some explaining.
It is a unique feature to mount, and an important one.
It allows you to easily start or stop only a part of your application until a certain defstate.
You don't have remember any dependent defstates, those are started or stopped automatically for you.

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

The `:up-to` option is actually used internally as well, which you can read more about in the section about reloading defstates.
