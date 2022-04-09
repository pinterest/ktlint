package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.core.LintError
import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThat
import com.pinterest.ktlint.test.MULTILINE_STRING_QUOTE
import com.pinterest.ktlint.test.format
import com.pinterest.ktlint.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class NoConsecutiveBlankLinesRuleTest {
    private val noConsecutiveBlankLinesRuleAssertThat = NoConsecutiveBlankLinesRule().assertThat()

    @Test
    fun `Given needless blank lines between declarations then do return lint errors`() {
        val code =
            """
            package com.test


            import com.test.util


            val a = "a"


            fun b() {
            }


            fun c()
            """.trimIndent()
        val formattedCode =
            """
            package com.test

            import com.test.util

            val a = "a"

            fun b() {
            }

            fun c()
            """.trimIndent()
        noConsecutiveBlankLinesRuleAssertThat(code)
            .hasLintErrors(
                LintError(3, 1, "no-consecutive-blank-lines", "Needless blank line(s)"),
                LintError(6, 1, "no-consecutive-blank-lines", "Needless blank line(s)"),
                LintError(9, 1, "no-consecutive-blank-lines", "Needless blank line(s)"),
                LintError(13, 1, "no-consecutive-blank-lines", "Needless blank line(s)")
            ).isFormattedAs(formattedCode)
    }

    @Nested
    inner class BlankLinesAtEndOfFile {
        @Test
        fun `Given two blank lines at end of file then do return lint errors`() {
            val code =
                """
                fun main() {
                }


                """.trimIndent()
            val formattedCode =
                """
                fun main() {
                }

                """.trimIndent()
            noConsecutiveBlankLinesRuleAssertThat(code)
                .hasLintErrors(
                    LintError(4, 1, "no-consecutive-blank-lines", "Needless blank line(s)")
                ).isFormattedAs(formattedCode)
        }

        @Test
        fun `Given three or more blank lines at end of file then do return lint errors`() {
            val code =
                """
                fun main() {
                }



                """.trimIndent()
            val formattedCode =
                """
                fun main() {
                }

                """.trimIndent()
            noConsecutiveBlankLinesRuleAssertThat(code)
                .hasLintErrors(
                    LintError(4, 1, "no-consecutive-blank-lines", "Needless blank line(s)")
                ).isFormattedAs(formattedCode)
        }
    }

    @Test
    fun `Given a string with multiple blank lines then do no return lint errors`() {
        val code =
            """
            fun main() {
                println(
                    $MULTILINE_STRING_QUOTE


                    $MULTILINE_STRING_QUOTE
                )
            }
            """.trimIndent()
        noConsecutiveBlankLinesRuleAssertThat(code).hasNoLintErrors()
    }

    @Nested
    inner class BlankLinesBetweenClassNameAndPrimaryConstructor {
        @Test
        fun `Given one or more blank line between the class name and primary constructor then do return a lint error`() {
            val code =
                """
                class A

                constructor(a: Int)

                class B


                private constructor(b: Int)
                """.trimIndent()
            val formattedCode =
                """
                class A
                constructor(a: Int)

                class B
                private constructor(b: Int)
                """.trimIndent()
            noConsecutiveBlankLinesRuleAssertThat(code)
                .hasLintErrors(
                    // TODO: Line number is incorrect
                    LintError(3, 1, "no-consecutive-blank-lines", "Needless blank line(s)"),
                    // TODO: Line number is incorrect
                    LintError(7, 1, "no-consecutive-blank-lines", "Needless blank line(s)")
                ).isFormattedAs(formattedCode)
        }

        @Test
        fun `Given a class followed bij an EOL comment and a blank line before the constructor then do no return a lint error`() {
            val code =
                """
                class A // comment

                constructor(a: Int)
                """.trimIndent()
            noConsecutiveBlankLinesRuleAssertThat(code)
                // TODO: Check why no error is reported here
                .hasNoLintErrors()
        }
    }

    @Test
    fun `should not raise NPE on linting Kotlin script file`() {
        assertThat(
            NoConsecutiveBlankLinesRule().lint(
                """
                import java.net.URI

                plugins {
                    `java-library`
                }
                """.trimIndent(),
                script = true
            )
        ).isEmpty()
    }

    @Test
    fun `should remove line in dot qualified expression`() {
        assertThat(
            NoConsecutiveBlankLinesRule().format(
                """
                fun foo(inputText: String) {
                    inputText


                        .toLowerCase()
                }
                """.trimIndent()
            )
        ).isEqualTo(
            """
            fun foo(inputText: String) {
                inputText
                    .toLowerCase()
            }
            """.trimIndent()
        )
    }
}
