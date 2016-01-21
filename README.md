# Mount lite

I like the idea of [Mount](https://github.com/tolitius/mount), a lot. But

* I don't need ClojureScript (and its CLJC or CLJS modes),
* I liked the idea of using meta data directly in the state vars better (which is how Mount used to work),
* I don't need suspending, and
* I wanted a nicer API.
 
This is Mount Lite. Clojure only, no suspending, flexible and composable API, substitutions are possible (and cleaner 
in my opinion) and states stop automatically whenever they are redefined (just like Mount, can be turned off). That's it.

You like it? Feel free to use it. Don't like it? The original Mount is great as well!

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
`:stop` expression.

```clj
(defstate db 
  :start (db/start (get-in config/config [:db :url]))
  :stop (do (println "Stopping DB...") (db/stop db)))
;=> #your.app/db
```

To start all global states, just use `start`. A sequence of started state vars is returned.

```clj
(mount/start)
;=> (#your.app.config/config #your.app/db)

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
;=> #your.app/db

db
;=> object[mount.lite.Unstarted 0x12345678 State #your.app/db is unstarted]
```

When in a rare case you don't want the current state stopping automatically when it is redefined, use the 
`:stop-on-reload?` key and set it to false.

### Substitute states

Whenever you want to mock a global state when testing, you can define anonymous `state` maps, and pass this to the 
`start` function using the `substitute` function (or with plain data, as described in the next section).

```clj
(mount/start (substitute #db (state :start (do (println "Starting fake DB") (atom {}))
                                    :stop (println "Stopping fake DB"))))
;>> Starting fake DB
;=> (#your.app/db)

db
;=> object[clojure.lang.Atom 0x2010a30b {:status :ready, :val {}}]

(mount/stop)
;>> Stopping fake DB
;=> (#your.app/db #your.app.config/config)
```

After a substituted state is stopped, it is brought back to its original definition. Thus, starting the state var again,
without a substitute configured for it, will start the original definition.

Note that substitution states don't need to be inline. For example, the following is also possible:

```clj
(def fake-db-state (state :start (atom {})))

(mount/start (substitute #db fake-db-state))
```

or, just a map

```clj
(def fake-db-map '{:start (atom {})})

(mount/start (substitute #db (state fake-db-map)))
```

### Only, except and start/stop options

The `start` and `stop` functions can take one or more option maps (as we have done already actually, with the 
substitutions above). The combination of these option maps make up a single options map, influencing what global states 
should be started or stopped, and, as we have seen already, which states should be substituted (in case of `start`).

These option maps support three keys:

* `:only` - A collection of the state vars that should be started or stopped (if not already having that status).

* `:except` - A collection of the state vars that should not be started or stopped.

* `:substitute` - A map of state vars to substitute states, only applicable for `start`.

The functions `only`, `except` and `substitute` create or update such option maps, as a convenience. These functions can
be threaded, if that's your style, but you don't need to, as both `start` and `stop` take multiples of these option 
maps. For example:

```clj
(mount/start {:only [#db]})
(mount/start (only #db))

(mount/start {:except [#db] :substitute {#your.app.config/config my-fake-config}})
(mount/start (-> (except #db) (substitute #your.app.config/config my-fake-config)))

(mount/start {:only [#db]} {:only [#your.app.config/config]})
(mount/start (only #db) (only #your.app.config/config))
(mount/start (-> (only #db) (only #your.app.config/config)))
(mount/start (only #db #your.app.config/config))
```

Whatever your style or situation, enjoy!

## License

Copyright © 2016 Functional Bytes

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
