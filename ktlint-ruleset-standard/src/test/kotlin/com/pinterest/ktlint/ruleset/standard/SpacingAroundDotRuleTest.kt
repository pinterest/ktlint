package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.core.LintError
import com.pinterest.ktlint.test.format
import com.pinterest.ktlint.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.testng.annotations.Test

class SpacingAroundDotRuleTest {

    @Test
    fun testLint() {
        assertThat(com.pinterest.ktlint.ruleset.standard.SpacingAroundDotRule().lint("fun String .foo() = \"foo . \""))
            .isEqualTo(
                listOf(
                    LintError(1, 11, "dot-spacing", "Unexpected spacing before \".\"")
                )
            )

        assertThat(com.pinterest.ktlint.ruleset.standard.SpacingAroundDotRule().lint("fun String. foo() = \"foo . \""))
            .isEqualTo(
                listOf(
                    LintError(1, 12, "dot-spacing", "Unexpected spacing after \".\"")
                )
            )

        assertThat(com.pinterest.ktlint.ruleset.standard.SpacingAroundDotRule().lint("fun String . foo() = \"foo . \""))
            .isEqualTo(
                listOf(
                    LintError(1, 11, "dot-spacing", "Unexpected spacing before \".\""),
                    LintError(1, 13, "dot-spacing", "Unexpected spacing after \".\"")
                )
            )

        assertThat(
            com.pinterest.ktlint.ruleset.standard.SpacingAroundDotRule().lint(
                """
                fun String.foo() {
                    (2..10).map { it + 1 }
                        .map { it * 2 }
                        .toSet()
                }
                """.trimIndent()
            )
        ).isEmpty()

        assertThat(
            com.pinterest.ktlint.ruleset.standard.SpacingAroundDotRule().lint(
                """
                fun String.foo() {
                    (2..10).map { it + 1 }
                        . map { it * 2 }
                        .toSet()
                }
                """.trimIndent()
            )
        ).isEqualTo(
            listOf(
                LintError(3, 10, "dot-spacing", "Unexpected spacing after \".\"")
            )
        )

        assertThat(
            com.pinterest.ktlint.ruleset.standard.SpacingAroundDotRule().lint(
                """
                fun String.foo() {
                    (2..10).map { it + 1 }
                        // Some comment
                        . map { it * 2 }
                        .toSet()
                }
                """.trimIndent()
            )
        ).isEqualTo(
            listOf(
                LintError(4, 10, "dot-spacing", "Unexpected spacing after \".\"")
            )
        )
    }

    @Test
    fun testLintComment() {
        assertThat(
            com.pinterest.ktlint.ruleset.standard.SpacingAroundDotRule().lint(
                """
                fun foo() {
                    /**.*/
                    generateSequence(locate(dir)) { seed -> locate(seed.parent.parent) } // seed.parent == .editorconfig dir
                        .map { it to lazy { load(it) } }
                }
                """.trimIndent()
            )
        ).isEmpty()
    }

    @Test
    fun testFormat() {
        assertThat(com.pinterest.ktlint.ruleset.standard.SpacingAroundDotRule().format("fun String .foo() = \"foo . \""))
            .isEqualTo("fun String.foo() = \"foo . \"")

        assertThat(com.pinterest.ktlint.ruleset.standard.SpacingAroundDotRule().format("fun String. foo() = \"foo . \""))
            .isEqualTo("fun String.foo() = \"foo . \"")

        assertThat(com.pinterest.ktlint.ruleset.standard.SpacingAroundDotRule().format("fun String . foo() = \"foo . \""))
            .isEqualTo("fun String.foo() = \"foo . \"")

        assertThat(
            com.pinterest.ktlint.ruleset.standard.SpacingAroundDotRule().format(
                """
                fun String.foo() {
                    (2..10).map { it + 1 }
                        . map { it * 2 }
                        .toSet()
                }
                """.trimIndent()
            )
        ).isEqualTo(
            """
            fun String.foo() {
                (2..10).map { it + 1 }
                    .map { it * 2 }
                    .toSet()
            }
            """.trimIndent()
        )
    }
}
