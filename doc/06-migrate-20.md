# Migrate to 2.0

Version 2.0 has breaking changes with respect to version 1.0.
Below are the changes from the 1.0 version to version 2.0.

### Simplified start/stop

Where the `start` and `stop` functions used to take zero to multiple "option maps", they now take [one optional argument](02-partial-system.md): the "up to" var.

If you were using the `only` or `except` options, see if you can replace them with the "up to" approach.
Otherwise use the [basic extension](https://cljdoc.org/d/functionalbytes/mount-lite/CURRENT/api/mount.extensions.basic).

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
You need to manually `(stop)` and `(star)t` your states, or use the [refresh extension](https://cljdoc.org/d/functionalbytes/mount-lite/CURRENT/api/mount.extensions.refresh).
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
