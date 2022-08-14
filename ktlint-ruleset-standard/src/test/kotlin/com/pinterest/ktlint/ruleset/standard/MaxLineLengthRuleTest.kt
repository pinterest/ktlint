package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.core.api.DefaultEditorConfigProperties.maxLineLengthProperty
import com.pinterest.ktlint.ruleset.standard.MaxLineLengthRule.Companion.ignoreBackTickedIdentifierProperty
import com.pinterest.ktlint.test.KtLintAssertThat.Companion.EOL_CHAR
import com.pinterest.ktlint.test.KtLintAssertThat.Companion.MAX_LINE_LENGTH_MARKER
import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRule
import com.pinterest.ktlint.test.LintViolation
import com.pinterest.ktlint.test.MULTILINE_STRING_QUOTE
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class MaxLineLengthRuleTest {
    private val maxLineLengthRuleAssertThat = assertThatRule { MaxLineLengthRule() }

    @DisplayName("Given code that exceeds the max line length but for which no lint error should be returned")
    @Nested
    inner class LintError {
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
                    LintViolation(2, 1, "Exceeded max line length (46)"),
                    LintViolation(3, 1, "Exceeded max line length (46)"),
                    LintViolation(5, 1, "Exceeded max line length (46)"),
                )
        }
    }

    @DisplayName("Given code that exceeds the max line length but for which no lint error should be returned")
    @Nested
    inner class NoLintErrorWhenExceedingMaxLineLength {
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
    inner class ErrorSuppression {
        @Test
        fun `Given some code followed by a ktlint-disable directive which causes the line length to be exceeded then do not return a lint error for that line`() {
            val code =
                """
                // $MAX_LINE_LENGTH_MARKER          $EOL_CHAR
                val bar = "bar" // ktlint-disable some-rule-id
                """.trimIndent()
            maxLineLengthRuleAssertThat(code)
                .setMaxLineLength()
                .hasNoLintViolations()
        }

        @Test
        fun `Given code that is wrapped into a ktlint-disable block then do no return lint errors for lines in this block`() {
            val code =
                """
                // $MAX_LINE_LENGTH_MARKER          $EOL_CHAR
                fun foo() {
                    println("teeeeeeeeeeeeeeeeeeeext")
                    /* ktlint-disable max-line-length */
                    println("teeeeeeeeeeeeeeeeeeeext")
                    println("teeeeeeeeeeeeeeeeeeeext")
                    /* ktlint-enable max-line-length */
                    println("teeeeeeeeeeeeeeeeeeeext")
                }
                """.trimIndent()
            maxLineLengthRuleAssertThat(code)
                .setMaxLineLength()
                .hasLintViolationsWithoutAutoCorrect(
                    LintViolation(3, 1, "Exceeded max line length (37)"),
                    LintViolation(8, 1, "Exceeded max line length (37)"),
                )
        }
    }

    @Nested
    inner class BacktickedStrings {
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
                .withEditorConfigOverride(ignoreBackTickedIdentifierProperty to true)
                // Note that no error was generated on line 2 with the long fun name but on another line
                .hasLintViolationWithoutAutoCorrect(4, 1, "Exceeded max line length (37)")
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
                .withEditorConfigOverride(ignoreBackTickedIdentifierProperty to true)
                .hasLintViolationsWithoutAutoCorrect(
                    // Note that no error was generated on line 2 with the long fun name but on another line
                    LintViolation(3, 1, "Exceeded max line length (37)"),
                    LintViolation(4, 1, "Exceeded max line length (37)"),
                )
        }
    }

    @Test
    fun testLintOff() {
        val code = "// some" + " long ".repeat(100) + "comment" // Total length of line is 7 + 600 + 7 = 614 characters
        maxLineLengthRuleAssertThat(code)
            .withEditorConfigOverride(maxLineLengthProperty to "off")
            .hasNoLintViolations()
    }

    @Test
    fun testRangeSearch() {
        for (i in 0 until 10) {
            assertThat(RangeTree((0..i).toList()).query(Int.MIN_VALUE, Int.MAX_VALUE).toString())
                .isEqualTo((0..i).toList().toString())
        }
        assertThat(RangeTree(emptyList()).query(1, 5).toString()).isEqualTo("[]")
        assertThat(RangeTree((5 until 10).toList()).query(1, 5).toString()).isEqualTo("[]")
        assertThat(RangeTree((5 until 10).toList()).query(3, 7).toString()).isEqualTo("[5, 6]")
        assertThat(RangeTree((5 until 10).toList()).query(7, 12).toString()).isEqualTo("[7, 8, 9]")
        assertThat(RangeTree((5 until 10).toList()).query(10, 15).toString()).isEqualTo("[]")
        assertThat(RangeTree(listOf(1, 5, 10)).query(3, 4).toString()).isEqualTo("[]")
    }
}
