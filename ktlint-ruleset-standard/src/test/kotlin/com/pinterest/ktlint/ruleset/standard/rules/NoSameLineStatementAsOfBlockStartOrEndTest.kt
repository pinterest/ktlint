package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.test.KtLintAssertThat
import com.pinterest.ktlint.test.LintViolation
import org.junit.jupiter.api.Test

class NoSameLineStatementAsOfBlockStartOrEndTest {
    private val noSameLineStatementAsOfBlockStartOrEndAssertThat =
        KtLintAssertThat.assertThatRule { NoSameLineStatementAsOfBlockStartOrEnd() }

    @Test
    fun `Given function body starts at the same line with function lbrace`() {
        val code =
            """
            fun foo() { if (true) {
                    // do something
                }
            }
            """.trimIndent()
        val formattedCode =
            """
            fun foo() {
                if (true) {
                    // do something
                }
            }
            """.trimIndent()
        noSameLineStatementAsOfBlockStartOrEndAssertThat(code)
            .hasLintViolation(1, 13, AFTER_LBRACE_ERROR_MSG)
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given function body ends at the same line with function rbrace`() {
        val code =
            """
            fun foo() {
                if (true) {
                    // do something
                } }
            """.trimIndent()
        val formattedCode =
            """
            fun foo() {
                if (true) {
                    // do something
                }
            }
            """.trimIndent()
        noSameLineStatementAsOfBlockStartOrEndAssertThat(code)
            .hasLintViolation(4, 5, BEFORE_RBRACE_ERROR_MSG)
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given function body doesn't start or end with braces`() {
        val code =
            """
            fun foo() {
                if (true) {
                    // do something
                }
            }
            """.trimIndent()
        noSameLineStatementAsOfBlockStartOrEndAssertThat(code)
            .hasNoLintViolations()
    }

    @Test
    fun `Given function body is if else expression`() {
        val code =
            """
            fun foo() = if (true) {
                // do something
            } else {
                // do something else
            }
            """.trimIndent()
        noSameLineStatementAsOfBlockStartOrEndAssertThat(code)
            .hasNoLintViolations()
    }

    @Test
    fun `Given function body is number`() {
        val code =
            """
            fun foo() = 1
            """.trimIndent()
        noSameLineStatementAsOfBlockStartOrEndAssertThat(code)
            .hasNoLintViolations()
    }

    @Test
    fun `Given function single line of body is aligns with both lbrace and rbrace`() {
        val code =
            """
            fun foo() { println("") }
            """.trimIndent()

        val formattedCode =
            """
            fun foo() {
                println("")
            }
            """.trimIndent()
        noSameLineStatementAsOfBlockStartOrEndAssertThat(code)
            .hasLintViolations(
                LintViolation(1, 13, AFTER_LBRACE_ERROR_MSG),
                LintViolation(1, 23, BEFORE_RBRACE_ERROR_MSG)
            )
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given function body is aligns with both lbrace and rbrace`() {
        val code =
            """
            fun foo() { if (true) {
                    // do something
                }}
            """.trimIndent()

        val formattedCode =
            """
            fun foo() {
                if (true) {
                    // do something
                }
            }
            """.trimIndent()
        noSameLineStatementAsOfBlockStartOrEndAssertThat(code)
            .hasLintViolations(
                LintViolation(1, 13, AFTER_LBRACE_ERROR_MSG),
                LintViolation(3, 5, BEFORE_RBRACE_ERROR_MSG)
            )
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given function doesn't have empty body`() {
        val code =
            """
            fun foo() {}
            """.trimIndent()
        noSameLineStatementAsOfBlockStartOrEndAssertThat(code)
            .hasNoLintViolations()
    }

    @Test
    fun `Given function doesn't have empty body with space`() {
        val code =
            """
            fun foo() { }
            """.trimIndent()
        noSameLineStatementAsOfBlockStartOrEndAssertThat(code)
            .hasNoLintViolations()
    }

    companion object {
        private const val AFTER_LBRACE_ERROR_MSG = "Expected new line after `{` of function body"
        private const val BEFORE_RBRACE_ERROR_MSG = "Expected new line before `}` of function body"
    }
}
