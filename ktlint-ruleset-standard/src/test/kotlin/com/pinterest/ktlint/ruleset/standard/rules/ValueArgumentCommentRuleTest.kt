package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.test.KtLintAssertThat
import com.pinterest.ktlint.test.LintViolation
import org.junit.jupiter.api.Test

class ValueArgumentCommentRuleTest {
    private val valueArgumentCommentRuleAssertThat = KtLintAssertThat.assertThatRule { ValueArgumentCommentRule() }

    @Test
    fun `Given a block comment inside a value argument`() {
        val code =
            """
            val foo = foo(
                bar /* some comment */ = "bar"
            )
            """.trimIndent()
        @Suppress("ktlint:standard:argument-list-wrapping", "ktlint:standard:max-line-length")
        valueArgumentCommentRuleAssertThat(code)
            .hasLintViolationWithoutAutoCorrect(2, 9, "A (block or EOL) comment inside or on same line after a 'value_argument' is not allowed. It may be placed on a separate line above.")
    }

    @Test
    fun `Given an EOL comment inside a value argument`() {
        val code =
            """
            val foo = foo(
                bar // some comment
                    = "bar"
            )
            """.trimIndent()
        @Suppress("ktlint:standard:argument-list-wrapping", "ktlint:standard:max-line-length")
        valueArgumentCommentRuleAssertThat(code)
            .hasLintViolationWithoutAutoCorrect(2, 9, "A (block or EOL) comment inside or on same line after a 'value_argument' is not allowed. It may be placed on a separate line above.")
    }

    @Test
    fun `Given a comment as only child of value argument list`() {
        val code =
            """
            val foo1 = foo(
                // some comment
            )
            val foo2 = foo(
                /* some comment */
            )
            """.trimIndent()
        valueArgumentCommentRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Given a comment on separate line before value argument ast node`() {
        val code =
            """
            val foo1 = foo(
                // some comment
                "bar"
            )
            val foo2 = foo(
                /* some comment */
                "bar"
            )
            """.trimIndent()
        valueArgumentCommentRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Given a comment as last node of value argument ast node`() {
        val code =
            """
            val foo1 = foo(
                "bar" // some comment
            )
            val foo2 = foo(
                "bar" /* some comment */
            )
            """.trimIndent()
        @Suppress("ktlint:standard:argument-list-wrapping", "ktlint:standard:max-line-length")
        valueArgumentCommentRuleAssertThat(code)
            .hasLintViolationsWithoutAutoCorrect(
                LintViolation(2, 11, "A comment in a 'value_argument_list' is only allowed when placed on a separate line"),
                LintViolation(5, 11, "A comment in a 'value_argument_list' is only allowed when placed on a separate line"),
            )
    }

    @Test
    fun `Given a comment after a comma on the same line as an value argument ast node`() {
        val code =
            """
            val foo1 = foo(
                "bar", // some comment
            )
            val foo2 = foo(
                "bar", /* some comment */
            )
            """.trimIndent()
        @Suppress("ktlint:standard:argument-list-wrapping", "ktlint:standard:max-line-length")
        valueArgumentCommentRuleAssertThat(code)
            .hasLintViolationsWithoutAutoCorrect(
                LintViolation(2, 12, "A comment in a 'value_argument_list' is only allowed when placed on a separate line"),
                LintViolation(5, 12, "A comment in a 'value_argument_list' is only allowed when placed on a separate line"),
            )
    }

    @Test
    fun `Given a comment as last node of value argument list`() {
        val code =
            """
            val foo1 = foo(
                "bar"
                // some comment
            )
            val foo1 = foo(
                "bar"
                /* some comment */
            )
            """.trimIndent()
        valueArgumentCommentRuleAssertThat(code).hasNoLintViolations()
    }
}
