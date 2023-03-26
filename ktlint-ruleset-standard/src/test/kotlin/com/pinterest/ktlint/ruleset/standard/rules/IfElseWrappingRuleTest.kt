package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.rule.engine.core.api.RuleProvider
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.CODE_STYLE_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.CodeStyleValue.ktlint_official
import com.pinterest.ktlint.test.KtLintAssertThat
import com.pinterest.ktlint.test.LintViolation
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class IfElseWrappingRuleTest {
    private val ifElseWrappingRuleAssertThat =
        KtLintAssertThat.assertThatRule(
            provider = { IfElseWrappingRule() },
            additionalRuleProviders =
                setOf(
                    // Required by visitor modifier
                    RuleProvider { DiscouragedCommentLocationRule() },
                    // Keep formatted code readable
                    RuleProvider { IndentationRule() },
                ),
            editorConfigProperties = setOf(CODE_STYLE_PROPERTY to ktlint_official),
        )

    @Test
    fun `Given a single line if statement without else then do not report a violation`() {
        val code =
            """
            fun foobar() {
                if (true) foo()
            }
            """.trimIndent()
        ifElseWrappingRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Given a single line if statement with a single else then do not report a violation`() {
        val code =
            """
            fun foobar() {
                if (true) foo() else bar()
            }
            """.trimIndent()
        ifElseWrappingRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Given an if with THEN on the same line as the multiline CONDITION`() {
        val code =
            """
            fun foobar() {
                if (true ||
                    false
                ) foo()
            }
            """.trimIndent()
        val formattedCode =
            """
            fun foobar() {
                if (true ||
                    false
                )
                    foo()
            }
            """.trimIndent()
        ifElseWrappingRuleAssertThat(code)
            .hasLintViolation(4, 7, "Expected a newline")
            .isFormattedAs(formattedCode)
    }

    @Nested
    inner class `Given a single line nested if-statement where the inner if is not wrapped in a block` {
        @Test
        fun `Given it is nested in a THEN`() {
            val code =
                """
                fun foobar() {
                    if (true) if (false) foo() else bar()
                }
                """.trimIndent()
            val formattedCode =
                """
                fun foobar() {
                    if (true)
                        if (false)
                            foo()
                        else
                            bar()
                }
                """.trimIndent()
            ifElseWrappingRuleAssertThat(code)
                .hasLintViolations(
                    LintViolation(2, 15, "Expected a newline"),
                    LintViolation(2, 26, "Expected a newline"),
                    LintViolation(2, 32, "Expected a newline"),
                    LintViolation(2, 37, "Expected a newline"),
                ).isFormattedAs(formattedCode)
        }

        @Test
        fun `Given it is nested in the ELSE`() {
            val code =
                """
                fun foobar() {
                    if (true) bar() else if (false) foo() else bar()
                }
                """.trimIndent()
            val formattedCode =
                """
                fun foobar() {
                    if (true)
                        bar()
                    else if (false)
                        foo()
                    else
                        bar()
                }
                """.trimIndent()
            ifElseWrappingRuleAssertThat(code)
                .hasLintViolations(
                    LintViolation(2, 15, "Expected a newline"),
                    LintViolation(2, 21, "Expected a newline"),
                    LintViolation(2, 37, "Expected a newline"),
                    LintViolation(2, 43, "Expected a newline"),
                    LintViolation(2, 48, "Expected a newline"),
                ).isFormattedAs(formattedCode)
        }
    }

    @Nested
    inner class `Given a single line if-statement with a branch wrapped in a block` {
        @Test
        fun `Given the THEN branch is wrapped`() {
            val code =
                """
                fun foobar() {
                    if (true) { if (false) foo() else bar() }
                }
                """.trimIndent()
            @Suppress("ktlint:argument-list-wrapping", "ktlint:max-line-length")
            ifElseWrappingRuleAssertThat(code)
                .hasLintViolationWithoutAutoCorrect(2, 15, "A single line if-statement should be kept simple. The 'THEN' may not be wrapped in a block.")
        }

        @Test
        fun `Given the ELSE branch is wrapped`() {
            val code =
                """
                fun foobar() {
                    if (true) bar() else { if (false) foo() else bar() }
                }
                """.trimIndent()
            @Suppress("ktlint:argument-list-wrapping", "ktlint:max-line-length")
            ifElseWrappingRuleAssertThat(code)
                .hasLintViolationWithoutAutoCorrect(2, 26, "A single line if-statement should be kept simple. The 'ELSE' may not be wrapped in a block.")
        }
    }
}