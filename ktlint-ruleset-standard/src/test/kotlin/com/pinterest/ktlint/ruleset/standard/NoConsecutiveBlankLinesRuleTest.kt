package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.core.LintError
import com.pinterest.ktlint.test.format
import com.pinterest.ktlint.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class NoConsecutiveBlankLinesRuleTest {

    @Test
    fun testLintInDeclarations() {
        assertThat(
            NoConsecutiveBlankLinesRule().lint(
                """fun a() {

            }


            fun b() {

            }"""
            )
        ).isEqualTo(
            listOf(
                LintError(5, 1, "no-consecutive-blank-lines", "Needless blank line(s)")
            )
        )
    }

    @Test
    fun testLintInCode() {
        assertThat(
            NoConsecutiveBlankLinesRule().lint(
                """fun main() {
                fun a()
                fun b()


                fun c()
            }"""
            )
        ).isEqualTo(
            listOf(
                LintError(5, 1, "no-consecutive-blank-lines", "Needless blank line(s)")
            )
        )
    }

    @Test
    fun testLintAtTheEndOfFile() {
        assertThat(
            NoConsecutiveBlankLinesRule().lint(
                """
                fun main() {
                }


                """.trimIndent()
            )
        ).isEqualTo(
            listOf(
                LintError(4, 1, "no-consecutive-blank-lines", "Needless blank line(s)")
            )
        )
    }

    @Test
    fun testLintInString() {
        assertThat(
            NoConsecutiveBlankLinesRule().lint(
                "fun main() {println(\"\"\"\n\n\n\"\"\")}"
            )
        ).isEmpty()
    }

    @Test
    fun testFormatInDeclarations() {
        assertThat(
            NoConsecutiveBlankLinesRule().format(
                """
            fun a() {

            }


            fun b() {

            }
            """
            )
        ).isEqualTo(
            """
            fun a() {

            }

            fun b() {

            }
            """
        )
    }

    @Test
    fun testFormatInCode() {
        assertThat(
            NoConsecutiveBlankLinesRule().format(
                """
            fun main() {
                fun a()
                fun b()


                fun c()

            }
            """
            )
        ).isEqualTo(
            """
            fun main() {
                fun a()
                fun b()

                fun c()

            }
            """
        )
    }

    @Test
    fun testFormatAtTheEndOfFile() {
        assertThat(NoConsecutiveBlankLinesRule().format("class A\n\n\n")).isEqualTo("class A\n")
        assertThat(NoConsecutiveBlankLinesRule().format("class A\n\n")).isEqualTo("class A\n")
        assertThat(NoConsecutiveBlankLinesRule().format("class A\n")).isEqualTo("class A\n")
    }
}
