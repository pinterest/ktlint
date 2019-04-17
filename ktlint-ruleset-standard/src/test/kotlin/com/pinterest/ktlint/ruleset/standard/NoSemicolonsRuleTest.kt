package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.core.LintError
import com.pinterest.ktlint.test.format
import com.pinterest.ktlint.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.testng.annotations.Test

class NoSemicolonsRuleTest {

    @Test
    fun testLint() {
        assertThat(
            com.pinterest.ktlint.ruleset.standard.NoSemicolonsRule().lint(
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
            )
        ).isEqualTo(
            listOf(
                LintError(1, 14, "no-semi", "Unnecessary semicolon"),
                LintError(6, 14, "no-semi", "Unnecessary semicolon")
            )
        )
    }

    @Test
    fun testFormat() {
        assertThat(
            com.pinterest.ktlint.ruleset.standard.NoSemicolonsRule().format(
                """
                fun main() {
                    fun name() { a();return b }
                    println()
                    println();
                };
                """.trimIndent()
            )
        ).isEqualTo(
            """
            fun main() {
                fun name() { a(); return b }
                println()
                println()
            }
            """.trimIndent()
        )
        assertThat(com.pinterest.ktlint.ruleset.standard.NoSemicolonsRule().format("fun main() {}; "))
            .isEqualTo("fun main() {} ")
        assertThat(
            com.pinterest.ktlint.ruleset.standard.NoSemicolonsRule().format(
                """
                enum class E {
                    ONE, TWO;
                    fun fn() {}
                }
                """.trimIndent()
            )
        ).isEqualTo(
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
        assertThat(
            com.pinterest.ktlint.ruleset.standard.NoSemicolonsRule().lint(
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
            )
        ).isEqualTo(
            listOf(
                LintError(8, 6, "no-semi", "Unnecessary semicolon")
            )
        )
    }

    @Test
    fun testSemicolonAllowedInKDocAfterIdentifiers() {
        assertThat(
            com.pinterest.ktlint.ruleset.standard.NoSemicolonsRule().lint(
                """
                /**
                 * [x];
                 */
                fun foo() {
                }
                """
            )
        ).isEmpty()
    }
}
