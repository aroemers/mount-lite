# Troubleshooting

## Why does the `defstate` that I added in a REPL session not get started between the defstates where I defined it?

Each `defstate` gets an order sequence number when they are loaded for the first time.
So adding a new `defstate` in a live session, means that that state will have the highest sequence number.
Restarting the JVM is one way to fix this.
If you really need/want to keep the JVM running, you could also swap in the namespace pointing to new defstate in [the \*states\* root atom](05-extension-point.html).
