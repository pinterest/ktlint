package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.ruleset.standard.StandardRuleSetProvider
import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRuleBuilder
import com.pinterest.ktlint.test.LintViolation
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class ConditionWrappingRuleTest {
    private val conditionWrappingRuleAssertThat =
        assertThatRuleBuilder { ConditionWrappingRule() }
            .addAdditionalRuleProvider { IndentationRule() }
            .addRequiredRuleProviderDependenciesFrom(StandardRuleSetProvider())
            .assertThat()

    @Test
    fun `Given all operands on the same line`() {
        val code =
            """
            val foo = bar || baz
            """.trimIndent()
        conditionWrappingRuleAssertThat(code).hasNoLintViolations()
    }

    @Nested
    inner class `Given a simple condition with a multiline operand in the left hand side` {
        @Test
        fun `Given the right hand side operand is preceded by a space`() {
            val code =
                """
                val foo =
                    multiLineOperand(
                        "bar"
                    ) || baz
                """.trimIndent()
            val formattedCode =
                """
                val foo =
                    multiLineOperand(
                        "bar"
                    ) ||
                        baz
                """.trimIndent()
            conditionWrappingRuleAssertThat(code)
                .hasLintViolation(4, 10, "Newline expected before operand in multiline condition")
                .isFormattedAs(formattedCode)
        }

        @Test
        fun `Given the right hand side operand is not preceded by a whitespace`() {
            val code =
                """
                val foo =
                    multiLineOperand(
                        "bar"
                    ) ||baz
                """.trimIndent()
            val formattedCode =
                """
                val foo =
                    multiLineOperand(
                        "bar"
                    ) ||
                        baz
                """.trimIndent()
            conditionWrappingRuleAssertThat(code)
                .hasLintViolation(4, 9, "Newline expected before operand in multiline condition")
                .isFormattedAs(formattedCode)
        }

        @Test
        fun `Given the right hand side operand is preceded by a newline`() {
            val code =
                """
                val foo =
                    multiLineOperand(
                        "bar"
                    ) ||
                        baz
                """.trimIndent()
            conditionWrappingRuleAssertThat(code).hasNoLintViolations()
        }
    }

    @Nested
    inner class `Given a simple condition with a multiline operand in the right hand side` {
        @Test
        fun `Given the operand is preceded by a space`() {
            val code =
                """
                val foo =
                    bar || multiLineOperand(
                        "baz"
                    )
                """.trimIndent()
            val formattedCode =
                """
                val foo =
                    bar ||
                        multiLineOperand(
                            "baz"
                        )
                """.trimIndent()
            conditionWrappingRuleAssertThat(code)
                .hasLintViolation(2, 12, "Newline expected before operand in multiline condition")
                .isFormattedAs(formattedCode)
        }

        @Test
        fun `Given the operand is not preceded by a whitespace`() {
            val code =
                """
                val foo =
                    bar ||multiLineOperand(
                        "baz"
                    )
                """.trimIndent()
            val formattedCode =
                """
                val foo =
                    bar ||
                        multiLineOperand(
                            "baz"
                        )
                """.trimIndent()
            conditionWrappingRuleAssertThat(code)
                .hasLintViolation(2, 11, "Newline expected before operand in multiline condition")
                .isFormattedAs(formattedCode)
        }

        @Test
        fun `Given the operand is preceded by a newline`() {
            val code =
                """
                val foo =
                    bar ||
                        multiLineOperand(
                            "baz"
                        )
                """.trimIndent()
            conditionWrappingRuleAssertThat(code).hasNoLintViolations()
        }
    }

    @Test
    fun `Given not all operands are on the same line`() {
        val code =
            """
            val foo =
                bar1 || bar2 ||
                    baz1 || baz2 || baz3
            """.trimIndent()
        val formattedCode =
            """
            val foo =
                bar1 ||
                    bar2 ||
                    baz1 ||
                    baz2 ||
                    baz3
            """.trimIndent()
        conditionWrappingRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(2, 13, "Newline expected before operand in multiline condition"),
                LintViolation(3, 17, "Newline expected before operand in multiline condition"),
                LintViolation(3, 25, "Newline expected before operand in multiline condition"),
            ).isFormattedAs(formattedCode)
    }

    @Nested
    inner class `Given a parenthesized logical expression for which all inner operands are on the same line` {
        @Test
        fun `Given first operand is parenthesized`() {
            val code =
                """
                val foo =
                    (baz1 && baz2) || bar1 ||
                        bar2
                """.trimIndent()
            val formattedCode =
                """
                val foo =
                    (baz1 && baz2) ||
                        bar1 ||
                        bar2
                """.trimIndent()
            conditionWrappingRuleAssertThat(code)
                .hasLintViolation(2, 23, "Newline expected before operand in multiline condition")
                .isFormattedAs(formattedCode)
        }

        @Test
        fun `Given second operand on first line is parenthesized`() {
            val code =
                """
                val foo =
                    bar1 || (baz1 && baz2) ||
                        bar2
                """.trimIndent()
            val formattedCode =
                """
                val foo =
                    bar1 ||
                        (baz1 && baz2) ||
                        bar2
                """.trimIndent()
            conditionWrappingRuleAssertThat(code)
                .hasLintViolation(2, 13, "Newline expected before operand in multiline condition")
                .isFormattedAs(formattedCode)
        }

        @Test
        fun `Given second operand on second line is parenthesized`() {
            val code =
                """
                val foo =
                    bar1 ||
                        (baz1 && baz2) || bar2
                """.trimIndent()
            val formattedCode =
                """
                val foo =
                    bar1 ||
                        (baz1 && baz2) ||
                        bar2
                """.trimIndent()
            conditionWrappingRuleAssertThat(code)
                .hasLintViolation(3, 27, "Newline expected before operand in multiline condition")
                .isFormattedAs(formattedCode)
        }

        @Test
        fun `Given last operand on second line is parenthesized`() {
            val code =
                """
                val foo =
                    bar1 || bar2 ||
                        (baz1 && baz2)
                """.trimIndent()
            val formattedCode =
                """
                val foo =
                    bar1 ||
                        bar2 ||
                        (baz1 && baz2)
                """.trimIndent()
            conditionWrappingRuleAssertThat(code)
                .hasLintViolation(2, 13, "Newline expected before operand in multiline condition")
                .isFormattedAs(formattedCode)
        }
    }
}
