# Change Log

## [2.1.0] - 2017-12-30
### Added
- Extension for inferred dependency graph among defstates using the optional [tools.namespace](https://github.com/clojure/tools.namespace#reloading-code-usage) library.

## [2.0.0] - 2017-08-24 - BREAKING

- See README and documentation for changes and migration guide.


## [0.9.8] - 2016-05-25
### Added
- Setting a log function via `log-fn`, which is called on state changes.
- An exception is thrown when the state graph cannot be created due to missing sources. This only affects functions such as `parallel` and `up-to`.

### Changed
- The `defstate` macro now disallows defining a var that already exists and is not a defstate.
- The documentation has moved to codox

## [0.9.7] - 2016-03-01
### Fixed
- Allow AOT-ing of defstates


## [0.9.6] - 2016-02-29
### Added
- Add optional bindings to `defstate`, and `:bindings` option to `start`. See README for details.
- Added `:on-reload` option for `defstate`.
- Added `:on-cascade` option for `defstate`.

## Changed
- The `on-reload` function now defaults to `nil`, and only overrides the states' `:on-reload` when set to a non-nil value.


## [0.9.5] - 2016-02-20
### Fixed
- Fix hang when starting or stopping no states in parallel.
- Fix wrong initialisation of parallel fork tasks.

### Added
- The `dot` function, for generating a Graphviz graph of all the states.


## [0.9.4] - 2016-02-11
### Changed
- Memoization (with a cache of 1) has been added to the var dependency graph calculation, for speed.

### Fixed
- Stopping `:up-to` an already stopped state now will stop its started dependencies. Same for starting up to an already started state.
- Starting or stopping `:up-to` an already started or stopped state will now not start or stop that state again.


## [0.9.3] - 2016-02-05
### Added
- Add `:parallel` option to `start` and `stop` options. Value is number of threads to use, to start/stop independent states in parallel.
- Internally mount-lite now builds a proper graph of dependencies, instead of an ordered sequence.
- Add processing of `:dependencies` meta data in a state var, in order to influence the deduced state dependency graph.
- Add `status` function to main API.

### Changed
- Start/stop option `:up-to` now uses the internal dependency graph to only start (or stop) the dependencies (or dependents) of the given var.
- Because of the finer grained `:up-to` behaviour, the "cascading stop" on a state redefinition has also improved.


## [0.9.2] - 2016-01-30
### Added
- Add the `on-reload` function to the API, replacing the `:on-reload` state option.
- Add the `:cascade` mode for `on-reload`.

### Removed
- Remove the `:on-reload` state option. It is replaced by the `on-reload` function.

### Fixed
- Fix docstring of `defstate` mentioning removed `:stop-on-reload?` key.
- Fix stale metadata on redefinition with `on-reload` set to `:lifecycle`.


## [0.9.1] - 2016-01-30
### Added
- Add `:on-reload` option. Defaults to `:stop`, but can be set to only redefine the `:lifecycle` expressions.

### Removed
- Removed `:stop-on-reload?` option. It has been replaced with the `:on-reload` option.

### Changed
- The order sequence number now increments in steps of 10. This allows new defstates in a REPL session to be placed in between other states (using `alter-meta!`).
- Moved some code from macros to functions in internals.
- The lifecycle keywords in the metadata of the state vars are not namespaced anymore.


## 0.9
- Initial release

[2.0.0]: https://github.com/aroemers/mount-lite/compare/2.1.0...2.0.0
[2.0.0]: https://github.com/aroemers/mount-lite/compare/2.0.0...0.9.8
[0.9.8]: https://github.com/aroemers/mount-lite/compare/0.9.7...0.9.8
[0.9.7]: https://github.com/aroemers/mount-lite/compare/0.9.6...0.9.7
[0.9.6]: https://github.com/aroemers/mount-lite/compare/0.9.5...0.9.6
[0.9.5]: https://github.com/aroemers/mount-lite/compare/0.9.4...0.9.5
[0.9.4]: https://github.com/aroemers/mount-lite/compare/0.9.3...0.9.4
[0.9.3]: https://github.com/aroemers/mount-lite/compare/0.9.2...0.9.3
[0.9.2]: https://github.com/aroemers/mount-lite/compare/0.9.1...0.9.2
[0.9.1]: https://github.com/aroemers/mount-lite/compare/0.9...0.9.1
