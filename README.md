# Mount lite

I like [Mount](https://github.com/tolitius/mount), a lot. But

* I wanted a composable and data-driven API (see [this](https://github.com/tolitius/mount/issues/19)
  and [this](https://github.com/tolitius/mount/issues/47) mount issue, and [this presentation](https://www.youtube.com/watch?v=3oQTSP4FngY)).
* I had my own ideas about how to handle redefinition of states.
* I don't need ClojureScript support (or its CLJC mode).
* I don't need suspending (or [other](https://github.com/tolitius/mount/issues/16)
  [features](https://github.com/tolitius/mount/blob/dc5c89b3e9a47601242fbc79846460812f81407d/src/mount/core.cljc#L301)) -
  but I did have some own feature ideas for a library like this.

Mount Lite is **Clojure only**, has a **flexible data-driven** API, **substitutions** are well supported
(and cleaner in my opinion), states **stop automatically and cascadingly on redefinition**, states can define **bindings**
for looser coupling and states can be started and stopped **in parallel**. That's it.

You like it? Feel free to use it. Don't like it? The original Mount is great!

> NOTE: [This blog post](http://www.functionalbytes.nl/clojure/mount/mount-lite/2016/02/11/mount-lite.html) explains in more detail why mount-lite was created and what it offers.

## Documentation

Put this in your dependencies `[functionalbytes/mount-lite "0.9.7"]` and make sure Clojars is one of your repositories.
Also make sure you use Clojure 1.7+, as the library uses transducers and volatiles.

> NOTE: Clojure 1.8 - with its direct linking - is safe to use as well.

You can find all the documentation about mount-lite, what makes it unique, and the API by clicking on the link below:

[GO TO DOCUMENTATION](http://aroemers.github.io/mount-lite/index.html).

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
  :start (db/start (get-in config/config [:db :url]))
  :stop  (db/stop db))
;=> #'your.app/db
```

Then start all defstates, use `(start)`.
A sequence of started state vars is returned.
The order in which the states are started is determined by their load order by the Clojure compiler.
Calling `(stop)` stops all the states in reverse order.

```clj
(mount/start)
;=> (#'your.app.config/config #'your.app/db)

db
;=> object[some.db.Object 0x12345678]

(mount/stop)
;=> (#'your.app/db #'your.app.config/config)

db
;=> #object[mount.lite.Unstarted 0x263571dd "State #'your.app/db is not started."]
```

*That's it, enjoy!*

## License

Copyright © 2016 Functional Bytes

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.

> Master branch: [![Circle CI](https://circleci.com/gh/aroemers/mount-lite/tree/master.svg?style=svg)](https://circleci.com/gh/aroemers/mount-lite/tree/master)
