package com.github.shyiko.ktlint.ruleset.standard

import com.gihub.shyiko.ktlint.ruleset.standard.NoSemicolonsRule
import com.github.shyiko.ktlint.core.LintError
import com.github.shyiko.ktlint.test.lint
import com.github.shyiko.ktlint.test.format
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
            LintError(4, 14, "no-semi", "Unnecessary semicolon")
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
        assertThat(NoSemicolonsRule().format(
            """
            enum class E {
                ONE, TWO;
                fun fn() {}
            }
            """.trimIndent()
        )).isEqualTo(
            """
            enum class E {
                ONE, TWO;
                fun fn() {}
            }
            """.trimIndent()
        )
    }

}
