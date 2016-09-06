# Changelog
All notable changes to this project will be documented in this file.  
This project adheres to [Semantic Versioning](http://semver.org/).

## 0.2.0 - 2016-09-05

### Added
- Support for 3rd party "ruleset"s. 

### Changed
- `ktlint -F` output (it now includes lint errors that cannot be fixed automatically). 

### Fixed
- `ktlint -F --debug` error count.  
- Glob implementation (previously it was prone to catastrophic backtracking).
- Redundant semicolon false positive in case of enum ([#12](https://github.com/shyiko/ktlint/issues/12)).
- Unused operator import false positive ([#13](https://github.com/shyiko/ktlint/issues/13)).

## 0.1.2 - 2016-08-05

### Fixed
- "in-use" escaped imports detection ([#7](https://github.com/shyiko/ktlint/issues/7)) (no-unused-imports).   

## 0.1.1 - 2016-08-01

### Fixed
- Incorrect spacing around curly braces (curly-spacing).

## 0.1.0 - 2016-07-27
