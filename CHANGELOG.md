# Change Log


## [0.9.1] - 2016-01-30
### Added
- `:on-reload` option has been added. Defaults to `:stop`, but can be set to only redefine the `:lifecycle` expressions.

### Removed
- `:stop-on-reload?` option does not exist anymore. It has been replaced with the `:on-reload`.

### Changed
- Moved some code from macros to functions in internals.
- The lifecycle keywords in the metadata of the state vars are not namespaced anymore.


## 0.9
- Initial release

[unreleased]: https://github.com/your-name/mount-lite/compare/0.9.1...HEAD
[0.9.1]: https://github.com/your-name/mount-lite/compare/0.9...0.9.1
