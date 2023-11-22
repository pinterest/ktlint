package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.rule.engine.core.api.IndentConfig
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.INDENT_STYLE_PROPERTY
import com.pinterest.ktlint.ruleset.standard.StandardRuleSetProvider
import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRuleBuilder
import com.pinterest.ktlint.test.LintViolation
import com.pinterest.ktlint.test.MULTILINE_STRING_QUOTE
import com.pinterest.ktlint.test.TAB
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class StringTemplateIndentRuleTest {
    private val stringTemplateIndentRuleAssertThat =
        assertThatRuleBuilder { StringTemplateIndentRule() }
            .addRequiredRuleProviderDependenciesFrom(StandardRuleSetProvider())
            .assertThat()

    @Test
    fun `Do not move a multiline string literal after return statement to a new line as that results in a compilation error`() {
        val code =
            """
            fun someFunction() {
                return $MULTILINE_STRING_QUOTE
                       someText
                       $MULTILINE_STRING_QUOTE.trimIndent()
            }
            """.trimIndent()
        val formattedCode =
            """
            fun someFunction() {
                return $MULTILINE_STRING_QUOTE
                    someText
                    $MULTILINE_STRING_QUOTE.trimIndent()
            }
            """.trimIndent()
        stringTemplateIndentRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(3, 12, "Unexpected indent of raw string literal"),
                LintViolation(4, 12, "Unexpected indent of raw string literal"),
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Move a multiline string literal (indented with tabs) as body expression`() {
        val code =
            """
            fun someFunction() = $MULTILINE_STRING_QUOTE
            ${TAB}${TAB}${TAB}${TAB}someText
            ${TAB}${TAB}${TAB}${TAB}$MULTILINE_STRING_QUOTE.trimIndent()
            """.trimIndent()
        val formattedCode =
            """
            fun someFunction() =
            ${TAB}$MULTILINE_STRING_QUOTE
            ${TAB}someText
            ${TAB}$MULTILINE_STRING_QUOTE.trimIndent()
            """.trimIndent()
        stringTemplateIndentRuleAssertThat(code)
            .withEditorConfigOverride(INDENT_STYLE_PROPERTY to IndentConfig.IndentStyle.TAB)
            .hasLintViolations(
                LintViolation(2, 5, "Unexpected indent of raw string literal"),
                LintViolation(3, 5, "Unexpected indent of raw string literal"),
            ).isFormattedAs(formattedCode)
    }

    @Nested
    inner class `A multiline raw string literal containing both tabs and spaces in the indentation margin can not be autocorrected` {
        @Suppress("RemoveCurlyBracesFromTemplate")
        @Test
        fun `Mixed indentation characters on inner lines`() {
            val code =
                """
                val foo = $MULTILINE_STRING_QUOTE
                      line1
                ${TAB} line2
                    $MULTILINE_STRING_QUOTE.trimIndent()
                """.trimIndent()
            stringTemplateIndentRuleAssertThat(code).hasLintViolationWithoutAutoCorrect(
                1,
                11,
                "Indentation of multiline raw string literal should not contain both tab(s) and space(s)",
            )
        }

        @Suppress("RemoveCurlyBracesFromTemplate", "RemoveCurlyBracesFromTemplate")
        @Test
        fun `Mixed indentation characters including line with opening quotes`() {
            val code =
                """
                val foo = $MULTILINE_STRING_QUOTE${TAB}line1
                    ${TAB} line2
                    $MULTILINE_STRING_QUOTE.trimIndent()
                """.trimIndent()
            stringTemplateIndentRuleAssertThat(code).hasLintViolationWithoutAutoCorrect(
                1,
                11,
                "Indentation of multiline raw string literal should not contain both tab(s) and space(s)",
            )
        }

        @Suppress("RemoveCurlyBracesFromTemplate")
        @Test
        fun `Mixed indentation characters including line with closing quotes`() {
            val code =
                """
                val foo = $MULTILINE_STRING_QUOTE
                    ${TAB}line1
                    ${TAB} line2$MULTILINE_STRING_QUOTE.trimIndent()
                """.trimIndent()
            stringTemplateIndentRuleAssertThat(code).hasLintViolationWithoutAutoCorrect(
                1,
                11,
                "Indentation of multiline raw string literal should not contain both tab(s) and space(s)",
            )
        }
    }

    @Test
    fun `A multiline raw string literal should not contain text on the same line as the opening quotes`() {
        val code =
            """
            val someVar =
                ${MULTILINE_STRING_QUOTE}line1
                line2
                $MULTILINE_STRING_QUOTE.trimIndent()
            """.trimIndent()
        val formattedCode =
            """
            val someVar =
                $MULTILINE_STRING_QUOTE
                line1
                line2
                $MULTILINE_STRING_QUOTE.trimIndent()
            """.trimIndent()
        stringTemplateIndentRuleAssertThat(code)
            .hasLintViolation(2, 13, "Missing newline after the opening quotes of the raw string literal")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `A multiline raw string literal can contain text on the same line as the closing quotes`() {
        val code =
            """
            val someVar = $MULTILINE_STRING_QUOTE
                          line1
                          line2$MULTILINE_STRING_QUOTE.trimIndent()
            """.trimIndent()
        val formattedCode =
            """
            val someVar =
                $MULTILINE_STRING_QUOTE
                line1
                line2
                $MULTILINE_STRING_QUOTE.trimIndent()
            """.trimIndent()
        stringTemplateIndentRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(2, 15, "Unexpected indent of raw string literal"),
                LintViolation(3, 15, "Unexpected indent of raw string literal"),
                LintViolation(3, 20, "Missing newline before the closing quotes of the raw string literal"),
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Move a multiline raw string literal assignment to a variable to a new line`() {
        val code =
            """
            val someVar = $MULTILINE_STRING_QUOTE
                          someText
                          $MULTILINE_STRING_QUOTE.trimIndent()
            """.trimIndent()
        val formattedCode =
            """
            val someVar =
                $MULTILINE_STRING_QUOTE
                someText
                $MULTILINE_STRING_QUOTE.trimIndent()
            """.trimIndent()
        stringTemplateIndentRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(2, 15, "Unexpected indent of raw string literal"),
                LintViolation(3, 15, "Unexpected indent of raw string literal"),
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Move a multiline string literal in a function parameter to a new line`() {
        val code =
            """
            fun someFunction() {
                println($MULTILINE_STRING_QUOTE
                        someText
                        $MULTILINE_STRING_QUOTE.trimIndent())
            }
            """.trimIndent()
        val formattedCode =
            """
            fun someFunction() {
                println(
                    $MULTILINE_STRING_QUOTE
                    someText
                    $MULTILINE_STRING_QUOTE.trimIndent()
                )
            }
            """.trimIndent()
        stringTemplateIndentRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(3, 13, "Unexpected indent of raw string literal"),
                LintViolation(4, 13, "Unexpected indent of raw string literal"),
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Format a multiline string literal with text starting at position 1 of the line`() {
        val code =
            """
            fun foo() {
                println($MULTILINE_STRING_QUOTE
            Some text starting at the beginning of the line

                Some text not starting at the beginning of the line
            $MULTILINE_STRING_QUOTE.trimIndent()
                )
            }
            """.trimIndent()
        val formattedCode =
            """
            fun foo() {
                println(
                    $MULTILINE_STRING_QUOTE
                    Some text starting at the beginning of the line

                        Some text not starting at the beginning of the line
                    $MULTILINE_STRING_QUOTE.trimIndent()
                )
            }
            """.trimIndent()
        stringTemplateIndentRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(3, 1, "Unexpected indent of raw string literal"),
                LintViolation(5, 1, "Unexpected indent of raw string literal"),
                LintViolation(6, 1, "Unexpected indent of raw string literal"),
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Format multiline string with unexpected tabs in indentation margin`() {
        val code =
            """
            val str = $MULTILINE_STRING_QUOTE
            ${TAB}line1
            ${TAB}${TAB}line2
            $MULTILINE_STRING_QUOTE.trimIndent()
            """.trimIndent()
        val formattedCode =
            """
            val str =
                $MULTILINE_STRING_QUOTE
                line1
                ${TAB}line2
                $MULTILINE_STRING_QUOTE.trimIndent()
            """.trimIndent()
        stringTemplateIndentRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(2, 1, "Unexpected 'tab' character(s) in margin of multiline string"),
                LintViolation(3, 1, "Unexpected 'tab' character(s) in margin of multiline string"),
                LintViolation(4, 1, "Unexpected indent of raw string literal"),
            ).isFormattedAs(formattedCode)
    }

    @Suppress("RemoveCurlyBracesFromTemplate")
    @Test
    fun `Format multiline string without tabs in the indentation margin`() {
        val code =
            """
            val str = $MULTILINE_STRING_QUOTE
                  ${TAB}Tab at the beginning of this line but after the indentation margin
                  Tab${TAB}in the middle of this string
                  Tab at the end of this line.${TAB}
            $MULTILINE_STRING_QUOTE.trimIndent()
            """.trimIndent()
        val formattedCode =
            """
            val str =
                $MULTILINE_STRING_QUOTE
                ${TAB}Tab at the beginning of this line but after the indentation margin
                Tab${TAB}in the middle of this string
                Tab at the end of this line.${TAB}
                $MULTILINE_STRING_QUOTE.trimIndent()
            """.trimIndent()
        stringTemplateIndentRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(2, 7, "Unexpected indent of raw string literal"),
                LintViolation(3, 7, "Unexpected indent of raw string literal"),
                LintViolation(4, 7, "Unexpected indent of raw string literal"),
                LintViolation(5, 1, "Unexpected indent of raw string literal"),
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `lint property delegate is indented properly 1`() {
        val code =
            """
            val string: String
                by lazy { $MULTILINE_STRING_QUOTE
                          someText
                          $MULTILINE_STRING_QUOTE.trimIndent()
                }
            """.trimIndent()
        val formattedCode =
            """
            val string: String
                by lazy {
                    $MULTILINE_STRING_QUOTE
                    someText
                    $MULTILINE_STRING_QUOTE.trimIndent()
                }
            """.trimIndent()
        stringTemplateIndentRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(3, 15, "Unexpected indent of raw string literal"),
                LintViolation(4, 15, "Unexpected indent of raw string literal"),
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `lint property delegate is indented properly 4`() {
        val code =
            """
            fun lazyString() = lazy { $MULTILINE_STRING_QUOTE
                                      someText
                                      $MULTILINE_STRING_QUOTE.trimIndent()
                                    }
            """.trimIndent()
        val formattedCode =
            """
            fun lazyString() =
                lazy {
                    $MULTILINE_STRING_QUOTE
                    someText
                    $MULTILINE_STRING_QUOTE.trimIndent()
                }
            """.trimIndent()
        stringTemplateIndentRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(2, 27, "Unexpected indent of raw string literal"),
                LintViolation(3, 27, "Unexpected indent of raw string literal"),
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `lint parameter with default multiline string raw string literal`() {
        val code =
            """
            data class SomeDataClass(
                val string: String = $MULTILINE_STRING_QUOTE
                                     someText
                                     $MULTILINE_STRING_QUOTE.trimIndent(),
                val int: Int
            )
            """.trimIndent()
        val formattedCode =
            """
            data class SomeDataClass(
                val string: String =
                    $MULTILINE_STRING_QUOTE
                    someText
                    $MULTILINE_STRING_QUOTE.trimIndent(),
                val int: Int
            )
            """.trimIndent()
        stringTemplateIndentRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(3, 26, "Unexpected indent of raw string literal"),
                LintViolation(4, 26, "Unexpected indent of raw string literal"),
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `lint parameter with multiline string raw string literal after arrow`() {
        val code =
            """
            val result = when {
                someBooleanFunction() -> $MULTILINE_STRING_QUOTE
                                         someText
                                         $MULTILINE_STRING_QUOTE.trimIndent()
                else -> $MULTILINE_STRING_QUOTE
                        someOtherText
                        $MULTILINE_STRING_QUOTE.trimIndent()
            }
            """.trimIndent()
        val formattedCode =
            """
            val result =
                when {
                    someBooleanFunction() ->
                        $MULTILINE_STRING_QUOTE
                        someText
                        $MULTILINE_STRING_QUOTE.trimIndent()
                    else ->
                        $MULTILINE_STRING_QUOTE
                        someOtherText
                        $MULTILINE_STRING_QUOTE.trimIndent()
                }
            """.trimIndent()
        stringTemplateIndentRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(3, 30, "Unexpected indent of raw string literal"),
                LintViolation(4, 30, "Unexpected indent of raw string literal"),
                LintViolation(6, 13, "Unexpected indent of raw string literal"),
                LintViolation(7, 13, "Unexpected indent of raw string literal"),
            ).isFormattedAs(formattedCode)
    }
}
