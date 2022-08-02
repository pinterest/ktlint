package com.pinterest.ktlint.ruleset.experimental

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRule
import org.junit.jupiter.api.Test

class DiscouragedCommentLocationTest {
    private val discouragedCommentLocationRuleAssertThat = assertThatRule { DiscouragedCommentLocationRule() }

    @Test
    fun `Given an EOL comment after a type parameter then report a discouraged comment location`() {
        val code =
            """
            fun <T> // some comment
            foo(t: T) = "some-result"
            """.trimIndent()
        discouragedCommentLocationRuleAssertThat(code)
            .hasLintViolationWithoutAutoCorrect(1, 9, "No comment expected at this location")
    }

    @Test
    fun `Given an EOL comment on a newline after a type parameter then report a discouraged comment location`() {
        val code =
            """
            fun <T>
            // some comment
            foo(t: T) = "some-result"
            """.trimIndent()
        discouragedCommentLocationRuleAssertThat(code)
            .hasLintViolationWithoutAutoCorrect(2, 1, "No comment expected at this location")
    }

    @Test
    fun `Given a block comment after a type parameter then report a discouraged comment location`() {
        val code =
            """
            fun <T> /* some comment */
            foo(t: T) = "some-result"
            """.trimIndent()
        discouragedCommentLocationRuleAssertThat(code)
            .hasLintViolationWithoutAutoCorrect(1, 9, "No comment expected at this location")
    }

    @Test
    fun `Given a block comment on a newline after a type parameter then report a discouraged comment location`() {
        val code =
            """
            fun <T>
            /* some comment */
            foo(t: T) = "some-result"
            """.trimIndent()
        discouragedCommentLocationRuleAssertThat(code)
            .hasLintViolationWithoutAutoCorrect(2, 1, "No comment expected at this location")
    }

    @Test
    fun `Given a KDOC comment after a type parameter then report a discouraged comment location`() {
        val code =
            """
            fun <T> /** some comment */
            foo(t: T) = "some-result"
            """.trimIndent()
        discouragedCommentLocationRuleAssertThat(code)
            .hasLintViolationWithoutAutoCorrect(1, 9, "No comment expected at this location")
    }

    @Test
    fun `Given a KDOC comment on a newline after a type parameter then report a discouraged comment location`() {
        val code =
            """
            fun <T>
            /**
              * some comment
              */
            foo(t: T) = "some-result"
            """.trimIndent()
        discouragedCommentLocationRuleAssertThat(code)
            .hasLintViolationWithoutAutoCorrect(2, 1, "No comment expected at this location")
    }
}
