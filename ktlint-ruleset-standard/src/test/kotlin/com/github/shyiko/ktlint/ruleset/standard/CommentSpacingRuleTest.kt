package com.github.shyiko.ktlint.ruleset.standard

import com.github.shyiko.ktlint.core.LintError
import com.github.shyiko.ktlint.test.format
import com.github.shyiko.ktlint.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.testng.annotations.Test

class CommentSpacingRuleTest {

    @Test
    fun testLintValidCommentSpacing() {
        assertThat(CommentSpacingRule().lint(
            """
                // comment
                var debugging = false // comment
                var debugging = false // comment//word
                    // comment
            """.trimIndent()
        )).isEmpty()
    }

    @Test
    fun testLintInvalidCommentSpacing() {
        assertThat(CommentSpacingRule().lint(
            """
                //comment
                var debugging = false// comment
                var debugging = false //comment
                var debugging = false//comment
                    //comment
            """.trimIndent()
        )).isEqualTo(listOf(
            LintError(1, 1, "comment-spacing", "Missing space after double slash in end of line comment"),
            LintError(2, 22, "comment-spacing", "Missing space before end of line comment"),
            LintError(3, 23, "comment-spacing", "Missing space after double slash in end of line comment"),
            LintError(4, 22, "comment-spacing", "Missing space before end of line comment"),
            LintError(4, 22, "comment-spacing", "Missing space after double slash in end of line comment"),
            LintError(5, 5, "comment-spacing", "Missing space after double slash in end of line comment")
        ))
    }

    @Test
    fun testFormatInvalidCommentSpacing() {
        assertThat(CommentSpacingRule().format(
            """
                //comment
                var debugging = false// comment
                var debugging = false //comment
                var debugging = false//comment
                    //comment
            """.trimIndent()
        )).isEqualTo(
            """
                // comment
                var debugging = false // comment
                var debugging = false // comment
                var debugging = false // comment
                    // comment
            """.trimIndent()
        )
    }
}
