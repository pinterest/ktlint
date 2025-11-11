package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRule
import org.junit.jupiter.api.Test

class MixedConditionOperatorsRuleTest {
    private val mixedConditionOperatorsRuleAssertThat = assertThatRule { MixedConditionOperatorsRule() }

    @Test
    fun `Given a single line condition with mixed logical operators in the same expression`() {
        val code =
            """
            val foo = bar1 && bar2 || bar3
            """.trimIndent()
        @Suppress("ktlint:standard:argument-list-wrapping", "ktlint:standard:max-line-length")
        mixedConditionOperatorsRuleAssertThat(code)
            .hasLintViolationWithoutAutoCorrect(1, 11, "A condition with mixed usage of '&&' and '||' is hard to read. Use parenthesis to clarify the (sub)condition.")
    }

    @Test
    fun `Given a multiline condition with mixed logical operators in the same expression`() {
        val code =
            """
            val foo = bar1 &&
                bar2 ||
                bar3
            """.trimIndent()
        @Suppress("ktlint:standard:argument-list-wrapping", "ktlint:standard:max-line-length")
        mixedConditionOperatorsRuleAssertThat(code)
            .hasLintViolationWithoutAutoCorrect(1, 11, "A condition with mixed usage of '&&' and '||' is hard to read. Use parenthesis to clarify the (sub)condition.")
    }

    @Test
    fun `Given a condition same logical operators in the expression but a different operator in a subexpression`() {
        val code =
            """
            val foo = bar1 && (bar2 || bar3) && bar4
            """.trimIndent()
        mixedConditionOperatorsRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Given a condition same logical operators in the expression and different operators in a subexpression`() {
        val code =
            """
            val foo = bar1 && (bar2 || bar3 && bar4) && bar5
            """.trimIndent()
        @Suppress("ktlint:standard:argument-list-wrapping", "ktlint:standard:max-line-length")
        mixedConditionOperatorsRuleAssertThat(code)
            .hasLintViolationWithoutAutoCorrect(1, 20, "A condition with mixed usage of '&&' and '||' is hard to read. Use parenthesis to clarify the (sub)condition.")
    }

    @Test
    fun `Given an assignment with a condition using only one type of operation reference than do not report a violation`() {
        val code =
            """
            var foo = false
            fun foo() {
                foo = bar1 && bar2 && bar3
            }
            """.trimIndent()
        mixedConditionOperatorsRuleAssertThat(code).hasNoLintViolations()
    }
}
