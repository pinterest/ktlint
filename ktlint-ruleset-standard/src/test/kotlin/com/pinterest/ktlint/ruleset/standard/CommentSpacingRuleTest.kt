package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.core.LintError
import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThat
import org.junit.jupiter.api.Test

class CommentSpacingRuleTest {
    private val commentSpacingRuleAssertThat = CommentSpacingRule().assertThat()

    @Test
    fun testLintValidCommentSpacing() {
        val code =
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
        commentSpacingRuleAssertThat(code).hasNoLintErrors()
    }

    @Test
    fun testFormatInvalidCommentSpacing() {
        val code =
            """
            //comment
            var debugging = false// comment
            var debugging = false //comment
            var debugging = false//comment
            fun main() {
                System.out.println(//123
                    "test"
                )
            }
                //comment
            """.trimIndent()
        val formattedCode =
            """
            // comment
            var debugging = false // comment
            var debugging = false // comment
            var debugging = false // comment
            fun main() {
                System.out.println( // 123
                    "test"
                )
            }
                // comment
            """.trimIndent()
        commentSpacingRuleAssertThat(code)
            .hasLintErrors(
                LintError(1, 1, "comment-spacing", "Missing space after //"),
                LintError(2, 22, "comment-spacing", "Missing space before //"),
                LintError(3, 23, "comment-spacing", "Missing space after //"),
                LintError(4, 22, "comment-spacing", "Missing space before //"),
                LintError(4, 22, "comment-spacing", "Missing space after //"),
                LintError(6, 24, "comment-spacing", "Missing space before //"),
                LintError(6, 24, "comment-spacing", "Missing space after //"),
                LintError(10, 5, "comment-spacing", "Missing space after //")
            ).isFormattedAs(formattedCode)
    }
}
