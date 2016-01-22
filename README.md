# Mount lite

I like [Mount](https://github.com/tolitius/mount), a lot. But

* I don't need ClojureScript support (and its CLJC or CLJS modes),
* I like using meta data directly in the state vars better (which is how Mount used to work before ClojureScript support),
* I don't need suspending (or [other](https://github.com/tolitius/mount/issues/16) 
  [features](https://github.com/tolitius/mount/blob/dc5c89b3e9a47601242fbc79846460812f81407d/src/mount/core.cljc#L301)) - 
  I'd like a library like this to be minimal, and
* I wanted a more composable and data-driven API (see [this mount issue](https://github.com/tolitius/mount/issues/19) 
  and [this presentation](https://www.youtube.com/watch?v=3oQTSP4FngY)).
 
Mount Lite is Clojure only, offers no suspending, has a flexible API, substitutions are supported 
(and somewhat cleaner in my opinion, but Mount may [get there](https://github.com/tolitius/mount/issues/45) as well) and states stop 
automatically whenever they are redefined (just like Mount, but with Mount Lite this can be disabled per state, 
something Mount may [have](https://github.com/tolitius/mount/issues/36) in the future as well). That's it.

You like it? Feel free to use it. Don't like it? The original Mount is great!

## Usage

Put this in your dependencies `[functionalbytes/mount-lite "0.9"]` and make sure Clojars is one of your repositories.

### Global states, starting and stopping

First, require the `mount.lite` namespace:

```clj
(ns your.app
  (:require [mount.lite :refer (defstate state only except substitute) :as mount]
            [your.app.config :as config] ;; Also has a defstate defined.
            [some.db.lib :as db]))
```

The simplest of a global state definition is one with a name and a `:start` expression. In this example we also supply a
`:stop` expression. These states should be part of your application, not libraries.

```clj
(defstate db 
  :start (db/start (get-in config/config [:db :url]))
  :stop (do (println "Stopping DB...") (db/stop db)))
;=> #'your.app/db
```

To start all global states, just use `start`. A sequence of started state vars is returned. The order in which the 
states are started is determined by their load order by the Clojure compiler. Using `stop` stops all the states in
reverse order.

```clj
(mount/start)
;=> (#'your.app.config/config #'your.app/db)

db
;=> object[some.db.Object 0x12345678]
```

Whenever you redefine a global state var, by default the current state is stopped automatically. The following example
also shows that one can use metadata, document strings and attribute maps.

```clj
(defstate ^:private db
  "My database state"
  {:attribute 'map}
  :start (db/start (get-in config/config [:db :url]))
  :stop (do (println "Stopping db...") (db/stop db)))
;>> Stopping DB...
;=> #'your.app/db

db
;=> object[mount.lite.Unstarted 0x12345678 "State #'your.app/db is not started."]
```

When in a rare case you don't want the current state stopping automatically when it is redefined, use the 
`:stop-on-reload?` key and set it to false.

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
(def fake-db-state (state :start (atom {})))

(mount/start (substitute #'db fake-db-state))
```

or, just a map

```clj
(def fake-db-quoted-map '{:start (atom {})})

(mount/start (substitute #'db (state fake-db-quoted-map)))

(def fake-db-state-map {:start (constantly (atom {}))})

(mount/start (substitute #'db fake-db-state-map)
```

### Only, except and start/stop options

The `start` and `stop` functions can take one or more option maps (as we have done already actually, with the 
substitutions above). The combination of these option maps make up a single options map, influencing what global states 
should be started or stopped, and, as we have seen already, which states should be substituted (in case of `start`).

These option maps support four keys, and are applied in the following order:

* `:only` - A collection of the state vars that should be started or stopped (if not already having that status).

* `:except` - A collection of the state vars that should not be started or stopped.

* `:up-to` - A defstate var until which the states are started or stopped. In case multiple option maps are supplied, 
  only the last :up-to option is used.

* `:substitute` - A map of state vars to substitute states, only applicable for `start`.

The functions `only`, `except`, `up-to` and `substitute` create or update such option maps, as a convenience. These functions can
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

And this shows how `up-to` works:
 
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

While the functions offer a convenient, readable and composable API, all of it is data driven. Your (test) configuration
can be stored anywhere, such as your `user.clj` file or in an EDN data resource.

Whatever your style or situation, enjoy!

## License

Copyright © 2016 Functional Bytes

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
