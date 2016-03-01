# Bindings

## A word of advice first

It is generally best to define the `defstate`s in application namespaces, not in the more general (library) namespaces.
This is because the `:start` and `:stop` expressions are tightly coupled to their environment, including references to other states.
This is fine though, as you as the application writer have full control over your states, and resources should be at the periphery of the application anyway.

## Passing arguments to defstates on start

In the rare situations where you do need a looser coupling between the `:start`/`:stop` expressions and their environment, mount-lite has a unique feature called bindings.
When defining a `defstate`, one can optionally supply a vector, just before the `:start` and `:stop` expressions.
This vector declares the bindings that can be used by the `:start`/`:stop` expressions, and their defaults.
For example:

```clj
(defstate incrementer [i 10]
  :start (fn [n] (+ n i))
  :stop  (println "stopping incrementer of" i))
```

When the `incrementer` state is started normally, it will become a function that increments the argument by 10.
However, one can start the `incrementer` with different bindings, like so:

```clj
(mount/start (bindings #'incrementer '[i 20]))
;=> (#'incrementer)

(incrementer 5)
;=> 25

(stop)
;>> stopping 20 incrementer
;=> (#'incrementer)
```

As can be seen, the bindings that were used when starting the state are also used when stopping the state.

> NOTE: If you want to inspect what the binding values are when a state has started, consult the var meta keyseq `[:mount.lite/current :bindings]`.

## Another word of advice

This bindings feature can be used for passing in any kind of object.
Yet, at the current time of writing, my opinion is to use this feature sparingly.
Configuration can be read from some configuration state, and substitutions are normally sufficient for mocking.
Using bindings a lot, especially as some kind of dependency injection, might hint towards a design flaw.

Still, for passing in some configuration values (e.g. command line arguments), I think this is a nice and clean solution: no need for `alter-var-root`s, thread-local dynamic vars (which will break in parallel mode) or other fragile and rigid solutions.
Bindings in that sense offer an easy, cleanly scoped and semantically clear way of passing values to states, ensuring the same values on stop as when a state was started.
