package com.pinterest.ktlint.ruleset.experimental

import com.pinterest.ktlint.core.LintError
import com.pinterest.ktlint.test.format
import com.pinterest.ktlint.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class FunctionReturnTypeSpacingRuleTest {
    @Test
    fun `Given a function signature without whitespace between the closing parenthesis and the colon of the return type then do not reformat`() {
        val code =
            """
            fun foo(): String = "some-result"
            """.trimIndent()
        assertThat(FunctionReturnTypeSpacingRule().lint(code)).isEmpty()
        assertThat(FunctionReturnTypeSpacingRule().format(code)).isEqualTo(code)
    }

    @Test
    fun `Given a function signature with at least one space between the closing parenthesis and the colon of the return type then reformat`() {
        val code =
            """
            fun foo() : String = "some-result"
            """.trimIndent()
        val formattedCode =
            """
            fun foo(): String = "some-result"
            """.trimIndent()
        assertThat(FunctionReturnTypeSpacingRule().lint(code)).containsExactly(
            LintError(1, 10, "function-return-type-spacing", "Unexpected whitespace")
        )
        assertThat(FunctionReturnTypeSpacingRule().format(code)).isEqualTo(formattedCode)
    }

    @Test
    fun `Given a function signature with a newline between the closing parenthesis and the colon of the return type then reformat`() {
        val code =
            """
            fun foo()
                : String = "some-result"
            """.trimIndent()
        val formattedCode =
            """
            fun foo(): String = "some-result"
            """.trimIndent()
        assertThat(FunctionReturnTypeSpacingRule().lint(code)).containsExactly(
            LintError(1, 10, "function-return-type-spacing", "Unexpected whitespace")
        )
        assertThat(FunctionReturnTypeSpacingRule().format(code)).isEqualTo(formattedCode)
    }

    @Test
    fun `Given a function signature without space between the colon and the return type then reformat`() {
        val code =
            """
            fun foo():String = "some-result"
            """.trimIndent()
        val formattedCode =
            """
            fun foo(): String = "some-result"
            """.trimIndent()
        assertThat(FunctionReturnTypeSpacingRule().lint(code)).containsExactly(
            LintError(1, 10, "function-return-type-spacing", "Single space expected between colon and return type")
        )
        assertThat(FunctionReturnTypeSpacingRule().format(code)).isEqualTo(formattedCode)
    }

    @Test
    fun `Given a function signature with multiple spaces between the colon and the return type then reformat`() {
        val code =
            """
            fun foo():  String = "some-result"
            """.trimIndent()
        val formattedCode =
            """
            fun foo(): String = "some-result"
            """.trimIndent()
        assertThat(FunctionReturnTypeSpacingRule().lint(code)).containsExactly(
            LintError(1, 10, "function-return-type-spacing", "Unexpected whitespace")
        )
        assertThat(FunctionReturnTypeSpacingRule().format(code)).isEqualTo(formattedCode)
    }

    @Test
    fun `Given a function signature with a new line between the colon and the return type then reformat`() {
        val code =
            """
            fun foo():
                String = "some-result"
            """.trimIndent()
        val formattedCode =
            """
            fun foo(): String = "some-result"
            """.trimIndent()
        assertThat(FunctionReturnTypeSpacingRule().lint(code)).containsExactly(
            LintError(1, 10, "function-return-type-spacing", "Unexpected whitespace")
        )
        assertThat(FunctionReturnTypeSpacingRule().format(code)).isEqualTo(formattedCode)
    }
}
