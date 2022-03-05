package com.pinterest.ktlint.ruleset.experimental

import com.pinterest.ktlint.core.LintError
import com.pinterest.ktlint.test.format
import com.pinterest.ktlint.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class BlockCommentInitialStarTest {
    @Test
    fun `Given a block comment for which the indentation followed by the star already aligns with the star in the first line of the block comment then do not reformat`() {
        val code =
            """
            /*
             * This blocked is formatted well.
             */
            """.trimIndent()
        assertThat(BlockCommentInitialStarAlignmentRule().lint(code)).isEmpty()
        assertThat(BlockCommentInitialStarAlignmentRule().format(code)).isEqualTo(code)
    }

    @Test
    fun `Given a block comment with a line that does not have an initial star then do not reformat that line`() {
        val code =
            """
            /*
                      This blocked is formatted well.
             */
            """.trimIndent()
        assertThat(BlockCommentInitialStarAlignmentRule().lint(code)).isEmpty()
        assertThat(BlockCommentInitialStarAlignmentRule().format(code)).isEqualTo(code)
    }

    @Test
    fun `Given a block comment with a line that contains a star which is preceded by a non-space or non-tab character then do not reformat that line`() {
        val code =
            """
            /*
                   - This line contains a * but it is not the initial *.
             */
            """.trimIndent()
        assertThat(BlockCommentInitialStarAlignmentRule().lint(code)).isEmpty()
        assertThat(BlockCommentInitialStarAlignmentRule().format(code)).isEqualTo(code)
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
        assertThat(BlockCommentInitialStarAlignmentRule().lint(code)).containsExactly(
            LintError(2, 8, "block-comment-initial-star-alignment", "Initial star should be align with start of block comment"),
            LintError(3, 6, "block-comment-initial-star-alignment", "Initial star should be align with start of block comment")
        )
        assertThat(BlockCommentInitialStarAlignmentRule().format(code)).isEqualTo(formattedCode)
    }

    @Test
    fun `Given function contain a block comment for which the initial stars of the block comment are not aligned then reformat`() {
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
        assertThat(BlockCommentInitialStarAlignmentRule().lint(code)).containsExactly(
            LintError(3, 12, "block-comment-initial-star-alignment", "Initial star should be align with start of block comment"),
            LintError(4, 4, "block-comment-initial-star-alignment", "Initial star should be align with start of block comment"),
            LintError(5, 10, "block-comment-initial-star-alignment", "Initial star should be align with start of block comment")
        )
        assertThat(BlockCommentInitialStarAlignmentRule().format(code)).isEqualTo(formattedCode)
    }
}
