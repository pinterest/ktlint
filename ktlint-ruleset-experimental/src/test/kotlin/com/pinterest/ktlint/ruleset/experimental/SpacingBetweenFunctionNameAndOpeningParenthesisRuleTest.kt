package com.pinterest.ktlint.ruleset.experimental

import com.pinterest.ktlint.test.KtLintAssertThat
import org.junit.jupiter.api.Test

class SpacingBetweenFunctionNameAndOpeningParenthesisRuleTest {
    private val spacingBetweenFunctionNameAndOpeningParenthesisRuleAssertThat =
        KtLintAssertThat.assertThatRule { SpacingBetweenFunctionNameAndOpeningParenthesisRule() }

    @Test
    fun `Given a function signature without whitespace between function name and opening parenthesis then do not reformat`() {
        val code =
            """
            fun foo() = "foo"
            """.trimIndent()
        spacingBetweenFunctionNameAndOpeningParenthesisRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Given a function signature with one or more spaces between function name and opening parenthesis then do not reformat`() {
        val code =
            """
            fun foo () = "foo"
            """.trimIndent()
        val formattedCode =
            """
            fun foo() = "foo"
            """.trimIndent()
        spacingBetweenFunctionNameAndOpeningParenthesisRuleAssertThat(code)
            .hasLintViolation(1, 8, "Unexpected whitespace")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a function signature with one or more new lines between function name and opening parenthesis then do not reformat`() {
        val code =
            """
            fun foo
            () = "foo"
            """.trimIndent()
        val formattedCode =
            """
            fun foo() = "foo"
            """.trimIndent()
        spacingBetweenFunctionNameAndOpeningParenthesisRuleAssertThat(code)
            .hasLintViolation(1, 8, "Unexpected whitespace")
            .isFormattedAs(formattedCode)
    }
}
