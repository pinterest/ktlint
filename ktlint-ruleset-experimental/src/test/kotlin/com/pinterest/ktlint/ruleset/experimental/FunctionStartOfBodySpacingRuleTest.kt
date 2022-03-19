package com.pinterest.ktlint.ruleset.experimental

import com.pinterest.ktlint.core.LintError
import com.pinterest.ktlint.test.format
import com.pinterest.ktlint.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class FunctionStartOfBodySpacingRuleTest {
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
            assertThat(FunctionStartOfBodySpacingRule().lint(code)).isEmpty()
            assertThat(FunctionStartOfBodySpacingRule().format(code)).isEqualTo(code)
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
                assertThat(FunctionStartOfBodySpacingRule().lint(code)).containsExactly(
                    LintError(
                        1,
                        10,
                        "function-start-of-body-spacing",
                        "Expected a single white space before assignment of expression body"
                    ),
                    LintError(
                        2,
                        18,
                        "function-start-of-body-spacing",
                        "Expected a single white space before assignment of expression body"
                    )
                )
                assertThat(FunctionStartOfBodySpacingRule().format(code)).isEqualTo(formattedCode)
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
                assertThat(FunctionStartOfBodySpacingRule().lint(code)).containsExactly(
                    LintError(1, 10, "function-start-of-body-spacing", "Unexpected whitespace"),
                    LintError(2, 18, "function-start-of-body-spacing", "Unexpected whitespace")
                )
                assertThat(FunctionStartOfBodySpacingRule().format(code)).isEqualTo(formattedCode)
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
                assertThat(FunctionStartOfBodySpacingRule().lint(code)).containsExactly(
                    LintError(1, 10, "function-start-of-body-spacing", "Unexpected whitespace"),
                    LintError(3, 18, "function-start-of-body-spacing", "Unexpected whitespace")
                )
                assertThat(FunctionStartOfBodySpacingRule().format(code)).isEqualTo(formattedCode)
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
                assertThat(FunctionStartOfBodySpacingRule().lint(code)).containsExactly(
                    LintError(1, 11, "function-start-of-body-spacing", "Expected a single white space between assignment and expression body on same line"),
                    LintError(2, 19, "function-start-of-body-spacing", "Expected a single white space between assignment and expression body on same line")
                )
                assertThat(FunctionStartOfBodySpacingRule().format(code)).isEqualTo(formattedCode)
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
                assertThat(FunctionStartOfBodySpacingRule().lint(code)).containsExactly(
                    LintError(1, 12, "function-start-of-body-spacing", "Unexpected whitespace"),
                    LintError(2, 20, "function-start-of-body-spacing", "Unexpected whitespace")
                )
                assertThat(FunctionStartOfBodySpacingRule().format(code)).isEqualTo(formattedCode)
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
                assertThat(FunctionStartOfBodySpacingRule().lint(code)).isEmpty()
                assertThat(FunctionStartOfBodySpacingRule().format(code)).isEqualTo(code)
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
            assertThat(FunctionStartOfBodySpacingRule().lint(code)).isEmpty()
            assertThat(FunctionStartOfBodySpacingRule().format(code)).isEqualTo(code)
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
            assertThat(FunctionStartOfBodySpacingRule().lint(code)).containsExactly(
                LintError(1, 10, "function-start-of-body-spacing", "Expected a single white space before start of function body"),
                LintError(4, 18, "function-start-of-body-spacing", "Expected a single white space before start of function body")
            )
            assertThat(FunctionStartOfBodySpacingRule().format(code)).isEqualTo(formattedCode)
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
            assertThat(FunctionStartOfBodySpacingRule().lint(code)).containsExactly(
                LintError(1, 10, "function-start-of-body-spacing", "Unexpected whitespace"),
                LintError(4, 18, "function-start-of-body-spacing", "Unexpected whitespace")
            )
            assertThat(FunctionStartOfBodySpacingRule().format(code)).isEqualTo(formattedCode)
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
            assertThat(FunctionStartOfBodySpacingRule().lint(code)).containsExactly(
                LintError(1, 10, "function-start-of-body-spacing", "Unexpected whitespace"),
                LintError(5, 18, "function-start-of-body-spacing", "Unexpected whitespace")
            )
            assertThat(FunctionStartOfBodySpacingRule().format(code)).isEqualTo(formattedCode)
        }
    }
}
