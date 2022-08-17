package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRule
import com.pinterest.ktlint.test.LintViolation
import com.pinterest.ktlint.test.MULTILINE_STRING_QUOTE
import org.junit.jupiter.api.Test

class NoBlankLineBeforeRbraceRuleTest {
    private val noBlankLineBeforeRbraceRuleAssertThat = assertThatRule { NoBlankLineBeforeRbraceRule() }

    @Test
    fun `Given some nested functions with empty line before closing brace then do return lint errors`() {
        val code =
            """
            fun main() {
                fun a() {

                }
                fun b()

            }
            """.trimIndent()
        val formattedCode =
            """
            fun main() {
                fun a() {
                }
                fun b()
            }
            """.trimIndent()
        noBlankLineBeforeRbraceRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(3, 1, "Unexpected blank line(s) before \"}\""),
                LintViolation(6, 1, "Unexpected blank line(s) before \"}\""),
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given some class with empty line before closing brace then do return lint errors`() {
        val code =
            """
            class A {

            }
            """.trimIndent()
        val formattedCode =
            """
            class A {
            }
            """.trimIndent()
        noBlankLineBeforeRbraceRuleAssertThat(code)
            .hasLintViolation(2, 1, "Unexpected blank line(s) before \"}\"")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given some string containing an empty line before a right brace then do not return a lint error`() {
        val code =
            """
            fun main() {
                println(
                    $MULTILINE_STRING_QUOTE

                    }
                    $MULTILINE_STRING_QUOTE
                )
            }
            """.trimIndent()
        noBlankLineBeforeRbraceRuleAssertThat(code)
            .hasNoLintViolations()
    }
}
