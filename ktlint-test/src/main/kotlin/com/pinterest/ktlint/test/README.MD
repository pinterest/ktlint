<h1>Testing KtLint rules</h1>

The `ktlint-test` module provides an API for testing KtLint rules. It is meant as public (and internal) API.

The most important API is the KtLintAssertThat which is an extension on top of AssertJ. It allows to test `lint` and `format` in a fluent way. All rules provided by KtLint <b>must</b> use the `KtLintAssertThat` style as described below.

<h2>Setup</h2>
It is advised to create an assertThat lambda for a specific KtLint rule once per test class.

```kotlin
val someRuleAssertThat = SomeRule().assertThat()
```

<h2>Validate the absence of lint errors</h2>

When no lint error is expected, the style below has to be used:

```kotlin
@Test
fun `Some test`() {
    val code =
        """
        ...Original code...
        """.trimIndent()
    someRuleAssertThat(code).hasNoLintViolations()
}
```

Code above executes `lint` to verify that no lint error occurs. Also `format` is executed to verify that no code changes have been applied.

<h2>Validate the existing of a single lint error</h2>

In case exactly one lint error is expected, the style below is to be used.

```kotlin
@Test
fun `Some test`() {
    val code =
        """
        ...Original code to be formatted...
        """.trimIndent()
    val formattedCode =
        """
        ...The code as it should be formatted...
        """.trimIndent()
    someRuleAssertThat(code)
        .hasLintViolation(1, 1, "some-error-description")
         .isFormattedAs(formattedCode)
}
```
Note that the rule-id is not specified (it will be derived from the rule associated with the AssertThat lambda). Tests for native KtLint rules always should verify the lint errors *and* the formatted output.

<h2>Validate the existing of multiple lint errors</h2>

When multiple lint errors are expected, then use the style below:

```kotlin
@Test
fun `Some test`() {
    val code =
        """
        ...Original code to be formatted...
        """.trimIndent()
    val formattedCode =
        """
        ...The code as it should be formatted...
        """.trimIndent()
    someRuleAssertThat(code)
        .hasLintViolations(
            LintViolation(1, 1, "some-error-description"),
            LintViolation(3, 6, "some-other-error-description")
        ).isFormattedAs(formattedCode)
}
```
Note that the rule-id is not specified (it will be derived from the rule associated with the AssertThat lambda). Tests for native KtLint rules always should verify the lint errors *and* the formatted output.

<h2>Lint errors which can not be automatically corrected</h2>

In case that a lint error can not be automatically detected, use `hasLintViolationWithoutAutoCorrect` or `hasLintViolationsWithoutAutoCorrect` instead. It is not possible to mix lint errors which can corrected automatically with lint errors which can not be corrected automatically.

<h2>Customizing the AssertThat lambda</h2>

The AssertThat lambda can be customized per test on aspects belows:

* Apply additional rules during format
* Set `.editorconfig` properties
* Set `.editorconfig` property for `max_line_length`
* Set file path
* Run as Kotlin Script

<h3>Apply additional rules during format</h3>

Each rule is to be focussed on a single task. As a result the formatted code after applying a single rule can look distorted whenever that rules depends on another rule to complete the formatting. In such cases it is possible to run additional rules during the `format` phase only. Note that `lint` errors of those additional rules are suppressed and can not be verified.

If the `ArgumentListWrappingRule` is run on code below

```kotlin
val x = test(
    one("a", "b",
    "c"),
    "Two"
)
```
it results in:
```kotlin
val x = test(
    one("a",
"b",
    "c"),
    "Two"
)
```
Technically this is correct, but it is very distracting when reading the unit test. Using the `addAdditionalFormattingRule`, test test can be written as:

```kotlin
        val code =
            """
            val x = test(
                one("a", "b",
                "c"),
                "Two"
            )
            """.trimIndent()
        val formattedCode =
            """
            val x = test(
                one(
                    "a",
                    "b",
                    "c"
                ),
                "Two"
            )
            """.trimIndent()
        argumentListWrappingRuleAssertThat(code)
            .addAdditionalFormattingRule(IndentationRule())
            .hasLintViolations(
              ...
            ).isFormattedAs(formattedCode)
```
Note that the Lint violations is test above only should contain violations caused by the `ArgumentListWrappingRule`.

<h3>Set `.editorconfig` property</h3>

Whenever the rule which is to be asserted takes `.editorconfig` properties into account, the behavior of the rule has to be tested given different values of each property. For this the styles below can be used:

```kotlin
trailingCommaRuleAssertThat(code)
    .withEditorConfigOverride(allowTrailingCommaOnCallSiteProperty to true)
    .withEditorConfigOverride(allowTrailingCommaProperty to true)
    .hasLintViolations(
      ...
    ).isFormattedAs(formattedCode)
```

or

```kotlin
trailingCommaRuleAssertThat(code)
    .withEditorConfigOverride(
        allowTrailingCommaOnCallSiteProperty to true,
        allowTrailingCommaProperty to true
    ).hasLintViolations(
      ...
    ).isFormattedAs(formattedCode)
```

See next section for special support regarding the `.editorconfig` property `max_line_length`.

<h3>Set `.editorconfig` property `max_line_length`</h3>

The `.editorconfig` property `max_line_length` can be set as any other property as described in section above. From a readability perspective it is better to use style below as it more cleary visualizes the end of the line with respect to the source code:

```kotlin
 val code =
     """
     // $MAX_LINE_LENGTH_MARKER                   $EOL_CHAR
     val fooooooooooooooo = "fooooooooooooooooooooo"
     """.trimIndent()
 maxLineLengthRuleAssertThat(code)
     .setMaxLineLength()
     .hasLintViolation(2, 1, "Exceeded max line length (46)")
```

The line ends at the exact position of the `$` in `$EOL_CHAR`. If the property has been set before via the `withEditorConfigOverride` function then that value is silently overwritten.

<h3>Set file path</h3>

For tests that rely that code is stored in a file with a specific name, use the `asFileWithPath` as showed below:

```kotlin
val code =
    """
    class B {
        class C
        class D
    }
    """.trimIndent()
fileNameRuleAssertThat(code)
  .asFileWithPath("/some/path/A.kt")
  .hasLintViolationWithoutAutoCorrect(1, 1, "class B should be declared in a file named B.kt")
```

<h3>Run as Kotlin Script</h3>

Whenever the code sample contain Kotlin Script instead of Kotlin, then use `.asKotlinScript()` as shown below:

```kotlin
val code =
    """
    @file:Suppress("UnstableApiUsage")
    pluginManagement {
    }
    """.trimIndent()
val formattedCode =
    """
    @file:Suppress("UnstableApiUsage")

    pluginManagement {
    }
    """.trimIndent()
annotationRuleAssertThat(code)
    .asKotlinScript()
    .hasLintViolation(1, 34, "File annotations should be separated from file contents with a blank line")
    .isFormattedAs(formattedCode)
```
