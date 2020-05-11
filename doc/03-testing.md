# Testing features

The mount-lite library has two main testing features, which can also be of use in the REPL.
As with everything in mount-lite, they are composable and can be used together.

## Method 1: with-system-map

A unique feature of mount-lite is the ability to override the value(s) of a state(s) within a well-defined scope.
One does this using the `with-system-map` macro.

The first argument to this macro is a map with global states to values, the rest of the arguments are considered the body of the expression.
The specified states now have the specified values inside the given body, regardless of their status outside the body.
Moreover, within the body, the specified states are now considered "started".

For example:

```clj
(defstate a :start 1)
(defstate b :start 2)

(start)
;=> (user/a user/b)

(with-system-map {a 10}
  (= 12 (+ @a @b)))
;=> true
```

The main advantage of this testing feature over the second one (discussed below), is that one does not need to start (part of) the system and making sure it is stopped again.
In other words, many simple unit tests only need a `with-system-map` with concrete state values.

## Method 2: with-substitutes

In case one writes more integration-like tests that need a (part of a) more "live" system, another testing feature is available.
Users of the original mount will recognize this: substituting.
Mount-lite's implementation improves upon this by ensuring that it is both functional and composable.

Using the `with-substitutes` macro, one overrides the start and stop logic of the global states within the scope of the macro.
If one starts and/or stops (part of) the system within this scope, the substituted logic is used.
Moreover, the states that are started within the scope, will hold on to the substituted stop logic, also outside the scope.

For example:

```clj
(defstate a :start 1 :stop (prn "stopping"))

(with-substitutes {a (state :start 11 :stop (prn "substituted stop"))}
  (start))
;=> (user/a)

@a
;=> 11

(stop)
"substituted stop"
```

Note that a substitute state can be created using the `state` macro.
More specifically, anything that implements the `IState` protocol can be used as a substitute.
