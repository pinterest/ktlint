package com.github.shyiko.ktlint.rule

import com.github.shyiko.ktlint.LintError
import com.github.shyiko.ktlint.format
import com.github.shyiko.ktlint.lint
import org.assertj.core.api.Assertions.assertThat
import org.testng.annotations.Test

class NoSemicolonsRuleTest {

    @Test
    fun testLint() {
        assertThat(NoSemicolonsRule().lint(
            """
            fun main() {
                fun name() { a(); return b }
                println(";")
                println();
            }
            """.trimIndent()
        )).isEqualTo(listOf(
            LintError(4, 14, "rule-id", "Unnecessary semicolon")
        ))
    }

    @Test
    fun testFormat() {
        assertThat(NoSemicolonsRule().format(
            """
            fun main() {
                fun name() { a();return b }
                println()
                println();
            };
            """.trimIndent()
        )).isEqualTo(
            """
            fun main() {
                fun name() { a(); return b }
                println()
                println()
            }
            """.trimIndent()
        )
        assertThat(NoSemicolonsRule().format("fun main() {}; "))
            .isEqualTo("fun main() {} ")
    }

}
