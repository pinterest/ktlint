package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.core.LintError
import com.pinterest.ktlint.test.format
import com.pinterest.ktlint.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class NoSemicolonsRuleTest {

    @Test
    fun testLint() {
        assertThat(
            NoSemicolonsRule().lint(
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
            NoSemicolonsRule().format(
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
        assertThat(NoSemicolonsRule().format("fun main() {}; "))
            .isEqualTo("fun main() {} ")
        assertThat(
            NoSemicolonsRule().format(
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
            NoSemicolonsRule().lint(
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
            NoSemicolonsRule().lint(
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

    @Test
    fun testSemicolonAllowedBeforeAnnotationAndLambda() {
        assertThat(
            NoSemicolonsRule().lint(
                """
                annotation class Ann
                val f: () -> String = run {
                    listOf(1).map {
                        it + it
                    };
                    @Ann
                    { "" }
                }
                """
            )
        ).isEmpty()
    }

    @Test
    fun testSemicolonAllowedBeforeCommentAndLambda() {
        assertThat(
            NoSemicolonsRule().lint(
                """
                val f: () -> String = run {
                    listOf(1).map {
                        it + it
                    };
                    // comment
                    { "" }
                }
                """
            )
        ).isEmpty()
    }

    @Test
    fun testSemicolonAllowedBeforeKDocAndLambda() {
        assertThat(
            NoSemicolonsRule().lint(
                """
                val f: () -> String = run {
                    listOf(1).map {
                        it + it
                    };
                    /**
                     * kdoc
                     */
                    { "" }
                }
                """
            )
        ).isEmpty()
    }
}
