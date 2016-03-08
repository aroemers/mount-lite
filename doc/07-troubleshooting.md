# Troubleshooting

## Why does the `defstate` that I added in a REPL session not get started between the defstates where I defined it?

Each `defstate` gets an order sequence number when they are loaded for the first time.
So adding a new `defstate` in a live session, means that that state will have the highest sequence number.
Restarting the JVM is one way to fix this.
Another more advanced way to fix this, is the following:
```clj
;; Define your new defstate somewhere:
(defstate shiny ...)

;; Update the :mount.lite/order metadata key with the correct number.
;; Luckily the order sequence number is incremented by 10, so there must som space left.
(alter-meta! #'shiny assoc :mount.lite/order <an-order-number-that-is-between-its-dependents-and-dependencies>)
```

## Why does the dependency graph (used in `:up-to` and `:parallel`) not see the dependency of a namespace I just added?

The namespaces of the Clojure files are read by the [tools.namespace](https://github.com/clojure/tools.namespace) library.
This means that the `(ns ...)` forms are read from the file system.
If you only made your changed in the REPL, tools.namespace won't see them.
Also, if you made your changes in a file from a [checkout](https://github.com/technomancy/leiningen/blob/master/doc/TUTORIAL.md#checkout-dependencies) dependency, tools.namespace won't see the changes either!
In the latter case, you'll have to install the dependency again.

## Why do I get the error `No such var: parse/name-from-ns-decl`?

Mount-lite depends on the `0.3.0` version of [tools.namespace](https://github.com/clojure/tools.namespace).
Older versions do not contain the mentioned var.
Make sure your dependencies are set up correctly, also in your (REPL) profile.
A leiningen upgrade can help as well.
