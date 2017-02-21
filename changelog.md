# Changelog
All notable changes to this project will be documented in this file.  
This project adheres to [Semantic Versioning](http://semver.org/).

## [0.5.0] - 2017-02-20

### Fixed
- redundant space inserted between `}` and `!!` (curly-spacing).

### Changed
- `indent` rule to allow "Method declaration parameters -> Align when multiline" (as this option is (unfortunately) "on" by default in Intellij IDEA) ([#26](https://github.com/shyiko/ktlint/issues/26)).

## 0.4.0 - 2017-02-01

### Fixed
- NPE in case of "Permission denied" (while scanning the file system).

### Changed
- `kotlin-compiler` version to 1.1-M04 (from 1.0.6).

## [0.3.1] - 2017-01-25

### Fixed
- Unused infix function call import false positive ([#25](https://github.com/shyiko/ktlint/issues/25)).

## [0.3.0] - 2017-01-11

### Added 
- `*.kts` (script) support.

### Changed
- `kotlin-compiler` version to 1.0.6 (from 1.0.3).

## [0.2.2] - 2016-10-11

### Fixed
- `no-wildcard-imports` rule (kotlinx.android.synthetic excluded from check) ([#16](https://github.com/shyiko/ktlint/pull/16)).

## [0.2.1] - 2016-09-13

### Fixed
- `curly-spacing` false negative in case of `}?.`.

## [0.2.0] - 2016-09-05

### Added
- Support for 3rd party "ruleset"s. 

### Changed
- `ktlint -F` output (it now includes lint errors that cannot be fixed automatically). 

### Fixed
- `ktlint -F --debug` error count.  
- Glob implementation (previously it was prone to catastrophic backtracking).
- Redundant semicolon false positive in case of enum ([#12](https://github.com/shyiko/ktlint/issues/12)).
- Unused operator import false positive ([#13](https://github.com/shyiko/ktlint/issues/13)).

## [0.1.2] - 2016-08-05

### Fixed
- "in-use" escaped imports detection ([#7](https://github.com/shyiko/ktlint/issues/7)) (no-unused-imports).   

## [0.1.1] - 2016-08-01

### Fixed
- Incorrect spacing around curly braces (curly-spacing).

## 0.1.0 - 2016-07-27

[0.5.0]: https://github.com/shyiko/ktlint/compare/0.4.0...0.5.0
[0.4.0]: https://github.com/shyiko/ktlint/compare/0.3.1...0.4.0
[0.3.1]: https://github.com/shyiko/ktlint/compare/0.3.0...0.3.1
[0.3.0]: https://github.com/shyiko/ktlint/compare/0.2.2...0.3.0
[0.2.2]: https://github.com/shyiko/ktlint/compare/0.2.1...0.2.2
[0.2.1]: https://github.com/shyiko/ktlint/compare/0.2.0...0.2.1
[0.2.0]: https://github.com/shyiko/ktlint/compare/0.1.2...0.2.0
[0.1.2]: https://github.com/shyiko/ktlint/compare/0.1.1...0.1.2
[0.1.1]: https://github.com/shyiko/ktlint/compare/0.1.0...0.1.1
