package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.rule.engine.core.api.editorconfig.MAX_LINE_LENGTH_PROPERTY
import com.pinterest.ktlint.ruleset.standard.rules.MaxLineLengthRule.Companion.IGNORE_BACKTICKED_IDENTIFIER_PROPERTY
import com.pinterest.ktlint.test.KtLintAssertThat.Companion.EOL_CHAR
import com.pinterest.ktlint.test.KtLintAssertThat.Companion.MAX_LINE_LENGTH_MARKER
import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRule
import com.pinterest.ktlint.test.LintViolation
import com.pinterest.ktlint.test.MULTILINE_STRING_QUOTE
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class MaxLineLengthRuleTest {
    private val maxLineLengthRuleAssertThat = assertThatRule { MaxLineLengthRule() }

    @Test
    fun `Given some code that exceeds the max line length then return a lint error`() {
        val code =
            """
            // $MAX_LINE_LENGTH_MARKER                   $EOL_CHAR
            val fooooooooooooooo = "fooooooooooooooooooooo"
            val foooooooooooooo = "foooooooooooooooooooo" // some comment
            val fooooooooooooo =
                "foooooooooooooooooooooooooooooooooooooooo"
            """.trimIndent()
        maxLineLengthRuleAssertThat(code)
            .setMaxLineLength()
            .hasLintViolationsWithoutAutoCorrect(
                LintViolation(2, 47, "Exceeded max line length (46)"),
                LintViolation(3, 61, "Exceeded max line length (46)"),
                LintViolation(5, 47, "Exceeded max line length (46)"),
            )
    }

    @Nested
    inner class `Given code that exceeds the max line length but for which no lint error should be returned` {
        @Test
        fun `Given a package statement that that exceeds the max line length then do not return a lint error`() {
            val code =
                """
                // $MAX_LINE_LENGTH_MARKER      $EOL_CHAR
                package com.toooooooooooooooo.long
                """.trimIndent()
            maxLineLengthRuleAssertThat(code)
                .setMaxLineLength()
                .hasNoLintViolations()
        }

        @Test
        fun `Given an import statement that that exceeds the max line length then do not return a lint error`() {
            val code =
                """
                // $MAX_LINE_LENGTH_MARKER      $EOL_CHAR
                import com.toooooooooooooooo.long
                """.trimIndent()
            maxLineLengthRuleAssertThat(code)
                .setMaxLineLength()
                .hasNoLintViolations()
        }

        @Test
        fun `Given a multiline string which exceeds the max line length then do not return a lint error`() {
            val code =
                """
                // $MAX_LINE_LENGTH_MARKER      $EOL_CHAR
                val foo =
                    $MULTILINE_STRING_QUOTE
                    foooooooooooooooooooooooooooooooooo
                    $MULTILINE_STRING_QUOTE
                """.trimIndent()
            maxLineLengthRuleAssertThat(code)
                .setMaxLineLength()
                .hasNoLintViolations()
        }

        @Test
        fun `Given a block comment which exceeds the max line length then do not return a lint error`() {
            val code =
                """
                // $MAX_LINE_LENGTH_MARKER      $EOL_CHAR
                /*
                 * This block comment exceeds the max line length
                 */
                """.trimIndent()
            maxLineLengthRuleAssertThat(code)
                .setMaxLineLength()
                .hasNoLintViolations()
        }

        @Test
        fun `Given a KDoc which exceeds the max line length then do not return a lint error`() {
            val code =
                """
                // $MAX_LINE_LENGTH_MARKER      $EOL_CHAR
                /**
                 * This block comment exceeds the max line length
                 */
                """.trimIndent()
            maxLineLengthRuleAssertThat(code)
                .setMaxLineLength()
                .hasNoLintViolations()
        }
    }

    @Nested
    inner class `Given some error suppression` {
        @Test
        fun `Given code that is suppressed then do no return lint errors for lines in this block`() {
            val code =
                """
                // $MAX_LINE_LENGTH_MARKER                     $EOL_CHAR
                fun foo() {
                    println("teeeeeeeeeeeeeeeeeeeeeeeeeeeeeeext")
                    @Suppress("ktlint:standard:max-line-length")
                    println("teeeeeeeeeeeeeeeeeeeeeeeeeeeeeeext")
                    println("teeeeeeeeeeeeeeeeeeeeeeeeeeeeeeext")
                }
                """.trimIndent()
            maxLineLengthRuleAssertThat(code)
                .setMaxLineLength()
                .hasLintViolationsWithoutAutoCorrect(
                    LintViolation(3, 49, "Exceeded max line length (48)"),
                    LintViolation(6, 49, "Exceeded max line length (48)"),
                )
        }
    }

    @Nested
    inner class `Given a string between backticks` {
        @Test
        fun `Given some text wrapped between backticks which causes the line length to be exceeded then do not return a lint error for that line`() {
            val code =
                """
                // $MAX_LINE_LENGTH_MARKER          $EOL_CHAR
                @Test
                fun `Some too long test description between backticks`() {
                    println("teeeeeeeeeeeeeeeeeeeext")
                }
                """.trimIndent()
            maxLineLengthRuleAssertThat(code)
                .setMaxLineLength()
                .withEditorConfigOverride(IGNORE_BACKTICKED_IDENTIFIER_PROPERTY to true)
                // Note that no error was generated on line 2 with the long fun name but on another line
                .hasLintViolationWithoutAutoCorrect(4, 38, "Exceeded max line length (37)")
        }

        @Test
        fun `Given some text wrapped between backticks which is ignored but still the line length is exceeded with remainder of text then do return a lint error`() {
            val code =
                """
                // $MAX_LINE_LENGTH_MARKER          $EOL_CHAR
                @Test
                fun `Some too long test description between backticks`(looooooooongParameterName: String) {
                    println("teeeeeeeeeeeeeeeeeeeext")
                }
                """.trimIndent()
            maxLineLengthRuleAssertThat(code)
                .setMaxLineLength()
                .withEditorConfigOverride(IGNORE_BACKTICKED_IDENTIFIER_PROPERTY to true)
                .hasLintViolationsWithoutAutoCorrect(
                    // Note that no error was generated on line 2 with the long fun name but on another line
                    LintViolation(3, 91, "Exceeded max line length (37)"),
                    LintViolation(4, 38, "Exceeded max line length (37)"),
                )
        }
    }

    @Test
    fun testLintOff() {
        val code = "// some" + " long ".repeat(100) + "comment" // Total length of line is 7 + 600 + 7 = 614 characters
        maxLineLengthRuleAssertThat(code)
            .withEditorConfigOverride(MAX_LINE_LENGTH_PROPERTY to "off")
            .hasNoLintViolations()
    }

    @Test
    fun `Given a block comment at the start of the file`() {
        val code =
            // The MAX_LINE_LENGTH_MARKER comment can not be used in this test as that comment should be the first line in the code. But for
            // test it is required that the block comment is the first comment in the code.
            """
            /*
             * Some comment for which the individual
             * lines do not exceed the max line, but
             * the total length of the comment does.
             */
            """.trimIndent()
        maxLineLengthRuleAssertThat(code)
            .withEditorConfigOverride(MAX_LINE_LENGTH_PROPERTY to 40)
            .hasNoLintViolations()
    }
}
