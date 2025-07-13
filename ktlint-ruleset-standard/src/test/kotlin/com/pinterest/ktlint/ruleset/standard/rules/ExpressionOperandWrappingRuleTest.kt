package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.ruleset.standard.StandardRuleSetProvider
import com.pinterest.ktlint.test.KtLintAssertThat.Companion.EOL_CHAR
import com.pinterest.ktlint.test.KtLintAssertThat.Companion.MAX_LINE_LENGTH_MARKER
import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRuleBuilder
import com.pinterest.ktlint.test.LintViolation
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class ExpressionOperandWrappingRuleTest {
    private val expressionOperandWrappingRuleAssertThat =
        assertThatRuleBuilder { ExpressionOperandWrappingRule() }
            .addAdditionalRuleProvider { IndentationRule() }
            .addRequiredRuleProviderDependenciesFrom(StandardRuleSetProvider())
            .assertThat()

    @Test
    fun `Given all operands on the same line`() {
        val code =
            """
            val foo1 = bar || baz
            val foo2 = bar + baz
            """.trimIndent()
        expressionOperandWrappingRuleAssertThat(code).hasNoLintViolations()
    }

    @Nested
    inner class `Given a simple expression with a multiline operand in the left hand side` {
        @Test
        fun `Given the right hand side operand is preceded by a space`() {
            val code =
                """
                val foo1 =
                    multiLineOperand(
                        "bar"
                    ) || baz
                val foo2 =
                    multiLineOperand(
                        "bar"
                    ) + baz
                """.trimIndent()
            val formattedCode =
                """
                val foo1 =
                    multiLineOperand(
                        "bar"
                    ) ||
                        baz
                val foo2 =
                    multiLineOperand(
                        "bar"
                    ) +
                        baz
                """.trimIndent()
            expressionOperandWrappingRuleAssertThat(code)
                .hasLintViolations(
                    LintViolation(4, 10, "Newline expected before operand in multiline expression"),
                    LintViolation(8, 9, "Newline expected before operand in multiline expression"),
                ).isFormattedAs(formattedCode)
        }

        @Test
        fun `Given the right hand side operand is not preceded by a whitespace`() {
            val code =
                """
                val foo1 =
                    multiLineOperand(
                        "bar"
                    ) ||baz
                val foo2 =
                    multiLineOperand(
                        "bar"
                    ) +baz
                """.trimIndent()
            val formattedCode =
                """
                val foo1 =
                    multiLineOperand(
                        "bar"
                    ) ||
                        baz
                val foo2 =
                    multiLineOperand(
                        "bar"
                    ) +
                        baz
                """.trimIndent()
            expressionOperandWrappingRuleAssertThat(code)
                .hasLintViolations(
                    LintViolation(4, 9, "Newline expected before operand in multiline expression"),
                    LintViolation(8, 8, "Newline expected before operand in multiline expression"),
                ).isFormattedAs(formattedCode)
        }

        @Test
        fun `Given the right hand side operand is preceded by a newline`() {
            val code =
                """
                val foo1 =
                    multiLineOperand(
                        "bar"
                    ) ||
                        baz
                val foo2 =
                    multiLineOperand(
                        "bar"
                    ) +
                        baz
                """.trimIndent()
            expressionOperandWrappingRuleAssertThat(code).hasNoLintViolations()
        }
    }

    @Nested
    inner class `Given a simple expression with a multiline operand in the right hand side` {
        @Test
        fun `Given the operand is preceded by a space`() {
            val code =
                """
                val foo1 =
                    bar || multiLineOperand(
                        "baz"
                    )
                val foo2 =
                    bar + multiLineOperand(
                        "baz"
                    )
                """.trimIndent()
            val formattedCode =
                """
                val foo1 =
                    bar ||
                        multiLineOperand(
                            "baz"
                        )
                val foo2 =
                    bar +
                        multiLineOperand(
                            "baz"
                        )
                """.trimIndent()
            expressionOperandWrappingRuleAssertThat(code)
                .hasLintViolations(
                    LintViolation(2, 12, "Newline expected before operand in multiline expression"),
                    LintViolation(6, 11, "Newline expected before operand in multiline expression"),
                ).isFormattedAs(formattedCode)
        }

        @Test
        fun `Given the operand is not preceded by a whitespace`() {
            val code =
                """
                val foo1 =
                    bar ||multiLineOperand(
                        "baz"
                    )
                val foo2 =
                    bar +multiLineOperand(
                        "baz"
                    )
                """.trimIndent()
            val formattedCode =
                """
                val foo1 =
                    bar ||
                        multiLineOperand(
                            "baz"
                        )
                val foo2 =
                    bar +
                        multiLineOperand(
                            "baz"
                        )
                """.trimIndent()
            expressionOperandWrappingRuleAssertThat(code)
                .hasLintViolations(
                    LintViolation(2, 11, "Newline expected before operand in multiline expression"),
                    LintViolation(6, 10, "Newline expected before operand in multiline expression"),
                ).isFormattedAs(formattedCode)
        }

        @Test
        fun `Given the operand is preceded by a newline`() {
            val code =
                """
                val foo1 =
                    bar ||
                        multiLineOperand(
                            "baz"
                        )
                val foo2 =
                    bar +
                        multiLineOperand(
                            "baz"
                        )
                """.trimIndent()
            expressionOperandWrappingRuleAssertThat(code).hasNoLintViolations()
        }
    }

    @Test
    fun `Given not all operands are on the same line`() {
        val code =
            """
            val foo1 =
                bar1 || bar2 ||
                    baz1 || baz2 || baz3
            val foo2 =
                bar1 + bar2 +
                    baz1 + baz2 + baz3
            """.trimIndent()
        val formattedCode =
            """
            val foo1 =
                bar1 ||
                    bar2 ||
                    baz1 ||
                    baz2 ||
                    baz3
            val foo2 =
                bar1 +
                    bar2 +
                    baz1 +
                    baz2 +
                    baz3
            """.trimIndent()
        expressionOperandWrappingRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(2, 13, "Newline expected before operand in multiline expression"),
                LintViolation(3, 17, "Newline expected before operand in multiline expression"),
                LintViolation(3, 25, "Newline expected before operand in multiline expression"),
                LintViolation(5, 12, "Newline expected before operand in multiline expression"),
                LintViolation(6, 16, "Newline expected before operand in multiline expression"),
                LintViolation(6, 23, "Newline expected before operand in multiline expression"),
            ).isFormattedAs(formattedCode)
    }

    @Nested
    inner class `Given a parenthesized logical expression for which all inner operands are on the same line` {
        @Test
        fun `Given first operand is parenthesized`() {
            val code =
                """
                val foo1 =
                    (baz1 && baz2) || bar1 ||
                        bar2
                val foo2 =
                    (baz1 - baz2) + bar1 +
                        bar2
                """.trimIndent()
            val formattedCode =
                """
                val foo1 =
                    (baz1 && baz2) ||
                        bar1 ||
                        bar2
                val foo2 =
                    (baz1 - baz2) +
                        bar1 +
                        bar2
                """.trimIndent()
            expressionOperandWrappingRuleAssertThat(code)
                .hasLintViolations(
                    LintViolation(2, 23, "Newline expected before operand in multiline expression"),
                    LintViolation(5, 21, "Newline expected before operand in multiline expression"),
                ).isFormattedAs(formattedCode)
        }

        @Test
        fun `Given second operand on first line is parenthesized`() {
            val code =
                """
                val foo1 =
                    bar1 || (baz1 && baz2) ||
                        bar2
                val foo2 =
                    bar1 + (baz1 - baz2) +
                        bar2
                """.trimIndent()
            val formattedCode =
                """
                val foo1 =
                    bar1 ||
                        (baz1 && baz2) ||
                        bar2
                val foo2 =
                    bar1 +
                        (baz1 - baz2) +
                        bar2
                """.trimIndent()
            expressionOperandWrappingRuleAssertThat(code)
                .hasLintViolations(
                    LintViolation(2, 13, "Newline expected before operand in multiline expression"),
                    LintViolation(5, 12, "Newline expected before operand in multiline expression"),
                ).isFormattedAs(formattedCode)
        }

        @Test
        fun `Given second operand on second line is parenthesized`() {
            val code =
                """
                val foo1 =
                    bar1 ||
                        (baz1 && baz2) || bar2
                val foo2 =
                    bar1 +
                        (baz1 - baz2) + bar2
                """.trimIndent()
            val formattedCode =
                """
                val foo1 =
                    bar1 ||
                        (baz1 && baz2) ||
                        bar2
                val foo2 =
                    bar1 +
                        (baz1 - baz2) +
                        bar2
                """.trimIndent()
            expressionOperandWrappingRuleAssertThat(code)
                .hasLintViolations(
                    LintViolation(3, 27, "Newline expected before operand in multiline expression"),
                    LintViolation(6, 25, "Newline expected before operand in multiline expression"),
                ).isFormattedAs(formattedCode)
        }

        @Test
        fun `Given last operand on second line is parenthesized`() {
            val code =
                """
                val foo1 =
                    bar1 || bar2 ||
                        (baz1 && baz2)
                val foo2 =
                    bar1 + bar2 +
                        (baz1 - baz2)
                """.trimIndent()
            val formattedCode =
                """
                val foo1 =
                    bar1 ||
                        bar2 ||
                        (baz1 && baz2)
                val foo2 =
                    bar1 +
                        bar2 +
                        (baz1 - baz2)
                """.trimIndent()
            expressionOperandWrappingRuleAssertThat(code)
                .hasLintViolations(
                    LintViolation(2, 13, "Newline expected before operand in multiline expression"),
                    LintViolation(5, 12, "Newline expected before operand in multiline expression"),
                ).isFormattedAs(formattedCode)
        }
    }

    @Test
    fun `Given an if-expression containing two simple expression on the same line, and another arithmetic expression on a separate line not exceeding max line length, then wrap on the logical operands only`() {
        val code =
            """
            // $MAX_LINE_LENGTH_MARKER      $EOL_CHAR
            fun foo() {
                if (bar1 || bar2 ||
                    baz1 + baz2 + baz3 > baz4
                ) {
                    // do something
                }
            }
            """.trimIndent()
        val formattedCode =
            """
            // $MAX_LINE_LENGTH_MARKER      $EOL_CHAR
            fun foo() {
                if (bar1 ||
                    bar2 ||
                    baz1 + baz2 + baz3 > baz4
                ) {
                    // do something
                }
            }
            """.trimIndent()
        expressionOperandWrappingRuleAssertThat(code)
            .setMaxLineLength()
            .hasLintViolation(3, 17, "Newline expected before operand in multiline expression")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a multiline expression containing EOL comments after the wrappable operand the wrap but leave the EOL after the operand on the same line`() {
        val code =
            """
            val foo1 =
                bar1 || // some comment
                    bar2 || // some comment
                    (baz1 && baz2) // some comment
            val foo2 =
                bar1 + // some comment
                    bar2 + // some comment
                    (baz1 - baz2) // some comment
            """.trimIndent()
        expressionOperandWrappingRuleAssertThat(code).hasNoLintViolations()
    }
}
