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
    fun testLintAfterPackageName() {
        assertThat(
            NoConsecutiveBlankLinesRule().lint(
                """
                package com.test

                fun main() {
                }
                """.trimIndent()
            )
        ).isEmpty()
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

    @Test
    fun `test two line breaks between class name and primary constructor`() {
        assertThat(
            NoConsecutiveBlankLinesRule().format(
                """
                class A

                constructor(a: Int)

                class B

                private constructor(b: Int)
                """.trimIndent()
            )
        ).isEqualTo(
            """
            class A
            constructor(a: Int)

            class B
            private constructor(b: Int)
            """.trimIndent()
        )
    }

    @Test
    fun `test three line breaks between class name and primary constructor`() {
        assertThat(
            NoConsecutiveBlankLinesRule().format(
                """
                class A


                constructor(a: Int)
                """.trimIndent()
            )
        ).isEqualTo(
            """
            class A
            constructor(a: Int)
            """.trimIndent()
        )
    }

    @Test
    fun `test two line breaks between comment and primary constructor`() {
        assertThat(
            NoConsecutiveBlankLinesRule().lint(
                """
                class A // comment

                constructor(a: Int)
                """.trimIndent()
            )
        ).isEmpty()
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
