# Changelog
All notable changes to this project will be documented in this file.  
This project adheres to [Semantic Versioning](http://semver.org/).

## Unreleased

### Added
- Add new applyToIDEA location for IDEA 2020.1.x and above on MacOs
- Debug output: print loaded .editorconfig content

### Fixed
- Do not enforce raw strings opening quote to be on a separate line ([#711](https://github.com/pinterest/ktlint/issues/711))
- False negative with multiline type parameter list in function signature for `parameter-list-wrapping`([#680](https://github.com/pinterest/ktlint/issues/680))
- Alternative `.editorconfig` path is ignored on stdin input ([#869](https://github.com/pinterest/ktlint/issues/869))

### Changed
- `Ktlint` object internal code cleanup
- Deprecate some of public methods in `Ktlint` object that should not be exposed as public api

## [0.38.1] - 2020-08-24
Minor release to support projects using mixed 1.3/1.4 Kotlin versions (e.g. Gradle plugins)

### Changed
- Compile with `apiLevel = 1.3`

## [0.38.0] - 2020-08-21

New release with Kotlin 1.4.0 support and several enhancements and bugfixes.

### Added
- Experimental SpacingAroundAngleBracketsRule ([#769](https://github.com/pinterest/ktlint/pull/769))
- Checksum generation for executable Jar ([#695](https://github.com/pinterest/ktlint/issues/695))
- Enable Gradle dependency verification
- `parameter-list-wrapping` rule now also considers function arguments while wrapping ([#620](https://github.com/pinterest/ktlint/issues/620))
- Publish snapshots built against kotlin development versions
- Initial support for tab-based indentation ([#128](https://github.com/pinterest/ktlint/issues/128))

### Fixed
- Safe-called wrapped trailing lambdas indented correctly ([#776](https://github.com/pinterest/ktlint/issues/776))
- `provideDelegate` imports are not marked as unused anymore ([#669](https://github.com/pinterest/ktlint/issues/669))
- Set continuation indent to 4 in IDE integration codestyle ([#775](https://github.com/pinterest/ktlint/issues/775)) 
- No empty lines between annotation and annotated target ([#688](https://github.com/pinterest/ktlint/issues/688))
- Unused imports reported correctly ([#526](https://github.com/pinterest/ktlint/issues/526)) ([#405](https://github.com/pinterest/ktlint/issues/405))
- No false empty lines inserted in multiline if-else block ([#793](https://github.com/pinterest/ktlint/issues/793))
- No-wildcard-imports properly handles custom infix function with asterisk ([#799](https://github.com/pinterest/ktlint/issues/799))
- Do not require else to be in the same line of a right brace if the right brace is not part of the if statement ([#756](https://github.com/pinterest/ktlint/issues/756))
- Brace-less if-else bodies starting with parens indented correctly ([#829](https://github.com/pinterest/ktlint/issues/829))
- If-condition with multiline call expression inside indented correctly ([#796](https://github.com/pinterest/ktlint/issues/796))

### Changed
- Update Gradle to 6.6 version
- Update ec4j to 0.2.2 version. Now it should report path to `.editorconfig` file on failed parsing 
and allow empty `.editorconfig` files.
- Update Kotlin to 1.4.0 version ([#830](https://github.com/pinterest/ktlint/issues/830))


## [0.37.2] - 2020-06-16

Minor release to fix further bugs in `ImportOrderingRule`.

### Fixed
- Imports with aliases no longer removed ([#766](https://github.com/pinterest/ktlint/issues/766))

## [0.37.1] - 2020-06-08

Minor release to fix some bugs in the 0.37.0 release.

### Fixed
- Invalid path exception error on Windows machines when loading properties from .editorconfig ([#761](https://github.com/pinterest/ktlint/issues/761))
- Imports with `as` no longer removed ([#766](https://github.com/pinterest/ktlint/issues/766))
- The contents of raw strings are no longer modified by the indent rule ([#682](https://github.com/pinterest/ktlint/issues/682))

## [0.37.0] - 2020-06-02

Thank you to [Tapchicoma](https://github.com/Tapchicoma) and [romtsn](https://github.com/romtsn) for all their hard work on this release!

### Added
- Gradle wrapper validation ([#684](https://github.com/pinterest/ktlint/pull/684))
- Experimental `SpacingAroundDoubleColon` rule ([#722](https://github.com/pinterest/ktlint/pull/722)]
- Experimental `SpacingBetweenDeclarationsWithCommentsRule` and `SpacingBetweenDeclarationsWithAnnotationsRule`. Fixes ([#721]https://github.com/pinterest/ktlint/issues/721)
- `kotlin_imports_layout` config for `.editorconfig` file so that import ordering is configurable. Fixes ([#527](https://github.com/pinterest/ktlint/issues/527))


### Changed
- Kotlin was updated to 1.3.70 version
- Loading properties from `.editorconfig` was fully delegated to ec4j library. This fixes ability to override
properties for specific files/directories ([#742](https://github.com/pinterest/ktlint/issues/742))
- Promote experimental "indent" rule to standard one, old standard "indent" rule is removed 
- Functions to calculate line/column are now public so they can be used by 3rd party tools ([#725](https://github.com/pinterest/ktlint/pull/725))
- `AnnotationRule` now handles file annotations as well ([#714](https://github.com/pinterest/ktlint/pull/714))

### Fixed
- Ignore keywords in KDoc comments ([#671](https://github.com/pinterest/ktlint/issues/671))
- Allow multiple spaces in KDoc comments ([#706](https://github.com/pinterest/ktlint/issues/706))
- Trailing comment no longer reported as incorrect indentation ([#710](https://github.com/pinterest/ktlint/issues/710)]
- Annotated function types no longer reported as an error ([#737](https://github.com/pinterest/ktlint/issues/737))
- `FinalNewlineRule` no longer reports error for empty files ([#723](https://github.com/pinterest/ktlint/issues/723))
- EOL comments will no longer cause `AnnotationRule` to report an error ([#736](https://github.com/pinterest/ktlint/issues/736))
- Formatter will no longer break class declaration with trailing comment ([#728](https://github.com/pinterest/ktlint/issues/728))
- Formatting for single line if/else statements ([#174](https://github.com/pinterest/ktlint/issues/174))
- Exception in `NoLineBreakBeforeAssignmentRule` ([#693](https://github.com/pinterest/ktlint/issues/693))


### Removed
- Removed Maven; builds all run under Gradle ([#445](https://github.com/pinterest/ktlint/issues/445))
- Old standard `IndentRule`

## [0.36.0] - 2019-12-03

### Added
- HTML reporter ([#641](https://github.com/pinterest/ktlint/pull/641))
- Experimental rule to lint enum entry names ([#638](https://github.com/pinterest/ktlint/pull/638))
- `@Suppress("RemoveCurlyBracesFromTemplate")` now respected ([#263](https://github.com/pinterest/ktlint/pull/263))

### Upgraded
- Gradle version to 5.6.2 ([#616](https://github.com/pinterest/ktlint/pull/616))
- Kotlin to 1.3.60 ([#658](https://github.com/pinterest/ktlint/pull/658))

### Fixed
- `.git` directory now discovered instead of hardcoded ([#623](https://github.com/pinterest/ktlint/pull/623))
- Several bugs with the experimental annotation rule ([#628](https://github.com/pinterest/ktlint/pull/628)) ([#642](https://github.com/pinterest/ktlint/pull/642)) ([#654](https://github.com/pinterest/ktlint/pull/654)) ([#624](https://github.com/pinterest/ktlint/pull/624))
- Allow newline after lambda return type ([#643](https://github.com/pinterest/ktlint/pull/643))
- Allow empty first line in a function that returns an anonymous object ([#655](https://github.com/pinterest/ktlint/pull/655))
- Indentation with lambda argument ([#627](https://github.com/pinterest/ktlint/pull/627))
- ktlint can now lint UTF-8 files with BOM ([#630](https://github.com/pinterest/ktlint/pull/630)
- Indentation with newline before return type ([#663](https://github.com/pinterest/ktlint/pull/663))
- Build/tests on Windows ([#640](https://github.com/pinterest/ktlint/pull/640))
- Allow whitespace after `(` followed by a comment ([#664](https://github.com/pinterest/ktlint/pull/664))

## [0.35.0] - 2019-10-12

### Added
- Support for specifying color for output via `--color-name` command line flag. ([#585](https://github.com/pinterest/ktlint/pull/585))
- Support for custom rulesets and providers on Java 9+ ([#573](https://github.com/pinterest/ktlint/pull/573))

### Deprecated
- `--apply-to-idea` flag; use `applyToIDEA` subcommand instead ([#554](https://github.com/pinterest/ktlint/pull/554))
- `--apply-to-idea-project` flag; use `applyToIDEAProject` subcommand instead ([#593](https://github.com/pinterest/ktlint/pull/593))
- `0.0.0-SNAPSHOT` builds; snapshot builds are now versioned, e.g. 0.35.0-SNAPSHOT ([#588](https://github.com/pinterest/ktlint/pull/588))
  - Note: When using the new snapshot builds, you may need to add an explicit dependency on `kotlin-compiler-embeddable` to your ruleset project.

### Removed
- Support for loading 3rd party rulesets via Maven ([#566](https://github.com/pinterest/ktlint/pull/566))

### Upgraded
- Kotlin version to 1.3.50 ([#565](https://github.com/pinterest/ktlint/pull/565)) ([#611](https://github.com/pinterest/ktlint/pull/611))

### Fixed
- Bugs with spacing in experimental `AnnotationRule` ([#552](https://github.com/pinterest/ktlint/pull/552)) ([#601](https://github.com/pinterest/ktlint/pull/601/)
- Brackets would be removed from empty companion object ([#600](https://github.com/pinterest/ktlint/pull/600/))
- Bugs with experimental `IndentationRule` ([#597](https://github.com/pinterest/ktlint/pull/597/)) ([#599](https://github.com/pinterest/ktlint/pull/599/))
- Erroneous space between `}` and `]` ([#596](https://github.com/pinterest/ktlint/pull/596))
- Spacing around multiplication sign in lambdas ([#598](https://github.com/pinterest/ktlint/pull/598))
- `--version` output with gradle-built JAR ([#613](https://github.com/pinterest/ktlint/issues/613))

## [0.34.2] - 2019-07-22

Minor bugfix release for 0.34.0. (Note: 0.34.1 deprecated/deleted due to regression in disabled_flags .editorconfig support.)

### Added
- Support for globally disabling rules via `--disabled_rules` command line flag. ([#534](https://github.com/pinterest/ktlint/pull/534))

### Fixed
- Regression with `--stdin` flag for `printAST` command ([#528](https://github.com/pinterest/ktlint/issues/528))
- Regressions with `NoUnusedImports` rule ([#531](https://github.com/pinterest/ktlint/issues/531), [#526](https://github.com/pinterest/ktlint/issues/526))
  - Note: this re-introduces [#405](https://github.com/pinterest/ktlint/issues/405)
- Indentation for enums with multi-line initializers ([#518](https://github.com/pinterest/ktlint/issues/518))

## [0.34.0] - 2019-07-15

### Added
- Support for Kotlin 1.3.41
- Support for globally disabling rules via custom `disabled_rules` property in `.editorconfig` ([#503](https://github.com/pinterest/ktlint/pull/503))
- `experimental:no-empty-first-line-in-method-block` ([#474](https://github.com/pinterest/ktlint/pull/474))
- Unit tests for ruleset providers

### Upgraded
- AssertJ from 3.9.0 to 3.12.2 ([#520](https://github.com/pinterest/ktlint/pull/520)) 

### Enabled
- Final newline by default ([#446](https://github.com/pinterest/ktlint/pull/446))
- `no-wildcard-import` (Re-enabled after temporarily disabling in 0.33.0)
- `experimental:annotation` ([#509](https://github.com/pinterest/ktlint/pull/509))
- `experimental:multiline-if-else` (no autocorrection)
- `experimental:package-name` (currently only disallows underscores in package names)

### Deprecated
- `MavenDependencyResolver`. Scheduled to be removed in 0.35.0 ([#468](https://github.com/pinterest/ktlint/pull/468))
- `--install-git-pre-commit-hook` flag; use `installGitPreCommitHook` subcommand instead ([#487](https://github.com/pinterest/ktlint/pull/487))
- `--print-ast` flag; use `printAST` subcommand instead ([#500](https://github.com/pinterest/ktlint/pull/500))

### Removed
- Support for `--ruleset-repository` and `--ruleset-update` flags

### Fixed
- `import-ordering` will now refuse to format import lists that contain top-level comments ([#408](https://github.com/pinterest/ktlint/issues/408))
- `no-unused-imports` reporting false negatives or false positives in some cases ([#405](https://github.com/pinterest/ktlint/issues/405)) and ([#506](https://github.com/pinterest/ktlint/issues/506))
- `experimental:indent` incorrectly formatting a lambda's closing brace ([#479](https://github.com/pinterest/ktlint/issues/479))

## [0.33.0] - 2019-05-28

### Added
- Support for Kotlin 1.3.31

### Disabled
- No wildcard imports rule ([#48](https://github.com/pinterest/ktlint/issues/48)). Developers wishing to still enforce this rule should add the code into a custom ruleset. 

### Fixed
- Spec file parsing is now platform-agnostic ([#365](https://github.com/pinterest/ktlint/pull/365))
- Unnecessary newline after `->` in some cases ([#403](https://github.com/pinterest/ktlint/pull/403))
- `SpacingAroundCommaRule` will no longer move code into comments
- Made newlines after `=` less aggressive ([#368](https://github.com/pinterest/ktlint/issues/368)) ([#380](https://github.com/pinterest/ktlint/issues/380))
- Erroneous newline when parameter comments are used ([#433](https://github.com/pinterest/ktlint/issues/433))

## [0.32.0] - 2019-04-22

### Added
- `experimental/import-ordering` rule ([#189](https://github.com/pinterest/ktlint/issues/189)).
  Use `ktlint --experimental` to enabled.
- Support for Kotlin 1.3.30
- Build now compatible with jitpack
- `ktlint` now part of Homebrew core (`shyiko/ktlint` tap deprecated)

### Fixed
- Incorrectly flagging a missing newline for functions with no parameters ([#327](https://github.com/pinterest/ktlint/issues/327)).
- Semicolons now allowed in KDocs ([#362](https://github.com/pinterest/ktlint/issues/362)).
- Spaces now disallowed after `super` ([#369](https://github.com/pinterest/ktlint/issues/369)).
- Annotations in function parameters now checked for indentation ([#374](https://github.com/pinterest/ktlint/issues/374)]

### Changed
- Code now lives in `com.pinterest` package
- groupId now `com.pinterest`
- Custom ruleset `META-INF.services` file must be renamed to `com.pinterest.ktlint.core.RuleSetProvider`

## [0.31.0] - 2019-03-10

### Added
- `dot-spacing` rule ([#293](https://github.com/shyiko/ktlint/issues/293)).
- `experimental/indent` rule ([#338](https://github.com/shyiko/ktlint/issues/338)).  
  Use `ktlint --experimental` to enable. 

### Fixed
- Spacing check around `<` & `>` operators.

### Changed
- `no-multi-spaces` rule (horizontal alignment of comments is no longer allowed) ([#269](https://github.com/shyiko/ktlint/issues/269)).
- `colon-spacing` rule (`:` must not appear at the beginning of the line).
- `package-name` rule (disabled until [#208](https://github.com/shyiko/ktlint/issues/208) is resolved).
- `--print-ast` to output [com.pinterest.ktlint.core.ast.ElementType.*](https://github.com/shyiko/ktlint/blob/master/ktlint-core/src/main/kotlin/com/github/shyiko/ktlint/core/ast/ElementType.kt) as `node.elementType`, e.g. 
```
$ echo 'fun f() {}' | ./ktlint/target/ktlint --print-ast --color --stdin
1: ~.psi.KtFile (FILE)
1:   ~.psi.KtPackageDirective (PACKAGE_DIRECTIVE) ""
1:   ~.psi.KtImportList (IMPORT_LIST) ""
1:   ~.psi.KtScript (SCRIPT)
1:     ~.psi.KtBlockExpression (BLOCK)
1:       ~.psi.KtNamedFunction (FUN)
1:         ~.c.i.p.impl.source.tree.LeafPsiElement (FUN_KEYWORD) "fun"
1:         ~.c.i.p.impl.source.tree.PsiWhiteSpaceImpl (WHITE_SPACE) " "
1:         ~.c.i.p.impl.source.tree.LeafPsiElement (IDENTIFIER) "f"
1:         ~.psi.KtParameterList (VALUE_PARAMETER_LIST)
1:           ~.c.i.p.impl.source.tree.LeafPsiElement (LPAR) "("
1:           ~.c.i.p.impl.source.tree.LeafPsiElement (RPAR) ")"
1:         ~.c.i.p.impl.source.tree.PsiWhiteSpaceImpl (WHITE_SPACE) " "
1:         ~.psi.KtBlockExpression (BLOCK)
1:           ~.c.i.p.impl.source.tree.LeafPsiElement (LBRACE) "{"
1:           ~.c.i.p.impl.source.tree.LeafPsiElement (RBRACE) "}"
1:       ~.c.i.p.impl.source.tree.PsiWhiteSpaceImpl (WHITE_SPACE) "\n"

   format: <line_number:> <node.psi::class> (<node.elementType>) "<node.text>"
   legend: ~ = org.jetbrains.kotlin, c.i.p = com.intellij.psi
```

- `kotlin-compiler` version to 1.3.21 (from 1.3.20).

### Removed 
- Dependency on JCenter ([#349](https://github.com/shyiko/ktlint/issues/349)).

## [0.30.0] - 2019-02-03

### Fixed
- `Missing newline before ")"` ([#327](https://github.com/shyiko/ktlint/issues/327)).

### Changed
- `kotlin-compiler` version to 1.3.20 (from 1.2.71) ([#331](https://github.com/shyiko/ktlint/issues/331)).

### Security
- `--ruleset`/`--reporter` switched to HTTPS ([#332](https://github.com/shyiko/ktlint/issues/332)).

## [0.29.0] - 2018-10-02

### Fixed
- `no-semi` rule to preserve semicolon after `companion object;` (see [#281](https://github.com/shyiko/ktlint/issues/281) for details).
- "line number off by one" when `end_of_line=CRLF` is used ([#286](https://github.com/shyiko/ktlint/issues/286)).

### Changed
- `package-name` rule not to check file location (until [#280](https://github.com/shyiko/ktlint/issues/280) can be properly addressed).
- `comment-spacing` rule not to flag `//region` & `//endregion` comments ([#278](https://github.com/shyiko/ktlint/issues/278)).
- `kotlin-compiler` version to 1.2.71 (from 1.2.51).

## [0.28.0] - 2018-09-05

### Fixed
- ktlint hanging in case of unhandled exception in a reporter ([#277](https://github.com/shyiko/ktlint/issues/277)).

### Changed
- `package-name` rule (directories containing `.` in their names are no longer considered to be invalid) ([#276](https://github.com/shyiko/ktlint/issues/276)).

## [0.27.0] - 2018-08-06

### Changed
- ktlint output (report location is now printed only if there are style violations) ([#267](https://github.com/shyiko/ktlint/issues/267)).

## [0.26.0] - 2018-07-30

### Changed
- `max-line-length` rule (multi-line strings are no longer checked) ([#262](https://github.com/shyiko/ktlint/issues/262)).

## [0.25.1] - 2018-07-25

### Fixed
- `json` reporter \ and control characters escaping ([#256](https://github.com/shyiko/ktlint/issues/256)).

## [0.25.0] - 2018-07-25

### Added
- `package-name` rule ([#246](https://github.com/shyiko/ktlint/pull/246)).
- `--editorconfig=path/to/.editorconfig` ([#250](https://github.com/shyiko/ktlint/pull/250)).
- Support for `end_of_line=native` (`.editorconfig`) ([#225](https://github.com/shyiko/ktlint/issues/225)).
- `tab -> space * indent_size` auto-correction (`--format`/`-F`).

### Fixed
- "Unnecessary semicolon" false positive ([#255](https://github.com/shyiko/ktlint/issues/255)).
- `(cannot be auto-corrected)` reporting.
- OOM in `--debug` mode while trying to print `root=true <- root=false` `.editorconfig` chain.

### Changed
- `kotlin-compiler` version to 1.2.51 (from 1.2.50).

## [0.24.0] - 2018-06-22

### Added 
- `paren-spacing` rule ([#223](https://github.com/shyiko/ktlint/issues/223)).
- Report location output ([#218](https://github.com/shyiko/ktlint/issues/218), [#224](https://github.com/shyiko/ktlint/issues/224)).
- An indication that some lint errors cannot be auto-corrected ([#219](https://github.com/shyiko/ktlint/issues/219)).
- Git hook to automatically check files for style violations on push (an alternative to existing `ktlint --install-git-pre-commit-hook`)  
(execute `ktlint --install-git-pre-push-hook` to install) ([#229](https://github.com/shyiko/ktlint/pull/229)).
- Support for `end_of_line=crlf` (`.editorconfig`) ([#225](https://github.com/shyiko/ktlint/issues/225)).

### Fixed
- `.editorconfig` path resolution  
(you no longer need to be inside project directory for `.editorconfig` to be loaded) ([#207](https://github.com/shyiko/ktlint/pull/207)).
- NPE in case of I/O error ([klob@0.2.1](https://github.com/shyiko/klob/blob/master/CHANGELOG.md#020---2017-07-21)).

### Changed
- `comment-spacing` rule to exclude `//noinspection` ([#212](https://github.com/shyiko/ktlint/pull/212)).
- `kotlin-compiler` version to 1.2.50 (from 1.2.41) ([#226](https://github.com/shyiko/ktlint/issues/226)).

## [0.23.1] - 2018-05-04

### Fixed
- `ClassCastException: cannot be cast to LeafPsiElement` ([#205](https://github.com/shyiko/ktlint/issues/205)).

## [0.23.0] - 2018-05-02

### Added
- `comment-spacing` ([#198](https://github.com/shyiko/ktlint/pull/198)),  
  `filename` ([#194](https://github.com/shyiko/ktlint/pull/194)) rules.
- `parameter-list-wrapping` left parenthesis placement check ([#201](https://github.com/shyiko/ktlint/pull/201)).
- `parameter-list-wrapping` auto-correction when `max_line_length` is exceeded ([#200](https://github.com/shyiko/ktlint/pull/200)).

### Fixed
- "Unused import" false positive (x.y.zNNN import inside x.y.z package) ([#204](https://github.com/shyiko/ktlint/issues/204)).

### Changed
- `kotlin-compiler` version to 1.2.41 (from 1.2.40).

## [0.22.0] - 2018-04-22

### Added
- `--apply-to-idea-project` (as an alternative to (global) `--apply-to-idea`) ([#178](https://github.com/shyiko/ktlint/issues/178)).
- Check to verify that annotations are placed before the modifiers ([#183](https://github.com/shyiko/ktlint/pull/183)).
- Access to PsiFile location information ([#194](https://github.com/shyiko/ktlint/pull/194)).

### Fixed
- `--format` commenting out operators (`chain-wrapping` rule) ([#193](https://github.com/shyiko/ktlint/pull/193)).

### Changed
- `indent` rule (`continuation_indent_size` is now ignored) ([#171](https://github.com/shyiko/ktlint/issues/171)).  
NOTE: if you have a custom `continuation_indent_size` (and `gcd(indent_size, continuation_indent_size) == 1`) ktlint
won't check the indentation.
- `--apply-to-idea` to inherit "Predefined style / Kotlin style guide" (Kotlin plugin 1.2.20+).
- `kotlin-compiler` version to 1.2.40 (from 1.2.30).

## [0.21.0] - 2018-03-29

### Changed
- `indent` rule to ignore `where <type constraint list>` clause ([#180](https://github.com/shyiko/ktlint/issues/180)).

## [0.20.0] - 2018-03-20

### Added
- Ability to load 3rd party reporters from the command-line (e.g. `--reporter=<name>,artifact=<group_id>:<artifact_id>:<version>`) ([#176](https://github.com/shyiko/ktlint/issues/176)).
- `--ruleset`/`--reporter` dependency tree validation.

### Fixed
- Handling of spaces in `--reporter=...,output=<path_to_a_file>` ([#177](https://github.com/shyiko/ktlint/issues/177)).
- `+`, `-`, `*`, `/`, `%`, `&&`, `||` wrapping ([#168](https://github.com/shyiko/ktlint/issues/168)).

### Changed
- `comma-spacing` rule to be more strict ([#173](https://github.com/shyiko/ktlint/issues/173)).
- `no-line-break-after-else` rule to allow multi-line `if/else` without curly braces. 

## [0.19.0] - 2018-03-04

### Changed
- Lambda formatting: if lambda is assigned a label, there should be no space between the label and the opening curly brace ([#167](https://github.com/shyiko/ktlint/issues/167)).  

## [0.18.0] - 2018-03-01

### Added
- Java 9 support ([#152](https://github.com/shyiko/ktlint/issues/152)).

### Changed
- `kotlin-compiler` version to 1.2.30 (from 1.2.20).

## [0.17.1] - 2018-02-28

### Fixed
- `Internal Error (parameter-list-wrapping)` when `indent_size=unset` ([#165](https://github.com/shyiko/ktlint/issues/165)). 

## [0.17.0] - 2018-02-28

### Fixed
- `+`/`-` wrapping inside `catch` block, after `else` and `if (..)` ([#160](https://github.com/shyiko/ktlint/issues/160)). 
- Multi-line parameter declaration indentation ([#161](https://github.com/shyiko/ktlint/issues/161)).
- Expected indentation reported by `indent` rule.

### Changed
- Error code returned by `ktlint --format/-F` when some of the errors cannot be auto-corrected (previously it was 0 instead of expected 1) ([#162](https://github.com/shyiko/ktlint/issues/162)). 

## [0.16.1] - 2018-02-27

### Fixed
- Handling of negative number condition in `when` block ([#160](https://github.com/shyiko/ktlint/issues/160)). 

## [0.16.0] - 2018-02-27

### Added
- `parameter-list-wrapping` rule ([#130](https://github.com/shyiko/ktlint/issues/130)).
- `+`, `-`, `*`, `/`, `%`, `&&`, `||` wrapping check (now part of `chain-wrapping` rule).

### Fixed
- Unused `componentN` import (where N > 5) false positive ([#142](https://github.com/shyiko/ktlint/issues/142)).
- max-line-length error suppression ([#158](https://github.com/shyiko/ktlint/issues/158)). 

### Changed
- `modifier-order` rule to match official [Kotlin Coding Conventions](https://kotlinlang.org/docs/reference/coding-conventions.html#modifiers) ([#146](https://github.com/shyiko/ktlint/issues/146))  
(`override` modifier should be placed before `suspend`/`tailrec`, not after) 

## [0.15.1] - 2018-02-14

### Fixed
- Race condition when multiple rules try to modify AST node that gets detached as a result of mutation ([#154](https://github.com/shyiko/ktlint/issues/154)).

## [0.15.0] - 2018-01-18

### Added
- `no-line-break-after-else` rule ([#125](https://github.com/shyiko/ktlint/issues/125)).

### Changed
- `kotlin-compiler` version to 1.2.20 (from 1.2.0).

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

[0.38.1]: https://github.com/pinterest/ktlint/compare/0.38.0...0.38.1
[0.38.0]: https://github.com/pinterest/ktlint/compare/0.37.2...0.38.0
[0.37.2]: https://github.com/pinterest/ktlint/compare/0.37.1...0.37.2
[0.37.1]: https://github.com/pinterest/ktlint/compare/0.37.0...0.37.1
[0.37.0]: https://github.com/pinterest/ktlint/compare/0.36.0...0.37.0
[0.36.0]: https://github.com/pinterest/ktlint/compare/0.35.0...0.36.0
[0.35.0]: https://github.com/pinterest/ktlint/compare/0.34.2...0.35.0
[0.34.2]: https://github.com/pinterest/ktlint/compare/0.33.0...0.34.2
[0.34.0]: https://github.com/pinterest/ktlint/compare/0.33.0...0.34.0
[0.33.0]: https://github.com/pinterest/ktlint/compare/0.32.0...0.33.0
[0.32.0]: https://github.com/pinterest/ktlint/compare/0.31.0...0.32.0
[0.31.0]: https://github.com/pinterest/ktlint/compare/0.30.0...0.31.0
[0.30.0]: https://github.com/pinterest/ktlint/compare/0.29.0...0.30.0
[0.29.0]: https://github.com/pinterest/ktlint/compare/0.28.0...0.29.0
[0.28.0]: https://github.com/pinterest/ktlint/compare/0.27.0...0.28.0
[0.27.0]: https://github.com/pinterest/ktlint/compare/0.26.0...0.27.0
[0.26.0]: https://github.com/pinterest/ktlint/compare/0.25.1...0.26.0
[0.25.1]: https://github.com/pinterest/ktlint/compare/0.25.0...0.25.1
[0.25.0]: https://github.com/pinterest/ktlint/compare/0.24.0...0.25.0
[0.24.0]: https://github.com/pinterest/ktlint/compare/0.23.1...0.24.0
[0.23.1]: https://github.com/pinterest/ktlint/compare/0.23.0...0.23.1
[0.23.0]: https://github.com/pinterest/ktlint/compare/0.22.0...0.23.0
[0.22.0]: https://github.com/pinterest/ktlint/compare/0.21.0...0.22.0
[0.21.0]: https://github.com/pinterest/ktlint/compare/0.20.0...0.21.0
[0.20.0]: https://github.com/pinterest/ktlint/compare/0.19.0...0.20.0
[0.19.0]: https://github.com/pinterest/ktlint/compare/0.18.0...0.19.0
[0.18.0]: https://github.com/pinterest/ktlint/compare/0.17.1...0.18.0
[0.17.1]: https://github.com/pinterest/ktlint/compare/0.17.0...0.17.1
[0.17.0]: https://github.com/pinterest/ktlint/compare/0.16.1...0.17.0
[0.16.1]: https://github.com/pinterest/ktlint/compare/0.16.0...0.16.1
[0.16.0]: https://github.com/pinterest/ktlint/compare/0.15.1...0.16.0
[0.15.1]: https://github.com/pinterest/ktlint/compare/0.15.0...0.15.1
[0.15.0]: https://github.com/pinterest/ktlint/compare/0.14.0...0.15.0
[0.14.0]: https://github.com/pinterest/ktlint/compare/0.13.0...0.14.0
[0.13.0]: https://github.com/pinterest/ktlint/compare/0.12.1...0.13.0
[0.12.1]: https://github.com/pinterest/ktlint/compare/0.12.0...0.12.1
[0.12.0]: https://github.com/pinterest/ktlint/compare/0.11.1...0.12.0
[0.11.1]: https://github.com/pinterest/ktlint/compare/0.11.0...0.11.1
[0.11.0]: https://github.com/pinterest/ktlint/compare/0.10.1...0.11.0
[0.10.2]: https://github.com/pinterest/ktlint/compare/0.10.1...0.10.2
[0.10.1]: https://github.com/pinterest/ktlint/compare/0.10.0...0.10.1
[0.10.0]: https://github.com/pinterest/ktlint/compare/0.9.2...0.10.0
[0.9.2]: https://github.com/pinterest/ktlint/compare/0.9.1...0.9.2
[0.9.1]: https://github.com/pinterest/ktlint/compare/0.9.0...0.9.1
[0.9.0]: https://github.com/pinterest/ktlint/compare/0.8.3...0.9.0
[0.8.3]: https://github.com/pinterest/ktlint/compare/0.8.2...0.8.3
[0.8.2]: https://github.com/pinterest/ktlint/compare/0.8.1...0.8.2
[0.8.1]: https://github.com/pinterest/ktlint/compare/0.8.0...0.8.1
[0.8.0]: https://github.com/pinterest/ktlint/compare/0.7.1...0.8.0
[0.7.1]: https://github.com/pinterest/ktlint/compare/0.7.0...0.7.1
[0.7.0]: https://github.com/pinterest/ktlint/compare/0.6.2...0.7.0
[0.6.2]: https://github.com/pinterest/ktlint/compare/0.6.1...0.6.2
[0.6.1]: https://github.com/pinterest/ktlint/compare/0.6.0...0.6.1
[0.6.0]: https://github.com/pinterest/ktlint/compare/0.5.1...0.6.0
[0.5.1]: https://github.com/pinterest/ktlint/compare/0.5.0...0.5.1
[0.5.0]: https://github.com/pinterest/ktlint/compare/0.4.0...0.5.0
[0.4.0]: https://github.com/pinterest/ktlint/compare/0.3.1...0.4.0
[0.3.1]: https://github.com/pinterest/ktlint/compare/0.3.0...0.3.1
[0.3.0]: https://github.com/pinterest/ktlint/compare/0.2.2...0.3.0
[0.2.2]: https://github.com/pinterest/ktlint/compare/0.2.1...0.2.2
[0.2.1]: https://github.com/pinterest/ktlint/compare/0.2.0...0.2.1
[0.2.0]: https://github.com/pinterest/ktlint/compare/0.1.2...0.2.0
[0.1.2]: https://github.com/pinterest/ktlint/compare/0.1.1...0.1.2
[0.1.1]: https://github.com/pinterest/ktlint/compare/0.1.0...0.1.1
