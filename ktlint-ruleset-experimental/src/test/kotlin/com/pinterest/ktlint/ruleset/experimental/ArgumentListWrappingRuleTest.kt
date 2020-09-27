package com.pinterest.ktlint.ruleset.experimental

import com.pinterest.ktlint.core.LintError
import com.pinterest.ktlint.test.format
import com.pinterest.ktlint.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class ArgumentListWrappingRuleTest {

    @Test
    fun testLintArgumentInconsistency() {
        assertThat(
            ArgumentListWrappingRule().lint(
                """
                    val x = f(
                        a,
                        b, c
                    )
                """.trimIndent()
            )
        ).isEqualTo(
            listOf(
                LintError(
                    3,
                    8,
                    "argument-list-wrapping",
                    "Argument should be on a separate line (unless all arguments can fit a single line)"
                )
            )
        )
    }

    @Test
    fun testFormatArgumentInconsistency() {
        assertThat(
            ArgumentListWrappingRule().format(
                """
                    val x = f(
                        a,
                        b, c
                    )
                """.trimIndent()
            )
        ).isEqualTo(
            """
                val x = f(
                    a,
                    b,
                    c
                )
            """.trimIndent()
        )
    }

    @Test
    fun testFormatArgumentsWithNestedCalls() {
        assertThat(
            ArgumentListWrappingRule().format(
                """
                    val x = test(
                        one("a", "b",
                        "c"),
                        "Two", "Three", "Four"
                    )
                """.trimIndent()
            )
        ).isEqualTo(
            """
                val x = test(
                    one(
                        "a",
                        "b",
                        "c"
                    ),
                    "Two",
                    "Three",
                    "Four"
                )
            """.trimIndent()
        )
    }

    @Test
    fun testLintArgumentListWhenMaxLineLengthExceeded() {
        assertThat(
            ArgumentListWrappingRule().lint(
                """
                    val x = f(a, b, c)
                """.trimIndent(),
                userData = mapOf("max_line_length" to "10")
            )
        ).isEqualTo(
            listOf(
                LintError(
                    1,
                    11,
                    "argument-list-wrapping",
                    "Argument should be on a separate line (unless all arguments can fit a single line)"
                ),
                LintError(
                    1,
                    14,
                    "argument-list-wrapping",
                    "Argument should be on a separate line (unless all arguments can fit a single line)"
                ),
                LintError(
                    1,
                    17,
                    "argument-list-wrapping",
                    "Argument should be on a separate line (unless all arguments can fit a single line)"
                ),
                LintError(1, 18, "argument-list-wrapping", """Missing newline before ")"""")
            )
        )
    }

    @Test
    fun testLambdaArgumentsAreIgnored() {
        assertThat(
            ArgumentListWrappingRule().lint(
                """
                abstract class A(init: String.() -> Int)
                class B : A({
                    toInt()
                })

                fun test(a: Any, b: (Any) -> Any) {
                    test(a = "1", b = {
                        it.toString()
                    })
                }
                """.trimIndent()
            )
        ).isEmpty()
    }

    @Test
    fun testFormatWithLambdaArguments() {
        assertThat(
            ArgumentListWrappingRule().format(
                """
                abstract class A(init: String.() -> Int)
                class B : A({
                    toInt()
                })

                fun test(a: Any, b: (Any) -> Any) {
                    test(
                        a = "1", b = {
                        it.toString()
                    })
                }

                fun test(a: Any, b: (Any) -> Any, c: Any) {
                    test(a = "1", b = {
                        it.toString()
                    }, c = 123)
                }

                fun test(a: Any, b: (Any) -> Any, c: Any) {
                    test(a = "1", b = {
                        it.toString()
                    },
                    c = 123)
                }

                fun test(a: Any, b: (Any) -> Any, c: Any) {
                    test("1",
                        { val x = it.toString(); x }, 123)
                }

                fun test(a: Any, b: (Any) -> Any, c: Any) {
                    test(
                        "1",
                        {
                            f(1,
                                { "stuff" }, 3)
                        },
                        123
                    )
                }
                """.trimIndent()
            )
        ).isEqualTo(
            """
            abstract class A(init: String.() -> Int)
            class B : A({
                toInt()
            })

            fun test(a: Any, b: (Any) -> Any) {
                test(
                    a = "1",
                    b = {
                        it.toString()
                    }
                )
            }

            fun test(a: Any, b: (Any) -> Any, c: Any) {
                test(a = "1", b = {
                    it.toString()
                }, c = 123)
            }

            fun test(a: Any, b: (Any) -> Any, c: Any) {
                test(
                    a = "1",
                    b = {
                        it.toString()
                    },
                    c = 123
                )
            }

            fun test(a: Any, b: (Any) -> Any, c: Any) {
                test(
                    "1",
                    { val x = it.toString(); x },
                    123
                )
            }

            fun test(a: Any, b: (Any) -> Any, c: Any) {
                test(
                    "1",
                    {
                        f(
                            1,
                            { "stuff" },
                            3
                        )
                    },
                    123
                )
            }
            """.trimIndent()
        )
    }

    @Test
    fun testLintVarargIsIgnored() {
        assertThat(
            ArgumentListWrappingRule().lint(
                """
                private val tokenSet = TokenSet.create(
                    MUL, PLUS, MINUS, DIV, PERC, LT, GT, LTEQ, GTEQ, EQEQEQ, EXCLEQEQEQ, EQEQ,
                    EXCLEQ, ANDAND, OROR, ELVIS, EQ, MULTEQ, DIVEQ, PERCEQ, PLUSEQ, MINUSEQ, ARROW
                )
                """.trimIndent()
            )
        ).isEmpty()
    }

    @Test
    fun testFormatPreservesIndentWithAnnotationsOnMultiLine() {
        assertThat(
            ArgumentListWrappingRule().format(
                """
                class A {
                    fun f(@Annotation
                        a: Any,
                        @Annotation([
                            "v1",
                            "v2"
                        ])
                        b: Any,
                        c: Any =
                            false,
                        @Annotation d: Any,
                        @SingleLineAnnotation([1, 2])
                        e: Any) {
                    }
                }
                """.trimIndent()
            )
        ).isEqualTo(
            """
            class A {
                fun f(@Annotation
                    a: Any,
                    @Annotation(
                        [
                            "v1",
                            "v2"
                        ]
                    )
                    b: Any,
                    c: Any =
                        false,
                    @Annotation d: Any,
                    @SingleLineAnnotation([1, 2])
                    e: Any) {
                }
            }
            """.trimIndent()
        )
    }

    @Test
    fun testLintArgumentAfterBlockComment() {
        assertThat(
            ArgumentListWrappingRule().lint(
                """
                fun main() {
                    someMethod(
                        /* firstName= */ "John",
                        /* lastName= */ "Doe",
                        /* age= */ 30
                    )
                }
                """.trimIndent()
            )
        ).isEmpty()
    }
}
