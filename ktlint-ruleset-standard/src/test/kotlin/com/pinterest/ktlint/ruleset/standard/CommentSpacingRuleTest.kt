package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.core.LintError
import com.pinterest.ktlint.test.format
import com.pinterest.ktlint.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.testng.annotations.Test

class CommentSpacingRuleTest {

    @Test
    fun testLintValidCommentSpacing() {
        assertThat(
            com.pinterest.ktlint.ruleset.standard.CommentSpacingRule().lint(
                """
                //
                //noinspection AndroidLintRecycle
                //region
                //endregion
                //language=SQL
                // comment
                var debugging = false // comment
                var debugging = false // comment//word
                    // comment
                """.trimIndent()
            )
        ).isEmpty()
    }

    @Test
    fun testLintInvalidCommentSpacing() {
        assertThat(
            com.pinterest.ktlint.ruleset.standard.CommentSpacingRule().lint(
                """
                //comment
                var debugging = false// comment
                var debugging = false //comment
                var debugging = false//comment
                    //comment
                """.trimIndent()
            )
        ).isEqualTo(
            listOf(
                LintError(1, 1, "comment-spacing", "Missing space after //"),
                LintError(2, 22, "comment-spacing", "Missing space before //"),
                LintError(3, 23, "comment-spacing", "Missing space after //"),
                LintError(4, 22, "comment-spacing", "Missing space before //"),
                LintError(4, 22, "comment-spacing", "Missing space after //"),
                LintError(5, 5, "comment-spacing", "Missing space after //")
            )
        )
    }

    @Test
    fun testFormatInvalidCommentSpacing() {
        assertThat(
            com.pinterest.ktlint.ruleset.standard.CommentSpacingRule().format(
                """
                //comment
                var debugging = false// comment
                var debugging = false //comment
                var debugging = false//comment
                    //comment
                """.trimIndent()
            )
        ).isEqualTo(
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
