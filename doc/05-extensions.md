# Extension point

The behaviour of mount-lite is pretty straightforward.
You start your states, use them, and stop them again.
There is even some control of what part of system to start (or stop), by [starting the system partially](02-partial-system.md).

However, one can think of all kinds of ways on how to have more control over what is started or stopped.
For this, mount-lite offers an extension point.
Such an extension point was introduced in mount-lite 2.0, and has been improved upon in 3.0.
It now leaks less of the internal state and ensures composability between extensions without effort from the extension developer.

## Creating your own extension

It works by letting you provide a predicate.
This predicate can be provided using the `mount.extensions/with-predicate` macro.
The first argument is the predicate, the rest of the arguments are considered its body.
It is within this body that the predicate is active.
Multiple of these `with-predicate` expressions can be nested, composing the predicates.

Each predicate receives a state that is about to be started or stopped.
All active predicates must agree on allowing this.

For example, the following extension only allows states to start or stop when it is defined in a certain namespace:

```clj
(use 'mount.extensions)

(defn- predicate-factory [ns]
  (fn [state]
    (= (str ns) (namespace state))))

(defmacro only-namespace [ns & body]
  `(with-predicate (predicate-factory ~ns)
    ~@body))
```

And here is how this extension could be used:

```clj
(only-namespace *ns*
  (start))
```

This particular extension may not be that useful, but it shows how the extension point works.
It also shows that a state implements `INamed`, making it suitable for the `namespace` and `name` core functions.

## Provided extensions

Mount-lite provides several extensions out of the box.
The [starting "up-to"](02-partial-system.md) feature is actually implemented as an extension.
But there are several more.

1. The [basic](https://cljdoc.org/d/functionalbytes/mount-lite/CURRENT/api/mount.extensions.basic) extension offers two composable macros called `with-only` and `with-except`, by which you can explicitly control which states are started/stopped.
2. The [data-driven](https://cljdoc.org/d/functionalbytes/mount-lite/CURRENT/api/mount.extensions.data-driven) extension offers a way of specifying how a system of states is started by supplying a pure data map.
3. The [namespace-deps](https://cljdoc.org/d/functionalbytes/mount-lite/CURRENT/api/mount.extensions.namespace-deps) extension enhances the ["up-to" feature](02-partial-system.md) of mount-lite by using a namespace dependency tree to determine which states should be started or stopped, instead of the default linear behaviour.
4. The [refresh](https://cljdoc.org/d/functionalbytes/mount-lite/CURRENT/api/mount.extensions.refresh) extension wrap the [tools.namespace library](https://github.com/clojure/tools.namespace) refresh functionality, by first stopping the to-be-reloaded states before the actual refresh.
After the refresh the stopped states are started again.
