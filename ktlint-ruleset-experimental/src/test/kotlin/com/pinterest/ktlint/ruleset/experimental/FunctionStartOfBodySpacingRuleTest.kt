package com.pinterest.ktlint.ruleset.experimental

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRule
import com.pinterest.ktlint.test.LintViolation
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class FunctionStartOfBodySpacingRuleTest {
    private val functionStartOfBodySpacingRuleAssertThat = assertThatRule { FunctionStartOfBodySpacingRule() }

    @Nested
    @DisplayName("Given a function signature followed by an expression body")
    inner class ExpressionBody {
        @Test
        fun `Given that the signature contains required spacing then do not reformat`() {
            val code =
                """
                fun foo() = "some-result"
                fun bar(): String = "some-result"
                """.trimIndent()
            functionStartOfBodySpacingRuleAssertThat(code).hasNoLintViolations()
        }

        @Nested
        @DisplayName("Given the spacing before the equality sign")
        inner class SpacingBeforeEqualitySignInExpressionBody {
            @Test
            fun `Given that no space is found before the equals sign then reformat`() {
                val code =
                    """
                    fun foo()= "some-result"
                    fun bar(): String= "some-result"
                    """.trimIndent()
                val formattedCode =
                    """
                    fun foo() = "some-result"
                    fun bar(): String = "some-result"
                    """.trimIndent()
                functionStartOfBodySpacingRuleAssertThat(code)
                    .hasLintViolations(
                        LintViolation(1, 10, "Expected a single white space before assignment of expression body"),
                        LintViolation(2, 18, "Expected a single white space before assignment of expression body")
                    ).isFormattedAs(formattedCode)
            }

            @Test
            fun `Given that multiple spaces are found before the equals sign then reformat`() {
                val code =
                    """
                    fun foo()  = "some-result"
                    fun bar(): String  = "some-result"
                    """.trimIndent()
                val formattedCode =
                    """
                    fun foo() = "some-result"
                    fun bar(): String = "some-result"
                    """.trimIndent()
                functionStartOfBodySpacingRuleAssertThat(code)
                    .hasLintViolations(
                        LintViolation(1, 10, "Unexpected whitespace"),
                        LintViolation(2, 18, "Unexpected whitespace")
                    ).isFormattedAs(formattedCode)
            }

            @Test
            fun `Given that newline is found before the equals sign then reformat`() {
                val code =
                    """
                    fun foo()
                        = "some-result"
                    fun bar(): String
                        = "some-result"
                    """.trimIndent()
                val formattedCode =
                    """
                    fun foo() = "some-result"
                    fun bar(): String = "some-result"
                    """.trimIndent()
                functionStartOfBodySpacingRuleAssertThat(code)
                    .hasLintViolations(
                        LintViolation(1, 10, "Unexpected whitespace"),
                        LintViolation(3, 18, "Unexpected whitespace")
                    ).isFormattedAs(formattedCode)
            }
        }

        @Nested
        @DisplayName("Given the spacing after the equality sign")
        inner class SpacingAfterEqualitySignInExpressionBody {
            @Test
            fun `Given that no space is found between the equals sign and expression body on the same line then reformat`() {
                val code =
                    """
                    fun foo() ="some-result"
                    fun bar(): String ="some-result"
                    """.trimIndent()
                val formattedCode =
                    """
                    fun foo() = "some-result"
                    fun bar(): String = "some-result"
                    """.trimIndent()
                functionStartOfBodySpacingRuleAssertThat(code)
                    .hasLintViolations(
                        LintViolation(1, 11, "Expected a single white space between assignment and expression body on same line"),
                        LintViolation(2, 19, "Expected a single white space between assignment and expression body on same line")
                    ).isFormattedAs(formattedCode)
            }

            @Test
            fun `Given that multiple space are found between the equals sign and expression body on the same line then reformat`() {
                val code =
                    """
                    fun foo() =  "some-result"
                    fun bar(): String =  "some-result"
                    """.trimIndent()
                val formattedCode =
                    """
                    fun foo() = "some-result"
                    fun bar(): String = "some-result"
                    """.trimIndent()
                functionStartOfBodySpacingRuleAssertThat(code)
                    .hasLintViolations(
                        LintViolation(1, 12, "Unexpected whitespace"),
                        LintViolation(2, 20, "Unexpected whitespace")
                    ).isFormattedAs(formattedCode)
            }

            @Test
            fun `Given that a newline is found between the equals sign and expression body then do not reformat`() {
                val code =
                    """
                    fun foo() =
                        "some-result"
                    fun bar(): String =
                        "some-result"
                    """.trimIndent()
                functionStartOfBodySpacingRuleAssertThat(code).hasNoLintViolations()
            }
        }
    }

    @Nested
    @DisplayName("Given a function signature followed by a body block")
    inner class BodyBlock {
        @Test
        fun `Given that the signature contains required spacing then do not reformat`() {
            val code =
                """
                fun foo() {
                    // do something
                }
                fun bar(): String {
                    return "some-result"
                }
                """.trimIndent()
            functionStartOfBodySpacingRuleAssertThat(code).hasNoLintViolations()
        }

        @Test
        fun `Given that no space is found before the body block then reformat`() {
            val code =
                """
                fun foo(){
                    // do something
                }
                fun bar(): String{
                    return "some-result"
                }
                """.trimIndent()
            val formattedCode =
                """
                fun foo() {
                    // do something
                }
                fun bar(): String {
                    return "some-result"
                }
                """.trimIndent()
            functionStartOfBodySpacingRuleAssertThat(code)
                .hasLintViolations(
                    LintViolation(1, 10, "Expected a single white space before start of function body"),
                    LintViolation(4, 18, "Expected a single white space before start of function body")
                ).isFormattedAs(formattedCode)
        }

        @Test
        fun `Given that multiple spaces are found before the body block then reformat`() {
            val code =
                """
                fun foo()  {
                    // do something
                }
                fun bar(): String  {
                    return "some-result"
                }
                """.trimIndent()
            val formattedCode =
                """
                fun foo() {
                    // do something
                }
                fun bar(): String {
                    return "some-result"
                }
                """.trimIndent()
            functionStartOfBodySpacingRuleAssertThat(code)
                .hasLintViolations(
                    LintViolation(1, 10, "Unexpected whitespace"),
                    LintViolation(4, 18, "Unexpected whitespace")
                ).isFormattedAs(formattedCode)
        }

        @Test
        fun `Given that newline is found before the body block then reformat`() {
            val code =
                """
                fun foo()
                {
                    // do something
                }
                fun bar(): String
                {
                    return "some-result"
                }
                """.trimIndent()
            val formattedCode =
                """
                fun foo() {
                    // do something
                }
                fun bar(): String {
                    return "some-result"
                }
                """.trimIndent()
            functionStartOfBodySpacingRuleAssertThat(code)
                .hasLintViolations(
                    LintViolation(1, 10, "Unexpected whitespace"),
                    LintViolation(5, 18, "Unexpected whitespace")
                ).isFormattedAs(formattedCode)
        }
    }
}
