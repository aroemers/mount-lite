# Reloading defstates

A library like mount-lite is developed for developing applications quickly, working nicely together with a REPL.
Reloading namespaces (or parts thereof) often helps a great deal.
Whenever you reload, you don't want any old state hanging around.
The mount-lite library is great for reloading parts of your application often.

## Default reloading behaviour

Whenever you redefine a defstate var, by default that defstate _and_ all the states depending on that state will be stopped automatically (in reverse order).
We call this a cascading stop, and uses an internal graph to determine the dependents.
For example:

```clj
(defstate a :start nil :stop (println "Stopping a"))
(defstate b :start nil :stop (println "Stopping b"))
(defstate c :start nil :stop (println "Stopping c"))

(start)
;=> (#'user/a #'user/b #'user/c)

(defstate b :start nil :stop (println "Different stop b"))
;;> Stopping c
;;> Stopping b

(start)
;=> (#'user/b #'user/c)
```

Note how the the `#'user/c` defstate var has been stopped automatically as well, as it depends on the redefined `#'user/b` defstate.
Also note that the `:stop` expression from `#'user/b` is used from the definition as it was started.

This cascading is great to work with, and in combination with the [tools.namespace](https://github.com/clojure/tools.namespace) library it can really shine.
Whenever you make sure your namespaces with `defstate` definitions have `{:clojure.tools.namespace.repl/unload false}` as metadata, calling `(clojure.tools.namespace.repl/refresh :after 'mount.lite/start)` will only stop the required states (in correct order) and restart them.

> NOTE: If you want your namespaces to be unloaded when using `c.t.n.r/refresh`, i.e. not including the mentioned metadata, make sure you call `(stop)` beforehand.

> NOTE: This cascading behaviour is actually implemented using the `:up-to` option, as described in the section about start/stop options.

## Overriding the default

While this cascading stop is what you want most of the times, there may be situations where you don't want this reloading and/or cascading stop behaviour.

### The :on-reload option

To alter the reloading behaviour, one can set a different mode via the `:on-reload` option on a `defstate`. You can set the option to one the following modes:

* `:cascade` - This is the default, as described above.

* `:stop` - This will stop only the state that is being redefined.

* `:lifecycle` -  This will only redefine the lifecycle functions, and keep the state running as is (including the accompanying `:stop` expression). I.e, it is only after a (re)start that the redefinition will be used.

### The :on-cascade option

If you don't want your `defstate` to be stopped whenever a dependency is stopped because it is redefined, you can have your defstate skip the cascading stop with the `:on-cascade` option on your `defstate`.
If you set this to `:skip`, the defstate won't be stopped automatically whenever a dependency is redefined that has `:cascade` as its `:on-reload` behaviour.

> NOTE: You can also override the `:on-reload` behaviour of all the `defstates` by setting a behaviour using the `on-reload` function. By setting is back to `nil`, the `:on-reload` setting of the `defstates` is used again.
