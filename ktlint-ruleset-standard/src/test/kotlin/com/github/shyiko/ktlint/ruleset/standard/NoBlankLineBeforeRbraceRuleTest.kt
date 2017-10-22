package com.github.shyiko.ktlint.ruleset.standard

import com.github.shyiko.ktlint.core.LintError
import com.github.shyiko.ktlint.test.format
import com.github.shyiko.ktlint.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.testng.annotations.Test

class NoBlankLineBeforeRbraceRuleTest {
    @Test
    fun testLintInString() {
        assertThat(NoBlankLineBeforeRbraceRule().lint(
            "fun main() {println(\"\"\"\n\n\n}\"\"\")}")).isEmpty()
    }

    @Test
    fun testLintBeforeRbrace() {
        assertThat(NoBlankLineBeforeRbraceRule().lint(
            """fun main() {
                fun a() {

                }
                fun b()

            }"""
        )).isEqualTo(listOf(
            LintError(3, 1, "no-blank-line-before-rbrace", "Needless blank line(s)"),
            LintError(6, 1, "no-blank-line-before-rbrace", "Needless blank line(s)")
        ))
    }

    @Test
    fun testFormatBeforeRbrace() {
        assertThat(NoBlankLineBeforeRbraceRule().format(
            """
            fun main() {
                fun a() {

                }
                fun b()

            }
            """
        )).isEqualTo(
            """
            fun main() {
                fun a() {
                }
                fun b()
            }
            """
        )
    }
}
