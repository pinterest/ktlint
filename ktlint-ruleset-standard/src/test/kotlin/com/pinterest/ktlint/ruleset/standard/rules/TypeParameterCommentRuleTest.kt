package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRule
import com.pinterest.ktlint.test.LintViolation
import org.junit.jupiter.api.Test

class TypeParameterCommentRuleTest {
    private val typeParameterCommentRuleAssertThat = assertThatRule { TypeParameterCommentRule() }

    @Test
    fun `Given a kdoc inside a type parameter`() {
        val code =
            """
            class Foo<in /** some comment */ Bar>
            """.trimIndent()
        typeParameterCommentRuleAssertThat(code)
            .hasLintViolationWithoutAutoCorrect(1, 14, "A KDoc is not allowed inside a 'type_parameter_list'")
    }

    @Test
    fun `Given a block comment inside a type parameter`() {
        val code =
            """
            class Foo<in /* some comment */ Bar>
            """.trimIndent()
        @Suppress("ktlint:standard:argument-list-wrapping", "ktlint:standard:max-line-length")
        typeParameterCommentRuleAssertThat(code)
            .hasLintViolationWithoutAutoCorrect(1, 14, "A (block or EOL) comment inside or on same line after a 'type_parameter' is not allowed. It may be placed on a separate line above.")
    }

    @Test
    fun `Given an EOL comment inside a type parameter`() {
        val code =
            """
            class Foo<in // some comment
            Bar>
            """.trimIndent()
        @Suppress("ktlint:standard:argument-list-wrapping", "ktlint:standard:max-line-length")
        typeParameterCommentRuleAssertThat(code)
            .hasLintViolationWithoutAutoCorrect(1, 14, "A (block or EOL) comment inside or on same line after a 'type_parameter' is not allowed. It may be placed on a separate line above.")
    }

    @Test
    fun `Given a kdoc as child of type parameter list`() {
        val code =
            """
            class Foo<
                /** some comment */
                Bar>
            """.trimIndent()
        typeParameterCommentRuleAssertThat(code)
            .hasLintViolationWithoutAutoCorrect(2, 5, "A KDoc is not allowed inside a 'type_parameter_list'")
    }

    @Test
    fun `Given a comment on separate line before type parameter ast node`() {
        val code =
            """
            class Foo1<
                // some comment
                Bar>
            class Foo2<
                /* some comment */
                Bar>
            """.trimIndent()
        typeParameterCommentRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Given a comment after, but on the same line as an type parameter ast node`() {
        val code =
            """
            class Foo1<
                Bar // some comment
                >
            class Foo2<Bar /* some comment */ >
            """.trimIndent()
        @Suppress("ktlint:standard:argument-list-wrapping", "ktlint:standard:max-line-length")
        typeParameterCommentRuleAssertThat(code)
            .hasLintViolationsWithoutAutoCorrect(
                LintViolation(2, 9, "A comment in a 'value_argument_list' is only allowed when placed on a separate line"),
                LintViolation(4, 16, "A comment in a 'value_argument_list' is only allowed when placed on a separate line"),
            )
    }

    @Test
    fun `Given a comment after a comma on the same line as an type parameter ast node`() {
        val code =
            """
            class FooBar1<
                Foo, // some comment
                Bar>
            class FooBar2<Foo, /* some comment */ Bar>
            """.trimIndent()
        @Suppress("ktlint:standard:argument-list-wrapping", "ktlint:standard:max-line-length")
        typeParameterCommentRuleAssertThat(code)
            .hasLintViolationsWithoutAutoCorrect(
                LintViolation(2, 10, "A comment in a 'value_argument_list' is only allowed when placed on a separate line"),
                LintViolation(4, 20, "A comment in a 'value_argument_list' is only allowed when placed on a separate line"),
            )
    }

    @Test
    fun `Given a comment as last node of type parameter list`() {
        val code =
            """
            class FooBar1<
                Foo
                // some comment
                >
            class FooBar2<
                Foo
                /* some comment */
                >
            """.trimIndent()
        typeParameterCommentRuleAssertThat(code).hasNoLintViolations()
    }
}
