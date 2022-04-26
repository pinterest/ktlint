package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThat
import com.pinterest.ktlint.test.LintViolation
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

const val ruleId = "no-line-break-before-assignment"

class NoLineBreakBeforeAssignmentRuleTest {
    private val noLineBreakBeforeAssignmentRuleAssertThat = NoLineBreakBeforeAssignmentRule().assertThat()

    @Test
    fun `Given a single line declaration then do not return a lint error`() {
        val code =
            """
            val valA = ""
            """.trimIndent()
        noLineBreakBeforeAssignmentRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Given a declaration with a linebreak between the equals sign and the value then do not return a lint error`() {
        val code =
            """
            val valA =
                  ""
            """.trimIndent()
        noLineBreakBeforeAssignmentRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Given a variable declaration with a newline before the equals sign`() {
        val code =
            """
            val valA
                  = ""
            """.trimIndent()
        val formattedCode =
            """
            val valA =
                  ""
            """.trimIndent()
        noLineBreakBeforeAssignmentRuleAssertThat(code)
            .hasLintViolation(2, 7, "Line break before assignment is not allowed")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a function declaration with a newline before the equals sign`() {
        val code =
            """
            fun funA()
                  = ""
            """.trimIndent()
        val formattedCode =
            """
            fun funA() =
                  ""
            """.trimIndent()
        noLineBreakBeforeAssignmentRuleAssertThat(code)
            .hasLintViolation(2, 7, "Line break before assignment is not allowed")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Issue 693 - Given a function declaration without space after the equals sign`() {
        val code =
            """
            fun a()
                    =f()
            """.trimIndent()
        val formattedCode =
            """
            fun a() =
                    f()
            """.trimIndent()
        noLineBreakBeforeAssignmentRuleAssertThat(code)
            // TODO: The error description is not correct
            .hasLintViolation(2, 9, "Line break before assignment is not allowed")
            .isFormattedAs(formattedCode)
    }

    @Nested
    inner class Comments {
        @Test
        fun `Given an EOL comment on a separate line between the function return type and the equals sign then do return a lint error`() {
            val code =
                """
                fun sum(a: Int, b: Int): Int
                    // comment
                    = a + b
                """.trimIndent()
            val formattedCode =
                """
                fun sum(a: Int, b: Int): Int =
                    // comment
                    a + b
                """.trimIndent()
            noLineBreakBeforeAssignmentRuleAssertThat(code)
                // TODO: The error description is not correct
                .hasLintViolation(3, 5, "Line break before assignment is not allowed")
                .isFormattedAs(formattedCode)
        }

        @Test
        fun `Given an EOL comment on same line as the function return type and the equals sign on the next line then do return a lint error`() {
            val code =
                """
                fun sum1(a: Int, b: Int): Int // comment
                    = a + b
                fun sum2(a: Int, b: Int): Int// comment
                    = a + b
                """.trimIndent()
            val formattedCode =
                """
                fun sum1(a: Int, b: Int): Int = // comment
                    a + b
                fun sum2(a: Int, b: Int): Int = // comment
                    a + b
                """.trimIndent()
            noLineBreakBeforeAssignmentRuleAssertThat(code)
                .hasLintViolations(
                    // TODO: The error description is not correct
                    LintViolation(2, 5, "Line break before assignment is not allowed"),
                    LintViolation(4, 5, "Line break before assignment is not allowed")
                ).isFormattedAs(formattedCode)
        }
    }

    @Test
    fun `Issue 1039 - Given a function declaration with a parameter having a linebreak before the equals sign of the default value then do return a lint error`() {
        val code =
            """
            fun test(b: Boolean?
            = null): Int = 3
            """.trimIndent()
        val formattedCode =
            """
            fun test(b: Boolean? =
            null): Int = 3
            """.trimIndent()
        noLineBreakBeforeAssignmentRuleAssertThat(code)
            .hasLintViolation(2, 1, "Line break before assignment is not allowed")
            .isFormattedAs(formattedCode)
    }
}
