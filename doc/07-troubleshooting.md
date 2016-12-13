# Troubleshooting

## Why does the _defstate_ that I added in a REPL session not get started between the defstates where I defined it?

Each `defstate` gets an order sequence number when they are loaded for the first time.
So adding a new `defstate` in a live session, means that that state will have the highest sequence number.
Restarting the JVM is one way to fix this.
If you really need/want to keep the JVM running, you could also swap in the namespace pointing to new defstate in [the \*states\* root atom](05-extension-point.html).

## Why do I get an exception, or a value from a different session, when I access a state?

By spawning a new thread with a new system of states using the `with-session` macro, the subthreads spawned from that session automatically use the system of states from that session.
But, if you use threads in your session that were created _outside_ of your session, those threads use the system of states from the (root) session in which they were created.
So, do keep in mind that `future`s and `agent`s use global ThreadPools from the Clojure runtime.
There are simple ways to work around this though, and mount-lite may even provide utilities for this at a later stage.
