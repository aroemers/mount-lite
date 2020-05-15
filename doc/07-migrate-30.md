# Migrate to 3.0

Below are the changes from the 2.0 version to version 3.0.
While backwards compatability has been kept in most of the core API, there are some changes that you need to know about and in some cases you might need to change some source code.

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

Your custom extensions will need to be revised to use the new [extension point](05-extensions.md).
The old extension point is not available anymore.
It will probably simplify your extension however.
