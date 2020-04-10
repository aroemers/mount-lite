# Migrating to 2.x

Version 2.0 was a breaking version.
This guide sums up what has changed.

## Simplified start/stop

Where the `start` and `stop` functions used to take zero to multiple "option maps", they now take [one optional argument](03-start-stop-options.md): the "up to" var.

If you were using the `only` or `except` options, see if you can replace them with the "up to" approach, or with the [extension point](05-extension-point.md), or keep using the 0.9.x version.

## Dereferencing

To use the value of a started state, you need to dereference it by using `@` or `deref` now.
Dereferencing a stopped state yields an exception.

## Substitutes need to be defined with the _state_ macro

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

## No more reloading behaviour

States are not automatically stopped anymore on redefinition.
You need to manually `stop` and `start` your states, or use the [tools.namespace](https://github.com/clojure/tools.namespace) library.
This also means that the `:on-reload` and `:on-cascade` options of `defstate` are gone, just as the `on-reload` function.

A [[mount.extensions.refresh]] extension has been added for an easy integration with tools.namespace library.

## No more graphs

Detecting the dependency graph for the `defstates` has been removed from the core.
It has returned as an another form as the [[mount.extensions.explicit-deps]] extension.
This means that the "up-to" behaviour may now start or stop more states than you were used to.

## No more parallel

Because state dependency graphs have been removed for now, starting or stopping states in `parallel` has been removed as well.

## No more bindings

The `bindings` feature has been removed as well.
It was rarely used, and only added complexity to the macros.
If you use the feature, it can easily be replaced by many other means.
