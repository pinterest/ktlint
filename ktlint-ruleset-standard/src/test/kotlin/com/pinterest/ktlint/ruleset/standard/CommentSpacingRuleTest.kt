package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThat
import com.pinterest.ktlint.test.LintViolation
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
        commentSpacingRuleAssertThat(code).hasNoLintViolations()
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
            .hasLintViolations(
                LintViolation(1, 1, "Missing space after //"),
                LintViolation(2, 22, "Missing space before //"),
                LintViolation(3, 23, "Missing space after //"),
                LintViolation(4, 22, "Missing space before //"),
                LintViolation(4, 22, "Missing space after //"),
                LintViolation(6, 24, "Missing space before //"),
                LintViolation(6, 24, "Missing space after //"),
                LintViolation(10, 5, "Missing space after //")
            ).isFormattedAs(formattedCode)
    }
}
