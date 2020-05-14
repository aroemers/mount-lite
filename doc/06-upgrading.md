# Migrating to 2.0 and 3.0

Major versions of mount-lite contain breaking changes.
Below you can read how to migrate to the next major version.


## Migrate to 3.0

Below are the changes from the 2.0 version to version 3.0.

### Core API now take plain defstates as arguments

Instead of using vars that hold defstates, mount-lite's functions now take the defstate themselves as input.
The documentation reflects that.
That said, vars are still supported for backwards compatibility.
For example, calling `(start #'my-state)` is still possible.

### Core API now takes maps instead of vectors

Instead of using vectors for macros such as `with-substitutes`, they now simply take a map.
The documentation reflects that.
That said, vectors are still supported for backwards compatilibilty.
For example, calling `(with-substitutes [#'my-state sub] ...)` is still supported.

### Redefining a defstate influences stop logic of started states

Before mount-lite 3.0 redefining a defstate did not influence the stop logic of an already started state.
In other words, stopping the state would use the old logic and it was only when starting it again that the new stop logic would be used.

In mount-lite 3.0 this has changed.
Now when you redefine a defstate, stopping an already started state will use the new stop logic.

### Status map does not have vars as its keys

The return value of the `(status)` function used to have vars as its keys.
Now the keys are the defstates themselves.

These defstates do implement the `INamed` protocol, so one can use the `namespace` and `name` core functions on them.

### Extension point has been reworked

Your custom extensions will need to be revised to use the new [extension point]().
The old extension point is not available anymore.
It will probably simplify your extension however.


## Migrate to 2.0

Below are the changes from the 1.0 version to version 2.0.

### Simplified start/stop

Where the `start` and `stop` functions used to take zero to multiple "option maps", they now take [one optional argument](): the "up to" var.

If you were using the `only` or `except` options, see if you can replace them with the "up to" approach.
Otherwise use the [basic extension](05-extension-point.md).

### Dereferencing

To use the value of a started state, you need to dereference it by using `@` or `deref` now.
Dereferencing a stopped state yields an exception.

### Substitutes need to be defined with the _state_ macro

Substitutes cannot be defined as a simple map anymore.
Please use the `state` macro to create anonymous states.
The syntax for the `state` macro has not changed though, with respect to versions before 2.0.
For example:

```clj
;; This is not supported anymore
(def sub {:start (fn [] ...)})

;; This is the only supported syntax now
(def sub (state :start ...))
```

### No more reloading behaviour

States are not automatically stopped anymore on redefinition.
You need to manually `(stop)` and `(star)t` your states, or use the [refresh extension]().
This also means that the `:on-reload` and `:on-cascade` options of `defstate` are gone, just as the `on-reload` function.

### No more graphs

Detecting the dependency graph for the `defstates` has been removed from the core.
It has returned as an another form as the [[mount.extensions.explicit-deps]] extension.
This means that the "up-to" behaviour may now start or stop more states than you were used to.

### No more parallel

Because state dependency graphs have been removed from the core behaviour, starting or stopping states in `parallel` has been removed as well.

### No more bindings

The `bindings` feature has been removed as well.
It was rarely used, and only added complexity to the macros.
If you use the feature, it can easily be replaced by many other means.
