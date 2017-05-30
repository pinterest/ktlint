# Changelog
All notable changes to this project will be documented in this file.  
This project adheres to [Semantic Versioning](http://semver.org/).

## [0.8.0] - 2017-05-30

### Added

- [.editorconfig](http://editorconfig.org/) support (right now only `indent_size` is honored and only if it's 
set in `[*{kt,kts}]` section).
- Support for vertically aligned comments (see [NoMultipleSpacesRuleTest.kt](ktlint-ruleset-standard/src/test/kotlin/com/github/shyiko/ktlint/ruleset/standard/NoMultipleSpacesRuleTest.kt)).

### Fixed

- ktlint-ruleset-standard ("no-unit-return" & "modifier-order" where not included).

## [0.7.1] - 2017-05-29

### Fixed

- Triggering of "Unused import" when element is referenced in KDoc(s) only ([#46](https://github.com/shyiko/ktlint/issues/46)).

## [0.7.0] - 2017-05-28

### Added

- "no-unit-return" rule.
- "modifier-order" rule ([#42](https://github.com/shyiko/ktlint/issues/42)).
- `else/catch/finally` on the same line as `}` check (now part of "keyword-spacing" rule).
- "ktlint-intellij-idea-integration" binary for easy Intellij IDEA config injection.

## [0.6.2] - 2017-05-22

### Fixed
- Unused "iterator" extension function import false positive ([#40](https://github.com/shyiko/ktlint/issues/40)).

## [0.6.1] - 2017-03-06

### Fixed
- Detection of unnecessary "same package" imports (no-unused-imports).
- FileNotFoundException while scanning FS ([#36](https://github.com/shyiko/ktlint/issues/36)).

## [0.6.0] - 2017-03-01

### Changed
- `kotlin-compiler` version to 1.1.0 (from 1.1-M04).

## [0.5.1] - 2017-02-28

### Fixed
- Unnecessary spacing around angle brackets in case of `super<T>` ([#34](https://github.com/shyiko/ktlint/issues/34)).

## [0.5.0] - 2017-02-20

### Fixed
- Redundant space inserted between `}` and `!!` (curly-spacing).

### Changed
- `indent` rule to allow "Method declaration parameters -> Align when multiline" (as this option is (unfortunately) "on" by default in Intellij IDEA) ([#26](https://github.com/shyiko/ktlint/issues/26)).

## [0.4.0] - 2017-02-01

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

[0.8.0]: https://github.com/shyiko/ktlint/compare/0.7.1...0.8.0
[0.7.1]: https://github.com/shyiko/ktlint/compare/0.7.0...0.7.1
[0.7.0]: https://github.com/shyiko/ktlint/compare/0.6.2...0.7.0
[0.6.2]: https://github.com/shyiko/ktlint/compare/0.6.1...0.6.2
[0.6.1]: https://github.com/shyiko/ktlint/compare/0.6.0...0.6.1
[0.6.0]: https://github.com/shyiko/ktlint/compare/0.5.1...0.6.0
[0.5.1]: https://github.com/shyiko/ktlint/compare/0.5.0...0.5.1
[0.5.0]: https://github.com/shyiko/ktlint/compare/0.4.0...0.5.0
[0.4.0]: https://github.com/shyiko/ktlint/compare/0.3.1...0.4.0
[0.3.1]: https://github.com/shyiko/ktlint/compare/0.3.0...0.3.1
[0.3.0]: https://github.com/shyiko/ktlint/compare/0.2.2...0.3.0
[0.2.2]: https://github.com/shyiko/ktlint/compare/0.2.1...0.2.2
[0.2.1]: https://github.com/shyiko/ktlint/compare/0.2.0...0.2.1
[0.2.0]: https://github.com/shyiko/ktlint/compare/0.1.2...0.2.0
[0.1.2]: https://github.com/shyiko/ktlint/compare/0.1.1...0.1.2
[0.1.1]: https://github.com/shyiko/ktlint/compare/0.1.0...0.1.1
