# Multiple parallel systems

Another unique feature of mount-lite is that it supports running multiple systems of states in parallel.
This feature was introduced in version 2.0 and has been improved upon in version 3.0 by making it simpler.
Where the 2.0 version was based on spinning up a "session" thread, now you simply specify a system by its key, whatever thread you're in.
If not specified, the key is `:default`.

All it takes to utilise this is using the `with-system-key` macro.
It takes an arbritary object - the system key - as its first argument, the rest of the arguments are considered its body.
Within this body, the mount-lite functions now operate on the system identified by this system key, i.e. `(start)`, `(stop)`, `(status)` and dereferencing a state for its value.

For example, we can start the `:default` system:
```clj
(start)
;=> (my.app.config/config my.app/db)
```

Now we can start another system, with a different `my.app/db` implementation:

```
(with-system-key :empty-db
  (with-substitutes {my.app/db my.app.integration-test/empty-db}
    (start)))
;=> (my.app.config/config my.app/db)
```

Both systems are now running in parallel.
You can see this by calling some function that would use the states' values in its implementation:

```clj
(count-records-in-db)
;=> 4321

(with-system-key :empty-db
  (count-records-in-db))
;=> 0
```

To know which systems are active simply call `(system-keys)` for a set of system keys.
