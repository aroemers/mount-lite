# Change Log


## [0.9.1] - 2016-01-30
### Added
- `:on-reload` option has been added. Defaults to `:stop`, but can be set to only redefine the `:lifecycle` expressions.

### Removed
- `:stop-on-reload?` option does not exist anymore. It has been replaced with the `:on-reload`.

### Changed
- The order sequence number now increments in steps of 10. This allows new defstates in a REPL session to be placed in between other states (using `alter-meta!`).
- Moved some code from macros to functions in internals.
- The lifecycle keywords in the metadata of the state vars are not namespaced anymore.


## 0.9
- Initial release

[unreleased]: https://github.com/aroemers/mount-lite/compare/0.9.1...HEAD
[0.9.1]: https://github.com/aroemers/mount-lite/compare/0.9...0.9.1
