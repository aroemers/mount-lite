# Extension point

The behaviour of mount-lite is pretty straightforward.
You start your states, use them, and stop them again.
There is even some control of what part of system to start (or stop), by [starting the system partially]().

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

## Provided extensions

Mount-lite provides several extensions out of the box.
The [starting "up-to"]() feature is actually implemented as an extension.
But there are several more.

### Extension 1: Basic

### Extension 2: Data-driven

### Extension 3: Namespace-based dependency graph

### Extension 4: Refresh
