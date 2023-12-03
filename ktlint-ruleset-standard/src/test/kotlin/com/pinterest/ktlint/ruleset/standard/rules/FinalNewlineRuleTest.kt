package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.rule.engine.core.api.editorconfig.INSERT_FINAL_NEWLINE_PROPERTY
import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRule
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class FinalNewlineRuleTest {
    private val finalNewlineRuleAssertThat = assertThatRule { FinalNewlineRule() }

    @Nested
    inner class `Given that the final new line is required (default)` {
        @Test
        fun `Given an empty file then do not return lint errors`() {
            finalNewlineRuleAssertThat("")
                .withEditorConfigOverride(FINAL_NEW_LINE_REQUIRED)
                .hasNoLintViolations()
        }

        @Test
        fun `Given a file for which the final new line is missing`() {
            val code =
                """
                fun name() {
                }
                """.trimIndent()
            val formattedCode =
                """
                fun name() {
                }

                """.trimIndent()
            finalNewlineRuleAssertThat(code)
                .withEditorConfigOverride(FINAL_NEW_LINE_REQUIRED)
                .hasLintViolation(2, 1, "File must end with a newline (\\n)")
                .isFormattedAs(formattedCode)
        }

        @Test
        fun `Given a file ending with a single newline then do not return lint errors`() {
            val code =
                """
                fun name() {
                }

                """.trimIndent()
            finalNewlineRuleAssertThat(code)
                .withEditorConfigOverride(FINAL_NEW_LINE_REQUIRED)
                .hasNoLintViolations()
        }

        @Test
        fun `Given a file ending with multiple newlines then do not return lint errors`() {
            val code =
                """
                fun main() {
                }


                """.trimIndent()
            finalNewlineRuleAssertThat(code)
                .withEditorConfigOverride(FINAL_NEW_LINE_REQUIRED)
                .hasNoLintViolations()
        }
    }

    @Nested
    inner class `Given that the final new line is not required` {
        @Test
        fun `Given an empty file then do not return lint errors`() {
            finalNewlineRuleAssertThat("")
                .withEditorConfigOverride(FINAL_NEW_LINE_NOT_REQUIRED)
                .hasNoLintViolations()
        }

        @Test
        fun `Given an file without a final new line then do not return lint errors`() {
            val code =
                """
                fun name() {
                }
                """.trimIndent()
            finalNewlineRuleAssertThat(code)
                .withEditorConfigOverride(FINAL_NEW_LINE_NOT_REQUIRED)
                .hasNoLintViolations()
        }

        @Test
        fun `Given a file ending with a final new line`() {
            val code =
                """
                fun name() {
                }

                """.trimIndent()
            val formattedCode =
                """
                fun name() {
                }
                """.trimIndent()
            finalNewlineRuleAssertThat(code)
                .withEditorConfigOverride(FINAL_NEW_LINE_NOT_REQUIRED)
                .hasLintViolation(2, 2, "Redundant newline (\\n) at the end of file")
                .isFormattedAs(formattedCode)
        }
    }

    private companion object {
        val FINAL_NEW_LINE_REQUIRED = INSERT_FINAL_NEWLINE_PROPERTY to true
        val FINAL_NEW_LINE_NOT_REQUIRED =
            INSERT_FINAL_NEWLINE_PROPERTY to false
    }
}
