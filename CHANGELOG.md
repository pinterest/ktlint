# Changelog
All notable changes to this project will be documented in this file.  
This project adheres to [Semantic Versioning](http://semver.org/).

## [0.14.0] - 2017-11-30

### Changed
- `continuation_indent_size` to 4 when `--android` profile is used ([android/kotlin-guides#37](https://github.com/android/kotlin-guides/issues/37)). 

### Fixed
- Maven integration ([#117](https://github.com/shyiko/ktlint/issues/117)).

## [0.13.0] - 2017-11-28

### Added
- `no-line-break-before-assignment` ([#105](https://github.com/shyiko/ktlint/issues/105)),  
  `chain-wrapping` ([#23](https://github.com/shyiko/ktlint/issues/23))
(when wrapping chained calls `.`, `?.` and `?:` should be placed on the next line),  
  `range-spacing` (no spaces around range (`..`) operator) rules.
- `--print-ast` CLI option which can be used to dump AST of the file   
(see [README / Creating a ruleset / AST](https://github.com/shyiko/ktlint#ast) for more details)
- `--color` CLI option for colored output (where supported, e.g. --print-ast, default (plain) reporter, etc) 

### Changed
- `.editorconfig` property resolution.   
An explicit `[*.{kt,kts}]` is not required anymore (ktlint looks for sections
containing `*.kt` (or `*.kts`) and will fallback to `[*]` whenever property cannot be found elsewhere).   
Also, a search for .editorconfig will no longer stop on first (closest) `.editorconfig` (unless it contains `root=true`). 
- `max-line-length` rule to assume `max_line_length=100` when `ktlint --android ...` is used  
(per [Android Kotlin Style Guide](https://android.github.io/kotlin-guides/style.html)).  
- `kotlin-compiler` version to 1.2.0 (from 1.1.51).

### Fixed
- `no-empty-class-body` auto-correction at the end of file ([#109](https://github.com/shyiko/ktlint/issues/109)).
- `max-line-length` rule when applied to KDoc ([#112](https://github.com/shyiko/ktlint/issues/112))  
(previously KDoc was subject to `max-line-length` even though regular comments were not).
- Spacing around `=` in @annotation|s (`op-spacing`).
- Spacing around generic type parameters of functions (e.g. `fun <T>f(): T {}` -> `fun <T> f(): T {}`).
- `no-consecutive-blank-lines` not triggering at the end of file (when exactly 2 blank lines are present) ([#108](https://github.com/shyiko/ktlint/issues/108)) 
- `indent` `continuation_indent_size % indent_size != 0` case ([#76](https://github.com/shyiko/ktlint/issues/76))
- `indent` rule skipping first parameter indentation check. 
- `final-newline` rule in the context of kotlin script.
- Git hook (previously files containing space character (among others) in their names were ignored)  
- Exit code when file cannot be linted due to the invalid syntax or internal error.

## [0.12.1] - 2017-11-13

### Fixed
- A conflict between `org.eclipse.aether:aether-*:1.1.0` and `org.eclipse.aether:aether-*:1.0.0.v20140518` ([#100](https://github.com/shyiko/ktlint/issues/100)).

## [0.12.0] - 2017-11-10

### Added
- `--android` (`-a`) CLI option (turns on [Android Kotlin Style Guide](https://android.github.io/kotlin-guides/style.html) compatibility)  
(right now it's used only by `ktlint --apply-to-idea`).

### Changed
- `ktlint --apply-to-idea` to account for `indent_size` & `continuation_indent_size` in `.editorconfig` (if any). 

### Removed
- `ktlint-intellij-idea-integration` binary (deprecated in [0.9.0](#090---2017-07-23)).

### Fixed
- "Unused import" false positive (`component1`..`component5`).

## [0.11.1] - 2017-10-26

### Fixed
- `--reporter`'s `output` handling (previously parent directory was expected to exist) ([#97](https://github.com/shyiko/ktlint/issues/97)).

## [0.11.0] - 2017-10-25

### Added
- `no-blank-line-before-rbrace` rule ([#65](https://github.com/shyiko/ktlint/issues/65)).

### Fixed
- Redundant space inserted between `}` and `::` (curly-spacing).

## [0.10.2] - 2017-10-25 [YANKED]

This release contains changes that were meant for 0.11.0 and so it was retagged as such.

## [0.10.1] - 2017-10-22

### Fixed
- Redundant space inserted between `}` and `[key]`/`(...)` (curly-spacing) ([#91](https://github.com/shyiko/ktlint/issues/91)).

## [0.10.0] - 2017-10-10

### Added

- Git hook to automatically check files for style violations on commit   
(execute `ktlint --install-git-pre-commit-hook` to install).
- Ability to specify multiple reporters   
(output can be controlled with `--reporter=<name>,output=<path/to/file>`) ([#71](https://github.com/shyiko/ktlint/issues/71)).
- Support for `indent_size=unset` (`.editorconfig`) ([#70](https://github.com/shyiko/ktlint/issues/70)).

### Fixed
- `( {` formatting   
(previously both `( {` and `({` were accepted as correct, while only `({` should be) (`curly-spacing` rule) ([#80](https://github.com/shyiko/ktlint/issues/80)).
- `if\nfn {}\nelse` formatting (`curly-spacing` rule). 
- `max_line_length=off` & `max_line_length=unset` handling (`.editorconfig`).

### Changed
- `kotlin-compiler` version to 1.1.51 (from 1.1.3-2).
- `ktlint --apply-to-idea` to include `OPTIMIZE_IMPORTS_ON_THE_FLY=true`. 

## [0.9.2] - 2017-09-01

### Fixed

- `: Unit =` formatting (`: Unit` is no longer dropped when `=` is used) ([#77](https://github.com/shyiko/ktlint/issues/77)). 

## [0.9.1] - 2017-07-30

### Fixed

- `${super.toString()}` linting (`string-template` rule) ([#69](https://github.com/shyiko/ktlint/issues/69)). 

## [0.9.0] - 2017-07-23

### Added

- [Reporter](ktlint-core/src/main/kotlin/com/github/shyiko/ktlint/core/Reporter.kt) API.   
`ktlint` comes with 3 built-in reporters: `plain` (default; `?group_by_file` can be appended to enable grouping by file (shown below)), `json` and `checkstyle`. 
```
$ ktlint --reporter=plain?group_by_file
path/to/file.kt
  1:10 Unused import.
  2:10 Unnecessary "Unit" return type.
path/to/another-file.kt
  1:10 Unnecessary semicolon.
```   
- [string-template](https://ktlint.github.io/#rule-string-template),  
[no-empty-class-body](https://ktlint.github.io/#rule-empty-class-body),  
max-line-length ([#47](https://github.com/shyiko/ktlint/issues/47)),  
final-newline (activated only if `insert_final_newline` is set in `.editorconfig` (under `[*.{kt,kts}]`)) rules.
- `--limit` CLI option (e.g. use `--limit=10` to limit the number of errors to display).
- `--relative` CLI flag which makes `ktlint` output file paths relative to working directory (e.g. `dir/file.kt` instead of
`/home/269/project/dir/file.kt`).

### Changed

- **BREAKING**: JDK version to 1.8 (as a result of upgrading `kotlin-compiler` to 1.1.3-2 (from 1.1.0)).
- File matching (offloaded to [klob](https://github.com/shyiko/klob)).  

### Deprecated

- `--ruleset-repository` and `--ruleset-update` CLI arguments in favour of `--repository` and `--repository-update` 
respectively (`--ruleset-*` will be removed in 1.0.0).  
- `ktlint-intellij-idea-integration` binary   
([Intellij IDEA integration](https://github.com/shyiko/ktlint#option-1-recommended) task is now included in `ktlint` (as `ktlint --apply-to-idea`)).

## [0.8.3] - 2017-06-19

### Fixed

- "Missing spacing after ";"" at the end of package declaration ([#59](https://github.com/shyiko/ktlint/issues/59)).
- "Unused import" false positive (`setValue`) ([#55](https://github.com/shyiko/ktlint/issues/55)).
- `get`/`set`ter spacing ([#56](https://github.com/shyiko/ktlint/pull/56)).

## [0.8.2] - 2017-06-06

### Fixed

- "Unused import" false positive (`getValue`) ([#54](https://github.com/shyiko/ktlint/issues/54)).

## [0.8.1] - 2017-05-30

### Fixed

- `ktlint --stdin` ([#51](https://github.com/shyiko/ktlint/issues/51)).

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

- [no-unit-return](https://ktlint.github.io/#rule-unit-return) rule.
- [modifier-order](https://ktlint.github.io/#rule-modifier-order) rule ([#42](https://github.com/shyiko/ktlint/issues/42)).
- `else/catch/finally` on the same line as `}` check (now part of "keyword-spacing" rule).
- `ktlint-intellij-idea-integration` binary for easy Intellij IDEA config injection.

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

[0.14.0]: https://github.com/shyiko/ktlint/compare/0.13.0...0.14.0
[0.13.0]: https://github.com/shyiko/ktlint/compare/0.12.1...0.13.0
[0.12.1]: https://github.com/shyiko/ktlint/compare/0.12.0...0.12.1
[0.12.0]: https://github.com/shyiko/ktlint/compare/0.11.1...0.12.0
[0.11.1]: https://github.com/shyiko/ktlint/compare/0.11.0...0.11.1
[0.11.0]: https://github.com/shyiko/ktlint/compare/0.10.1...0.11.0
[0.10.2]: https://github.com/shyiko/ktlint/compare/0.10.1...0.10.2
[0.10.1]: https://github.com/shyiko/ktlint/compare/0.10.0...0.10.1
[0.10.0]: https://github.com/shyiko/ktlint/compare/0.9.2...0.10.0
[0.9.2]: https://github.com/shyiko/ktlint/compare/0.9.1...0.9.2
[0.9.1]: https://github.com/shyiko/ktlint/compare/0.9.0...0.9.1
[0.9.0]: https://github.com/shyiko/ktlint/compare/0.8.3...0.9.0
[0.8.3]: https://github.com/shyiko/ktlint/compare/0.8.2...0.8.3
[0.8.2]: https://github.com/shyiko/ktlint/compare/0.8.1...0.8.2
[0.8.1]: https://github.com/shyiko/ktlint/compare/0.8.0...0.8.1
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
