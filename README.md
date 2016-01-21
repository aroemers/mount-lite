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

Put this in your dependencies `[functionalbytes/mount-lite "1.0"]` and make sure Clojars is one of your repositories.
For now, read the examples below. Note that there are many ways to use and compose the API, so those different ways are used 
throughout these examples.

### Global states, starting and stopping

```clj
(ns your.app
  (:require [mount.lite :refer (defstate) :as mount]
            [your.app.config :as config]
            [your.app.db :as db]))

(defstate db 
  :start (db/start config/get [:db :url])
  :stop (do (println "Stopping db...") (db/stop db)))
;=> #your.app/db
  
(mount/start)
;=> (#your.app.config/config #your.app/db)

(defstate ^:private db
  "My database state"
  {:extra 'meta}
  :start (db/start config/get [:db :url])
  :stop (do (println "Stopping db...") (db/stop db)))
;>> Stopping db...
;=> #your.app/db

db
;=> object[mount.lite.Unstarted 0x1234566 State #your.app/db is unstarted]
```

### Substitute states

```clj
(mount/start (mount/substitute #db (state :start (do (println "Starting fake DB") (atom {}))
                                          :stop (println "Stopping fake DB"))))
;>> Starting fake DB
;=> (#your.app/db)

db
;=> object[clojure.lang.Atom 0x2010a30b {:status :ready, :val {}}]

(mount/stop)
;>> Stopping fake DB
;=> (#your.app/db #your.app.config/config)

(def fake-db '{:start (atom {})})

(mount/start {:substitute {#db (state fake-db)
              :only #{#db}})
;=> (#your.app/db)
```

### Start and stop options

```clj
(def fake-config (state :start {:url "localhost"}))

(mount/start (mount/except #your.app/db) 
             (mount/substitute #your.app.config/config fake-config))
              
(mount/stop (only #your.app.config/config #your.app/db))

(mount/start {:except [#your.app.config/config]})
```

## License

Copyright © 2016 Functional Bytes

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
