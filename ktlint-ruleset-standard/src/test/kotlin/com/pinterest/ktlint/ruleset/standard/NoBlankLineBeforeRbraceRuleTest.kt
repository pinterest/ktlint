package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.core.LintError
import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThat
import com.pinterest.ktlint.test.MULTILINE_STRING_QUOTE
import org.junit.jupiter.api.Test

class NoBlankLineBeforeRbraceRuleTest {
    private val noBlankLineBeforeRbraceRuleAssertThat = NoBlankLineBeforeRbraceRule().assertThat()

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
            .hasLintErrors(
                LintError(3, 1, "no-blank-line-before-rbrace", "Unexpected blank line(s) before \"}\""),
                LintError(6, 1, "no-blank-line-before-rbrace", "Unexpected blank line(s) before \"}\"")
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
            .hasLintErrors(
                LintError(2, 1, "no-blank-line-before-rbrace", "Unexpected blank line(s) before \"}\"")
            ).isFormattedAs(formattedCode)
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
            .hasNoLintErrors()
    }
}
