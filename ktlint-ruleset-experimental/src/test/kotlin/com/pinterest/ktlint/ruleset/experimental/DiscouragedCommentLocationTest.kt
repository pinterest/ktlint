package com.pinterest.ktlint.ruleset.experimental

import com.pinterest.ktlint.core.LintError
import com.pinterest.ktlint.test.format
import com.pinterest.ktlint.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class DiscouragedCommentLocationTest {
    @Test
    fun `Given an EOL comment after a type parameter then report a discouraged comment location`() {
        val code =
            """
            fun <T> // some comment
            foo(t: T) = "some-result"
            """.trimIndent()
        assertThat(DiscouragedCommentLocationRule().lint(code)).containsExactly(
            LintError(1, 9, "discouraged-comment-location", "No comment expected at this location")
        )
        assertThat(DiscouragedCommentLocationRule().format(code)).isEqualTo(code)
    }

    @Test
    fun `Given an EOL comment on a newline after a type parameter then report a discouraged comment location`() {
        val code =
            """
            fun <T>
            // some comment
            foo(t: T) = "some-result"
            """.trimIndent()
        assertThat(DiscouragedCommentLocationRule().lint(code)).containsExactly(
            LintError(2, 1, "discouraged-comment-location", "No comment expected at this location")
        )
        assertThat(DiscouragedCommentLocationRule().format(code)).isEqualTo(code)
    }

    @Test
    fun `Given a block comment after a type parameter then report a discouraged comment location`() {
        val code =
            """
            fun <T> /* some comment */
            foo(t: T) = "some-result"
            """.trimIndent()
        assertThat(DiscouragedCommentLocationRule().lint(code)).containsExactly(
            LintError(1, 9, "discouraged-comment-location", "No comment expected at this location")
        )
        assertThat(DiscouragedCommentLocationRule().format(code)).isEqualTo(code)
    }

    @Test
    fun `Given a block comment on a newline after a type parameter then report a discouraged comment location`() {
        val code =
            """
            fun <T>
            /* some comment */
            foo(t: T) = "some-result"
            """.trimIndent()
        assertThat(DiscouragedCommentLocationRule().lint(code)).containsExactly(
            LintError(2, 1, "discouraged-comment-location", "No comment expected at this location")
        )
        assertThat(DiscouragedCommentLocationRule().format(code)).isEqualTo(code)
    }

    @Test
    fun `Given a KDOC comment after a type parameter then report a discouraged comment location`() {
        val code =
            """
            fun <T> /** some comment */
            foo(t: T) = "some-result"
            """.trimIndent()
        assertThat(DiscouragedCommentLocationRule().lint(code)).containsExactly(
            LintError(1, 9, "discouraged-comment-location", "No comment expected at this location")
        )
        assertThat(DiscouragedCommentLocationRule().format(code)).isEqualTo(code)
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
        assertThat(DiscouragedCommentLocationRule().lint(code)).containsExactly(
            LintError(2, 1, "discouraged-comment-location", "No comment expected at this location")
        )
        assertThat(DiscouragedCommentLocationRule().format(code)).isEqualTo(code)
    }
}
