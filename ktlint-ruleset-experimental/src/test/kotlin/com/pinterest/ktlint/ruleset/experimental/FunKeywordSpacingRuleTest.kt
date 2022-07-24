package com.pinterest.ktlint.ruleset.experimental

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
}
