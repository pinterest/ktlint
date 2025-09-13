package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRule
import com.pinterest.ktlint.test.LintViolation
import com.pinterest.ktlint.test.replaceStringTemplatePlaceholder
import org.junit.jupiter.api.Test

class ThenSpacingRuleTest {
    private val thenSpacingRuleAssertThat = assertThatRule { ThenSpacingRule() }

    @Test
    fun `Given a then block without spacing`() {
        val code =
            """
            fun foo() {
                if (true)true
                if (true)(0)else (1)
                if (true)print(0)else (1)
            }
            """.trimIndent().replaceStringTemplatePlaceholder()
        val formattedCode =
            """
            fun foo() {
                if (true) true
                if (true) (0) else (1)
                if (true) print(0) else (1)
            }
            """.trimIndent().replaceStringTemplatePlaceholder()
        thenSpacingRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(2, 14, "Expected a whitespace before 'then' block"),
                LintViolation(3, 14, "Expected a whitespace before 'then' block"),
                LintViolation(3, 17, "Expected a whitespace after 'then' block"),
                LintViolation(4, 14, "Expected a whitespace before 'then' block"),
                LintViolation(4, 22, "Expected a whitespace after 'then' block"),
            ).isFormattedAs(formattedCode)
    }
}
