package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRule
import org.junit.jupiter.api.Test

class NoBlankLinesInChainedMethodCallsRuleTest {
    private val noBlankLinesInChainedMethodCallsRuleAssertThat =
        assertThatRule { NoBlankLinesInChainedMethodCallsRule() }

    @Test
    fun `single blank line in dot qualified expression returns lint error`() {
        val code =
            """
            fun foo(inputText: String) {
                inputText

                    .lowercase(Locale.getDefault())
            }
            """.trimIndent()
        val formattedCode =
            """
            fun foo(inputText: String) {
                inputText
                    .lowercase(Locale.getDefault())
            }
            """.trimIndent()
        noBlankLinesInChainedMethodCallsRuleAssertThat(code)
            .hasLintViolation(3, 1, "Needless blank line(s)")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `single blank line between statements does not return lint error`() {
        val code =
            """
            fun foo(inputText: String) {
                bar()

                bar()
            }
            """.trimIndent()
        noBlankLinesInChainedMethodCallsRuleAssertThat(code).hasNoLintViolations()
    }
}
