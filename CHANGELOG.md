# Change Log

## [Unreleased][unreleased]
### Added
- Add `:bindings` option to `defstate` and `start`. See README for details.


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

[unreleased]: https://github.com/aroemers/mount-lite/compare/0.9.4...HEAD
[0.9.4]: https://github.com/aroemers/mount-lite/compare/0.9.3...0.9.4
[0.9.3]: https://github.com/aroemers/mount-lite/compare/0.9.2...0.9.3
[0.9.2]: https://github.com/aroemers/mount-lite/compare/0.9.1...0.9.2
[0.9.1]: https://github.com/aroemers/mount-lite/compare/0.9...0.9.1
