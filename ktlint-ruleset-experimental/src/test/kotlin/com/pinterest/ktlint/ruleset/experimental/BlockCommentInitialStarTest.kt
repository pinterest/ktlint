package com.pinterest.ktlint.ruleset.experimental

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRule
import com.pinterest.ktlint.test.LintViolation
import org.junit.jupiter.api.Test

class BlockCommentInitialStarTest {
    private val blockCommentInitialStarAlignmentRuleAssertThat = assertThatRule { BlockCommentInitialStarAlignmentRule() }

    @Test
    fun `Given a block comment for which the indentation followed by the star already aligns with the star in the first line of the block comment then do not reformat`() {
        val code =
            """
            /*
             * This blocked is formatted well.
             */
            """.trimIndent()
        blockCommentInitialStarAlignmentRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Given a block comment with a line that does not have an initial star then do not reformat that line`() {
        val code =
            """
            /*
                      This blocked is formatted well.
             */
            """.trimIndent()
        blockCommentInitialStarAlignmentRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Given a block comment with a line that contains a star which is preceded by a non-space or non-tab character then do not reformat that line`() {
        val code =
            """
            /*
                   - This line contains a * but it is not the initial *.
             */
            """.trimIndent()
        blockCommentInitialStarAlignmentRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Given that the initial stars of the block comment are not aligned then reformat`() {
        val code =
            """
            /*
                  * This blocked is not formatted well.
                */
            """.trimIndent()
        val formattedCode =
            """
            /*
             * This blocked is not formatted well.
             */
            """.trimIndent()
        blockCommentInitialStarAlignmentRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(2, 8, "Initial star should be align with start of block comment"),
                LintViolation(3, 6, "Initial star should be align with start of block comment"),
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a function that contains a block comment for which the initial stars of the block comment are not aligned then reformat`() {
        val code =
            """
            fun foo() {
                /*
                      * This blocked is not formatted well.
              * This blocked is not formatted well.
                    */
            }
            """.trimIndent()
        val formattedCode =
            """
            fun foo() {
                /*
                 * This blocked is not formatted well.
                 * This blocked is not formatted well.
                 */
            }
            """.trimIndent()
        blockCommentInitialStarAlignmentRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(3, 12, "Initial star should be align with start of block comment"),
                LintViolation(4, 4, "Initial star should be align with start of block comment"),
                LintViolation(5, 10, "Initial star should be align with start of block comment"),
            ).isFormattedAs(formattedCode)
    }
}
