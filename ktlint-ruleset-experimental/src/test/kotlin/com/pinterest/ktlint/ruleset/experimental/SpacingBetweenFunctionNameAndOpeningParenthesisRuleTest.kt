package com.pinterest.ktlint.ruleset.experimental

import com.pinterest.ktlint.core.LintError
import com.pinterest.ktlint.test.format
import com.pinterest.ktlint.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class SpacingBetweenFunctionNameAndOpeningParenthesisRuleTest {
    @Test
    fun `Given a function signature without whitespace between function name and opening parenthesis then do not reformat`() {
        val code =
            """
            fun foo() = "foo"
            """.trimIndent()
        assertThat(SpacingBetweenFunctionNameAndOpeningParenthesisRule().lint(code)).isEmpty()
        assertThat(SpacingBetweenFunctionNameAndOpeningParenthesisRule().format(code)).isEqualTo(code)
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
        assertThat(SpacingBetweenFunctionNameAndOpeningParenthesisRule().lint(code)).containsExactly(
            LintError(1, 8, "spacing-between-function-name-and-opening-parenthesis", "Unexpected whitespace")
        )
        assertThat(SpacingBetweenFunctionNameAndOpeningParenthesisRule().format(code)).isEqualTo(formattedCode)
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
        assertThat(SpacingBetweenFunctionNameAndOpeningParenthesisRule().lint(code)).containsExactly(
            LintError(1, 8, "spacing-between-function-name-and-opening-parenthesis", "Unexpected whitespace")
        )
        assertThat(SpacingBetweenFunctionNameAndOpeningParenthesisRule().format(code)).isEqualTo(formattedCode)
    }
}
