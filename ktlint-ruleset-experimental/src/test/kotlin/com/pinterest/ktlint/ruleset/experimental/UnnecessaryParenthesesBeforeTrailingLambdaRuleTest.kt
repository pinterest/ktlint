package com.pinterest.ktlint.ruleset.experimental

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRule
import org.junit.jupiter.api.Test

class UnnecessaryParenthesesBeforeTrailingLambdaRuleTest {
    private val unnecessaryParenthesesBeforeTrailingLambdaRuleAssertThat =
        assertThatRule { UnnecessaryParenthesesBeforeTrailingLambdaRule() }

    @Test
    fun `Remove unnecessary parentheses in function call followed by lambda`() {
        val code =
            """
            fun countDash(input: String) =
                "some-string".count() { it == '-' }
            """.trimIndent()
        val formattedCode =
            """
            fun countDash(input: String) =
                "some-string".count { it == '-' }
            """.trimIndent()
        unnecessaryParenthesesBeforeTrailingLambdaRuleAssertThat(code)
            .hasLintViolation(2, 24, "Empty parentheses in function call followed by lambda are unnecessary")
            .isFormattedAs(formattedCode)
    }
}
