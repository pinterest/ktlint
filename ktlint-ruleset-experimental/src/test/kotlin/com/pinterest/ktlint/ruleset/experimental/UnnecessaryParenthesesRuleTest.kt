package com.pinterest.ktlint.ruleset.experimental

import com.pinterest.ktlint.core.LintError
import com.pinterest.ktlint.test.format
import com.pinterest.ktlint.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class UnnecessaryParenthesesRuleTest {
    private val unnecessaryParenthesesRule = UnnecessaryParenthesesRule()

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
        assertThat(unnecessaryParenthesesRule.format(code)).isEqualTo(formattedCode)
        assertThat(unnecessaryParenthesesRule.lint(code)).containsExactly(
            LintError(2, 24, "unnecessary-parentheses", "Empty parentheses in function call followed by lambda are unnecessary")
        )
    }
}
