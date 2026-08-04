# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build and Development Commands

```bash
# Build all modules and run ktlint self-check
./gradlew ktlintCheck build

# Run all tests
./gradlew test

# Run a single module's tests
./gradlew :ktlint-ruleset-standard:test

# Run a specific test class
./gradlew :ktlint-ruleset-standard:test --tests "io.github.ktlint.core.ruleset.standard.rules.FinalNewlineRuleTest"

# Format all Kotlin sources
./gradlew ktlintFormat

# Run tests on specific JDK versions (CI runs 17 and 21)
./gradlew testOnJdk17
./gradlew testOnJdk21

# Enable trace logging during tests
KTLINT_UNIT_TEST_TRACE=on ./gradlew :ktlint-ruleset-standard:test
```

Build uses Gradle with Kotlin DSL. Convention plugins live in `build-logic/`. Dependencies are declared in `gradle/libs.versions.toml`. Kotlin API/language version is locked to 2.2; `explicitApi()` is enforced in all modules.

## Module Structure

| Module | Role |
|---|---|
| `ktlint-rule-engine-core` | Public API types: `RuleV2`, `RuleId`, `EditorConfigProperty`, `AutocorrectDecision` |
| `ktlint-rule-engine` | Core engine: `KtLintRuleEngine`, `CodeFormatter`, `EditorConfigLoader` |
| `ktlint-ruleset-standard` | ~100 built-in lint rules |
| `ktlint-cli-ruleset-core` | `RuleSetV2Provider` SPI base class (ServiceLoader entry point for custom rulesets) |
| `ktlint-cli` | CLI entry point, shadow JAR, uses Clikt for argument parsing |
| `ktlint-test` | Test utilities: `KtLintAssertThat` DSL, `KtlintTestFileSystem` |
| `ktlint-cli-reporter-*` | Seven reporter modules (plain, json, html, checkstyle, sarif, baseline, etc.) |
| `ktlint-bom` | Bill of Materials for consistent dependency versions |
| `ktlint-api-consumer` | Example of embedding ktlint as a library |

## Architecture

### Rule Definition

All current rules extend `RuleV2` from `ktlint-rule-engine-core`. Built-in standard rules extend `StandardRule`, which pre-fills `ruleId` with `standard:<id>` and sets `About` metadata:

```kotlin
class FinalNewlineRule : StandardRule(
    id = "final-newline",
    usesEditorConfigProperties = setOf(INSERT_FINAL_NEWLINE_PROPERTY),
)
```

Custom rules may extend `RuleV2` directly. The project itself only use rulesets with custom rules for testing purposes, as well as explaining how to build a custom ruleset. 

```kotlin
class NoVarRule : RuleV2(
    ruleId = RuleId("myruleset:no-var"),
    about = About(maintainer = "...", repositoryUrl = "...", issueTrackerUrl = "..."),
)
```

### Rule Lifecycle Hooks

Rules implement up to four hooks called by the `KtLintRuleEngine` per file:

1. `beforeFirstNode(editorConfig)` — read `.editorconfig` properties here
2. `beforeVisitChildNodes(node, emit)` — depth-first, before children
3. `afterVisitChildNodes(node, emit)` — after children
4. `afterLastNode()` — teardown

The `emit` lambda: `(offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision`. Rules inspect the returned value and call `.ifAutocorrectAllowed { }` to decide whether to modify the AST. Call `stopTraversalOfAST()` to skip remaining nodes.

### AST Traversal

The engine uses the Kotlin compiler's embedded IntelliJ PSI/AST (`org.jetbrains.kotlin.com.intellij.lang.ASTNode`). Element type constants are in `ElementType` (e.g. `ElementType.VAR_KEYWORD`). `ASTNodeExtension.kt` provides many helper extension functions.

### EditorConfig Integration

Rules declare consumed properties in `usesEditorConfigProperties`. The engine resolves them from the `.editorconfig` hierarchy and passes them via `beforeFirstNode(editorConfig)`. Rules read values with `editorConfig[MY_PROPERTY]`.

### Rule Discovery (Custom Rulesets)

Custom rulesets register via Java `ServiceLoader`. Create a file at:
```
META-INF/services/io.github.ktlint.core.cli.ruleset.core.api.RuleSetV2Provider
```
listing the fully-qualified `RuleSetV2Provider` subclass name.

### Thread Safety

`KtLintRuleEngine` is thread-safe. `RuleV2Provider` (a `() -> RuleV2` lambda) creates a fresh rule instance per file, so rule instances may hold mutable state safely.

### Format Loop

`CodeFormatter` runs format passes in a loop until output stabilizes (up to `maxFormatRunsPerFile`), allowing fixes from one rule to trigger another rule's fix. However, it is not guaranteed that all problems are fixed after that, especially not on a code base which has never been formatted before. Ktlint CLI clearly indicates this in its console output. 

### Rule Opt-in Markers

- `RuleV2.Experimental` — only runs when experimental rules are enabled
- `RuleV2.OfficialCodeStyle` — only runs with `code_style = ktlint_official`
- `RuleV2.OnlyWhenEnabledInEditorconfig` — opt-in rule, disabled by default
- `@SinceKtlint("1.2", STABLE)` — documents when a rule was added and its stability

## Testing Patterns

Tests use JUnit 5, AssertJ, and the custom `KtLintAssertThat` DSL from `ktlint-test`. Each rule in `ktlint-ruleset-standard` has a corresponding `*Test.kt`. Tests are organized with `@Nested inner class` using backtick-quoted descriptive names.

```kotlin
private val myRuleAssertThat = assertThatRule { MyRule() }

@Test
fun `description of scenario`() {
    val code = "val x = 1"
    myRuleAssertThat(code)
        .hasLintViolation(1, 1, "error message")
        .isFormattedAs("val x = 1\n")
    // or: .hasNoLintViolations()
    // or: .hasLintViolationWithoutAutoCorrect(2, 5, "error message")
}
```

Tests use an in-memory `KtlintTestFileSystem` (Jimfs) so the project's own `.editorconfig` does not affect test outcomes.

`@KtlintDocumentationTest` marks tests that also serve as documentation examples.
