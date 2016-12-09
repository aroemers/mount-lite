# Extension point

In former versions of mount-lite the API gave you more fine-grained influence on what states are started or stopped when calling `start` or `stop`.
You could tell mount to `only` start/stop certain states, or start/stop all states `except` some.
You could tell mount to determine the dependencies of a state more accurately by building a dependency graph using the [tools.namespace](https://github.com/clojure/tools.namespace) library.

These features have been removed from the API, for [various reasons](http://www.functionalbytes.nl/clojure/mount/mount-lite/2016/12/09/mount-lite-2.html).
However, an extension point is available, in case you want these or other features that influence what is started or stopped.
Future versions may even provide implementations for these, using the extension point discussed below.
In any case, the core API and implementation is not affected by these features and can be kept simple and lite.

### The *states* dynamic var

The root binding of the dynamic `*states*` var is used by the internals of mount-lite to keep track of what global `defstates` have been defined, and in what order.
The root binding is an atom of namespaced keywords, which can be resolved back to the `defstate` vars.
These keywords are automatically pruned, in case some `defstate` may have been unmapped.

The `start` and `stop` functions read from the atom in the `*states*` var, to determine what states are available for starting or stopping.
By binding the `*states*` var to an atom of your own, holding a different set of namespaced keywords, you effectively influence the `start` and `stop` function in this regard.

Do not fiddle with the root binding of the `*states*` var directly!

### Bringing back "only"

For example, if you'd really need the `only` option back, you could do the following:

```clj
(defmacro with-only
  [states & body]
  `(binding [*states* (atom (map var->keyword ~states))]
     ~@body))
```
