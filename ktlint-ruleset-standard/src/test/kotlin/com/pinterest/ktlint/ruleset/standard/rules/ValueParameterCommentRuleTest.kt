package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.test.KtLintAssertThat
import com.pinterest.ktlint.test.LintViolation
import org.junit.jupiter.api.Test

class ValueParameterCommentRuleTest {
    private val valueParameterCommentRuleAssertThat = KtLintAssertThat.assertThatRule { ValueParameterCommentRule() }

    @Test
    fun `Given a kdoc as only child of value parameter list`() {
        val code =
            """
            class Foo1(
                // some comment
            )
            class Foo2(
                /* some comment */
            )

            """.trimIndent()
        valueParameterCommentRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Given a comment as only child of value parameter list`() {
        val code =
            """
            class Foo1(
                // some comment
            )
            class Foo2(
                /* some comment */
            )

            """.trimIndent()
        valueParameterCommentRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Given a comment on separate line before value parameter ast node`() {
        val code =
            """
            class Foo1(
                // some comment
                val bar: Bar,
                // some comment
                val bar: Bar,
            )
            class Foo2(
                /* some comment */
                val bar: Bar,
                /* some comment */
                val bar: Bar,
            )
            class Foo3(
                /** some comment */
                val bar: Bar,
                /** some comment */
                val bar: Bar,
            )
            """.trimIndent()
        valueParameterCommentRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Given a comment inside value parameter ast node`() {
        val code =
            """
            class Foo1(
                val bar:
                    // some comment
                    Bar
            )
            class Foo2(
                val bar: /* some comment */ Bar
            )
            class Foo3(
                val bar: /** some comment */ Bar
            )
            """.trimIndent()
        @Suppress("ktlint:standard:argument-list-wrapping", "ktlint:standard:max-line-length")
        valueParameterCommentRuleAssertThat(code)
            .hasLintViolationsWithoutAutoCorrect(
                LintViolation(3, 9, "A comment inside or on same line after a 'value_parameter' is not allowed. It may be placed on a separate line above."),
                LintViolation(7, 14, "A comment inside or on same line after a 'value_parameter' is not allowed. It may be placed on a separate line above."),
                LintViolation(10, 14, "A comment inside or on same line after a 'value_parameter' is not allowed. It may be placed on a separate line above."),
            )
    }

    @Test
    fun `Given a comment as last node of value parameter ast node`() {
        val code =
            """
            class Foo1(
                val bar: Bar // some comment
            )
            class Foo2(
                val bar: Bar /* some comment */
            )
            class Foo3(
                val bar: Bar /* some comment */
            )
            """.trimIndent()
        @Suppress("ktlint:standard:argument-list-wrapping", "ktlint:standard:max-line-length")
        valueParameterCommentRuleAssertThat(code)
            .hasLintViolationsWithoutAutoCorrect(
                LintViolation(2, 18, "A comment inside or on same line after a 'value_parameter' is not allowed. It may be placed on a separate line above."),
                LintViolation(5, 18, "A comment inside or on same line after a 'value_parameter' is not allowed. It may be placed on a separate line above."),
                LintViolation(8, 18, "A comment inside or on same line after a 'value_parameter' is not allowed. It may be placed on a separate line above."),
            )
    }

    @Test
    fun `Given a comment as last node of value parameter list`() {
        val code =
            """
            class Foo(
                val bar: Bar
                // some comment
            )
            class Foo(
                val bar: Bar
                /* some comment */
            )
            """.trimIndent()
        valueParameterCommentRuleAssertThat(code).hasNoLintViolations()
    }
}
