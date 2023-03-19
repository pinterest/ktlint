package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.rule.engine.core.api.editorconfig.CODE_STYLE_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.CodeStyleValue
import com.pinterest.ktlint.test.KtLintAssertThat
import com.pinterest.ktlint.test.LintViolation
import org.junit.jupiter.api.Test

class MultilineExpressionWrappingTest {
    private val multilineExpressionWrappingAssertThat = KtLintAssertThat.assertThatRule { MultilineExpressionWrapping() }

    @Test
    fun `Given value argument in a function with a multiline dot qualified expression on the same line as the assignment`() {
        val code =
            """
            val foo = foo(
                parameterName = "The quick brown fox "
                    .plus("jumps ")
                    .plus("over the lazy dog"),
            )
            """.trimIndent()
        val formattedCode =
            """
            val foo =
                foo(
                    parameterName =
                        "The quick brown fox "
                            .plus("jumps ")
                            .plus("over the lazy dog"),
                )
            """.trimIndent()
        multilineExpressionWrappingAssertThat(code)
            .addAdditionalRuleProvider { IndentationRule() }
            .withEditorConfigOverride(CODE_STYLE_PROPERTY to CodeStyleValue.ktlint_official)
            .hasLintViolations(
                LintViolation(1, 11, "A multiline expression should start on a new line"),
                LintViolation(2, 21, "A multiline expression should start on a new line"),
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given value argument in a function with a multiline binary expression on the same line as the assignment`() {
        val code =
            """
            val foo = foo(
                parameterName = "The quick brown fox " +
                    "jumps " +
                    "over the lazy dog",
            )
            """.trimIndent()
        val formattedCode =
            """
            val foo =
                foo(
                    parameterName =
                        "The quick brown fox " +
                            "jumps " +
                            "over the lazy dog",
                )
            """.trimIndent()
        multilineExpressionWrappingAssertThat(code)
            .addAdditionalRuleProvider { IndentationRule() }
            .withEditorConfigOverride(CODE_STYLE_PROPERTY to CodeStyleValue.ktlint_official)
            .hasLintViolations(
                LintViolation(1, 11, "A multiline expression should start on a new line"),
                LintViolation(2, 21, "A multiline expression should start on a new line"),
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given value argument in a function with a multiline safe access expression on the same line as the assignment`() {
        val code =
            """
            val foo = foo(
                parameterName = theQuickBrownFoxOrNull
                    ?.plus("jumps ")
                    ?.plus("over the lazy dog"),
            )
            """.trimIndent()
        val formattedCode =
            """
            val foo =
                foo(
                    parameterName =
                        theQuickBrownFoxOrNull
                            ?.plus("jumps ")
                            ?.plus("over the lazy dog"),
                )
            """.trimIndent()
        multilineExpressionWrappingAssertThat(code)
            .addAdditionalRuleProvider { IndentationRule() }
            .withEditorConfigOverride(CODE_STYLE_PROPERTY to CodeStyleValue.ktlint_official)
            .hasLintViolations(
                LintViolation(1, 11, "A multiline expression should start on a new line"),
                LintViolation(2, 21, "A multiline expression should start on a new line"),
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given value argument in a function with a multiline combination of a safe access expression and a call expression on the same line as the assignment`() {
        val code =
            """
            val foo = foo(
                parameterName = theQuickBrownFoxOrNull()
                    ?.plus("jumps ")
                    ?.plus("over the lazy dog"),
            )
            """.trimIndent()
        val formattedCode =
            """
            val foo =
                foo(
                    parameterName =
                        theQuickBrownFoxOrNull()
                            ?.plus("jumps ")
                            ?.plus("over the lazy dog"),
                )
            """.trimIndent()
        multilineExpressionWrappingAssertThat(code)
            .addAdditionalRuleProvider { IndentationRule() }
            .withEditorConfigOverride(CODE_STYLE_PROPERTY to CodeStyleValue.ktlint_official)
            .hasLintViolations(
                LintViolation(1, 11, "A multiline expression should start on a new line"),
                LintViolation(2, 21, "A multiline expression should start on a new line"),
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given value argument in a function with a multiline combination of a dot qualified and a safe access expression on the same line as the assignment`() {
        val code =
            """
            val foo = foo(
                parameterName = "The quick brown fox "
                    .takeIf { it.jumps }
                    ?.plus("jumps over the lazy dog"),
            )
            """.trimIndent()
        val formattedCode =
            """
            val foo =
                foo(
                    parameterName =
                        "The quick brown fox "
                            .takeIf { it.jumps }
                            ?.plus("jumps over the lazy dog"),
                )
            """.trimIndent()
        multilineExpressionWrappingAssertThat(code)
            .addAdditionalRuleProvider { IndentationRule() }
            .withEditorConfigOverride(CODE_STYLE_PROPERTY to CodeStyleValue.ktlint_official)
            .hasLintViolations(
                LintViolation(1, 11, "A multiline expression should start on a new line"),
                LintViolation(2, 21, "A multiline expression should start on a new line"),
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given value argument in a function with a multiline call expression on the same line as the assignment`() {
        val code =
            """
            val foo = foo(
                parameterName = bar(
                    "bar"
                )
            )
            """.trimIndent()
        val formattedCode =
            """
            val foo =
                foo(
                    parameterName =
                        bar(
                            "bar"
                        )
                )
            """.trimIndent()
        multilineExpressionWrappingAssertThat(code)
            .addAdditionalRuleProvider { IndentationRule() }
            .withEditorConfigOverride(CODE_STYLE_PROPERTY to CodeStyleValue.ktlint_official)
            .hasLintViolations(
                LintViolation(1, 11, "A multiline expression should start on a new line"),
                LintViolation(2, 21, "A multiline expression should start on a new line"),
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a return statement with a multiline expression then do not reformat as it would result in a compilation error`() {
        val code =
            """
            fun foo() {
                return bar(
                    "bar"
                )
            }
            """.trimIndent()
        multilineExpressionWrappingAssertThat(code)
            .withEditorConfigOverride(CODE_STYLE_PROPERTY to CodeStyleValue.ktlint_official)
            .hasNoLintViolations()
    }

    @Test
    fun `Given a function with a multiline body expression`() {
        val code =
            """
            fun foo() = bar(
                "bar"
            )
            """.trimIndent()
        val formattedCode =
            """
            fun foo() =
                bar(
                    "bar"
                )
            """.trimIndent()
        multilineExpressionWrappingAssertThat(code)
            .addAdditionalRuleProvider { IndentationRule() }
            .withEditorConfigOverride(CODE_STYLE_PROPERTY to CodeStyleValue.ktlint_official)
            .hasLintViolation(1, 13, "A multiline expression should start on a new line")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a function with a multiline signature without a return type but with a multiline expression body starting on same line as closing parenthesis of function`() {
        val code =
            """
            fun foo(
                foobar: String
            ) = bar(
                foobar
            )
            """.trimIndent()
        multilineExpressionWrappingAssertThat(code)
            .addAdditionalRuleProvider { IndentationRule() }
            .withEditorConfigOverride(CODE_STYLE_PROPERTY to CodeStyleValue.ktlint_official)
            .hasNoLintViolations()
    }
}
