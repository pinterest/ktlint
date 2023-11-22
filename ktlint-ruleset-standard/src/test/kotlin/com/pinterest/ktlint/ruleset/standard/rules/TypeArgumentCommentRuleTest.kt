package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRule
import com.pinterest.ktlint.test.LintViolation
import org.junit.jupiter.api.Test

class TypeArgumentCommentRuleTest {
    private val typeArgumentCommentRuleAssertThat = assertThatRule { TypeArgumentCommentRule() }

    @Test
    fun `Given a kdoc inside a type projection`() {
        val code =
            """
            fun Foo<out /** some comment */ Any>.foo() {}
            """.trimIndent()
        typeArgumentCommentRuleAssertThat(code)
            .hasLintViolationWithoutAutoCorrect(1, 13, "A KDoc is not allowed inside a 'type_argument_list'")
    }

    @Test
    fun `Given a block comment inside a type projection`() {
        val code =
            """
            fun Foo<out /* some comment */ Any>.foo() {}
            """.trimIndent()
        @Suppress("ktlint:standard:argument-list-wrapping", "ktlint:standard:max-line-length")
        typeArgumentCommentRuleAssertThat(code)
            .hasLintViolationWithoutAutoCorrect(1, 13, "A (block or EOL) comment inside or on same line after a 'type_projection' is not allowed. It may be placed on a separate line above.")
    }

    @Test
    fun `Given a EOL comment inside type projection`() {
        val code =
            """
            fun Foo<out // some comment
            Any>.foo() {}
            """.trimIndent()
        @Suppress("ktlint:standard:argument-list-wrapping", "ktlint:standard:max-line-length")
        typeArgumentCommentRuleAssertThat(code)
            .hasLintViolationWithoutAutoCorrect(1, 13, "A (block or EOL) comment inside or on same line after a 'type_projection' is not allowed. It may be placed on a separate line above.")
    }

    @Test
    fun `Given a kdoc as child of type argument list`() {
        val code =
            """
            val fooBar: FooBar<
                /** some comment */
                Foo, Bar>
            """.trimIndent()
        typeArgumentCommentRuleAssertThat(code)
            .hasLintViolationWithoutAutoCorrect(2, 5, "A KDoc is not allowed inside a 'type_argument_list'")
    }

    @Test
    fun `Given a comment on separate line before type projection ast node`() {
        val code =
            """
            val fooBar1: FooBar<
                // some comment
                Foo, Bar>
            val fooBar2: FooBar<
                /* some comment */
                Foo, Bar>
            """.trimIndent()
        typeArgumentCommentRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Given a comment after a comma on the same line as an type projection ast node`() {
        val code =
            """
            val fooBar1: FooBar<Foo, // some comment
                Bar>
            val fooBar2: FooBar<Foo, /* some comment */
                Bar>
            """.trimIndent()
        typeArgumentCommentRuleAssertThat(code)
            .hasLintViolationsWithoutAutoCorrect(
                LintViolation(1, 26, "A comment in a 'type_argument_list' is only allowed when placed on a separate line"),
                LintViolation(3, 26, "A comment in a 'type_argument_list' is only allowed when placed on a separate line"),
            )
    }

    @Test
    fun `Given a comment as last node of type argument list`() {
        val code =
            """
            val fooBar: FooBar<Foo, Bar
                // some comment
            >
            val fooBar: FooBar<Foo, Bar
                /* some comment */
            >
            """.trimIndent()
        typeArgumentCommentRuleAssertThat(code).hasNoLintViolations()
    }
}
