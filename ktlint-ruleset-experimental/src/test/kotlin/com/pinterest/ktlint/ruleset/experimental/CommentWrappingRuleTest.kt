package com.pinterest.ktlint.ruleset.experimental

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThat
import com.pinterest.ktlint.test.LintViolation
import org.junit.jupiter.api.Test

class CommentWrappingRuleTest {
    private val commentWrappingRuleAssertThat = CommentWrappingRule().assertThat()

    @Test
    fun `Given a single line block comment that start starts and end on a separate line then do not reformat`() {
        val code =
            """
            /* Some comment */
            """.trimIndent()
        commentWrappingRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Given a multi line block comment that start starts and end on a separate line then do not reformat`() {
        val code =
            """
            /*
             * Some comment
             */
            """.trimIndent()
        commentWrappingRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Given a block comment followed by a code element on the same line as the block comment ended then split the elements with a new line`() {
        val code =
            """
            /* Some comment 1 */ val foo1 = "foo1"
            /* Some comment 2 */val foo2 = "foo2"
            /* Some comment 3 */ fun foo3() = "foo3"
            /* Some comment 4 */fun foo4() = "foo4"
            """.trimIndent()
        val formattedCode =
            """
            /* Some comment 1 */
            val foo1 = "foo1"
            /* Some comment 2 */
            val foo2 = "foo2"
            /* Some comment 3 */
            fun foo3() = "foo3"
            /* Some comment 4 */
            fun foo4() = "foo4"
            """.trimIndent()
        commentWrappingRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(1, 21, "A block comment may not be followed by any other element on that same line"),
                LintViolation(2, 21, "A block comment may not be followed by any other element on that same line"),
                LintViolation(3, 21, "A block comment may not be followed by any other element on that same line"),
                LintViolation(4, 21, "A block comment may not be followed by any other element on that same line")
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a block comment containing a newline which is preceded by another element on the same line then raise lint error but do not autocorrect`() {
        val code =
            """
            val foo = "foo" /* Some comment
                             * with a newline
                             */
            """.trimIndent()
        commentWrappingRuleAssertThat(code)
            .hasLintViolationWithoutAutoCorrect(1, 17, "A block comment after any other element on the same line must be separated by a new line")
    }

    @Test
    fun `Given a block comment that does not contain a newline and which is after some code om the same line is changed to an EOL comment`() {
        val code =
            """
            val foo = "foo" /* Some comment */
            """.trimIndent()
        val formattedCode =
            """
            val foo = "foo" // Some comment
            """.trimIndent()
        commentWrappingRuleAssertThat(code)
            .hasLintViolation(1, 16, "A single line block comment after a code element on the same line must be replaced with an EOL comment")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a block comment in between code elements on the same line then raise lint error but do not autocorrect`() {
        val code =
            """
            val foo /* some comment */ = "foo"
            """.trimIndent()
        commentWrappingRuleAssertThat(code)
            .hasLintViolationWithoutAutoCorrect(1, 9, "A block comment in between other elements on the same line is disallowed")
    }

    @Test
    fun `Given a block comment containing a new line and the block is preceded and followed by other code elements then raise lint errors but do not autocorrect`() {
        val code =
            """
            val foo /*
            some comment
            */ = "foo"
            """.trimIndent()
        commentWrappingRuleAssertThat(code)
            .hasLintViolationWithoutAutoCorrect(1, 9, "A block comment starting on same line as another element and ending on another line before another element is disallowed")
    }

    @Test
    fun `Given a block comment which is indented then keep that indent when wrapping the line`() {
        val code =
            """
            fun bar() {
                /* Some comment */ val foo = "foo"
            }
            """.trimIndent()
        val formattedCode =
            """
            fun bar() {
                /* Some comment */
                val foo = "foo"
            }
            """.trimIndent()
        commentWrappingRuleAssertThat(code)
            .hasLintViolation(2, 23, "A block comment may not be followed by any other element on that same line")
    }
}
