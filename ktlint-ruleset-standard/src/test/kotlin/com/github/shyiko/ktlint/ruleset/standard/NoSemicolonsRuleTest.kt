package com.github.shyiko.ktlint.ruleset.standard

import com.github.shyiko.ktlint.core.LintError
import com.github.shyiko.ktlint.test.format
import com.github.shyiko.ktlint.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.testng.annotations.Test

class NoSemicolonsRuleTest {

    @Test
    fun testLint() {
        assertThat(NoSemicolonsRule().lint(
            """
            package a.b.c;

            fun main() {
                fun name() { a(); return b }
                println(";")
                println();

                Any();
                {
                }.print()
                Any()
                ;{ /*...*/ }.print()
            }
            """.trimIndent()
        )).isEqualTo(listOf(
            LintError(1, 14, "no-semi", "Unnecessary semicolon"),
            LintError(6, 14, "no-semi", "Unnecessary semicolon")
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

    @Test
    fun testSemiIsPreservedAfterCompanionObject() {
        // github issue #281
        assertThat(NoSemicolonsRule().lint(
            """
            class A {
                companion object;
                companion object ;
            }
            class A {
                companion object {
                    val s = ""
                };
            }
            """.trimIndent()
        )).isEqualTo(listOf(
            LintError(8, 6, "no-semi", "Unnecessary semicolon")
        ))
    }
}
