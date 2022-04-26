package com.pinterest.ktlint.ruleset.experimental

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThat
import org.junit.jupiter.api.Test

class FunctionReturnTypeSpacingRuleTest {
    private val functionReturnTypeSpacingRuleAssertThat = FunctionReturnTypeSpacingRule().assertThat()

    @Test
    fun `Given a function signature without whitespace between the closing parenthesis and the colon of the return type then do not reformat`() {
        val code =
            """
            fun foo(): String = "some-result"
            """.trimIndent()
        functionReturnTypeSpacingRuleAssertThat(code).hasNoLintViolations()
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
        functionReturnTypeSpacingRuleAssertThat(code)
            .hasLintViolation(1, 10, "Unexpected whitespace")
            .isFormattedAs(formattedCode)
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
        functionReturnTypeSpacingRuleAssertThat(code)
            .hasLintViolation(1, 10, "Unexpected whitespace")
            .isFormattedAs(formattedCode)
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
        functionReturnTypeSpacingRuleAssertThat(code)
            .hasLintViolation(1, 10, "Single space expected between colon and return type")
            .isFormattedAs(formattedCode)
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
        functionReturnTypeSpacingRuleAssertThat(code)
            .hasLintViolation(1, 10, "Unexpected whitespace")
            .isFormattedAs(formattedCode)
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
        functionReturnTypeSpacingRuleAssertThat(code)
            .hasLintViolation(1, 10, "Unexpected whitespace")
            .isFormattedAs(formattedCode)
    }
}
