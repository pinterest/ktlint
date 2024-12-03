package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRule
import org.junit.jupiter.api.Test

class FunKeywordSpacingRuleTest {
    private val funKeywordSpacingRuleAssertThat = assertThatRule { FunKeywordSpacingRule() }

    @Test
    fun `Given a function signature with multiple spaces between the fun keyword and the function name then remove the redundant spaces`() {
        val code =
            """
            fun  foo() = "some-result"
            """.trimIndent()
        val formattedCode =
            """
            fun foo() = "some-result"
            """.trimIndent()
        funKeywordSpacingRuleAssertThat(code)
            .hasLintViolation(1, 4, "Single space expected after the fun keyword")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a function signature with a newline between the fun keyword and the function name then remove the redundant newline`() {
        val code =
            """
            fun
            foo() = "some-result"
            """.trimIndent()
        val formattedCode =
            """
            fun foo() = "some-result"
            """.trimIndent()
        funKeywordSpacingRuleAssertThat(code)
            .hasLintViolation(1, 4, "Single space expected after the fun keyword")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Issue 2879 - Given a function with name between backticks then the fun keyword and name should be separated by a space`() {
        val code =
            """
            fun`foo or bar`() = "foo"
            """.trimIndent()
        val formattedCode =
            """
            fun `foo or bar`() = "foo"
            """.trimIndent()
        funKeywordSpacingRuleAssertThat(code)
            .hasLintViolation(1, 4, "Space expected between the fun keyword and backtick")
            .isFormattedAs(formattedCode)
    }
}
