package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.core.LintError
import com.pinterest.ktlint.test.format
import com.pinterest.ktlint.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.testng.annotations.Test

class ArgumentListWrappingRuleTest {

    @Test
    fun testLintCallArgumentList() {
        assertThat(
            ArgumentListWrappingRule().lint(
                """
                fun main() {
                    f(paramA,
                        paramB,
                           paramC,
                        paramD)
                }
                """.trimIndent()
            )
        ).isEqualTo(
            listOf(
                LintError(2, 6, "argument-list-wrapping", """Missing newline after "(""""),
                LintError(4, 12, "argument-list-wrapping", "Unexpected indentation (expected 8, actual 11)"),
                LintError(5, 15, "argument-list-wrapping", """Missing newline before ")"""")
            )
        )
    }

    @Test
    fun testLintCallArgumentWhenMaxLineLengthExceeded() {
        assertThat(
            ArgumentListWrappingRule().lint(
                """
                fun main() {
                    f(paramA, paramB, paramC, paramD)
                }
                """.trimIndent(),
                userData = mapOf("max_line_length" to "10")
            )
        ).isEqualTo(
            listOf(
                LintError(2, 6, "argument-list-wrapping", """Missing newline after "(""""),
                LintError(2, 7, "argument-list-wrapping", "Arguments exceed maximum line length (can be split)"),
                LintError(2, 15, "argument-list-wrapping", "Arguments exceed maximum line length (can be split)"),
                LintError(2, 23, "argument-list-wrapping", "Arguments exceed maximum line length (can be split)"),
                LintError(2, 31, "argument-list-wrapping", "Arguments exceed maximum line length (can be split)"),
                LintError(2, 37, "argument-list-wrapping", """Missing newline before ")"""")
            )
        )
    }

    @Test
    fun testLintCallArgumentListValid() {
        assertThat(
            ArgumentListWrappingRule().lint(
                """
                fun main() {
                    f(paramA, paramB, paramC, paramD)
                }
                """.trimIndent()
            )
        ).isEmpty()
    }

    @Test
    fun testLintCallArgumentListValidMultiLine() {
        assertThat(
            ArgumentListWrappingRule().lint(
                """
                fun main() {
                    f(
                        paramA,
                        paramB,
                        paramC,
                        paramD
                    )
                }
                """.trimIndent()
            )
        ).isEmpty()
    }

    @Test
    fun testLintCallArgumentListValidSomeSingleLine() {
        assertThat(
            ArgumentListWrappingRule().lint(
                """
                fun main() {
                    f(
                        paramA,
                        paramB, paramC,
                        paramD
                    )
                }
                """.trimIndent()
            )
        ).isEmpty()
    }

    @Test
    fun testFormatCallArgumentList() {
        assertThat(
            ArgumentListWrappingRule().format(
                """
                fun main() {
                    f(paramA,
                        paramB,
                           paramC,
                        paramD)
                }
                """.trimIndent()
            )
        ).isEqualTo(
            """
            fun main() {
                f(
                    paramA,
                    paramB,
                    paramC,
                    paramD
                )
            }
            """.trimIndent()
        )
    }

    @Test
    fun testFormatCallArgumentListWhenMaxLineLengthExceeded() {
        assertThat(
            ArgumentListWrappingRule().format(
                """
                fun main() {
                    f(paramA, paramB, paramC, paramD)
                }
                """.trimIndent(),
                userData = mapOf("max_line_length" to "10")
            )
        ).isEqualTo(
            """
            fun main() {
                f(
                    paramA,
                    paramB,
                    paramC,
                    paramD
                )
            }
            """.trimIndent()
        )
    }
}
