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
    fun testLambdaArgumentsAreIgnored2() {
        assertThat(
            ArgumentListWrappingRule().lint(
                """
                fun main() {
                    foo(bar.apply {
                        // stuff
                    })
                }
                """.trimIndent()
            )
        ).isEmpty()
    }

    @Test
    fun testLambdaArgumentsAreIgnored3() {
        assertThat(
            ArgumentListWrappingRule().lint(
                """
                fun main() {
                    println(Runnable {
                        println("hello")
                        println("world")
                    })
                }
                """.trimIndent()
            )
        ).isEmpty()
    }

    @Test
    fun testLambdaArgumentsAreIgnoredWithMaxLineLength() {
        assertThat(
            ArgumentListWrappingRule().lint(
                """
                abstract class A(init: String.() -> Int)
                class B : A({
                    toInt()
                    toInt()
                    toInt()
                    toInt()
                    toInt()
                    toInt()
                })

                fun test(a: Any, b: (Any) -> Any) {
                    test(a = "1", b = {
                        it.toString()
                    })
                }
                """.trimIndent(),
                userData = mapOf("max_line_length" to "80")
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

    @Test
    fun testLintInIfCondition() {
        assertThat(
            ArgumentListWrappingRule().lint(
                """
                fun test(param1: Int, param2: Int) {
                    if (listOfNotNull(
                            param1
                        ).isEmpty()
                    ) {
                        println(1)
                    } else if (listOfNotNull(
                            param2
                        ).isEmpty()
                    ) {
                        println(2)
                    }
                }
                """.trimIndent()
            )
        ).isEmpty()
    }

    @Test
    fun testLintInIfCondition2() {
        assertThat(
            ArgumentListWrappingRule().lint(
                """
                fun test(param1: Int, param2: Int) {
                    if (
                        listOfNotNull(
                            param1
                        ).isEmpty()
                    ) {
                        println(1)
                    } else if (
                        listOfNotNull(
                            param2
                        ).isEmpty()
                    ) {
                        println(2)
                    }
                }
                """.trimIndent()
            )
        ).isEmpty()
    }

    @Test
    fun testLintAfterElse() {
        assertThat(
            ArgumentListWrappingRule().lint(
                """
                fun foo(i: Int, j: Int) = 1

                fun test() {
                    val x = if (true) 1 else foo(
                        2,
                        3
                    )
                }
                """.trimIndent()
            )
        ).isEmpty()
    }

    @Test
    fun testLintInWhenCondition() {
        assertThat(
            ArgumentListWrappingRule().lint(
                """
                fun foo(i: Int) = true

                fun test(i: Int) {
                    when (foo(
                        i
                    )) {
                        true -> println(1)
                        false -> println(2)
                    }
                }
                """.trimIndent()
            )
        ).isEmpty()
    }

    @Test
    fun testLintInWhileCondition() {
        assertThat(
            ArgumentListWrappingRule().lint(
                """
                fun foo(i: Int) = true

                fun test(i: Int) {
                    while (foo(
                            i
                        )
                    ) {
                        println()
                    }
                }
                """.trimIndent()
            )
        ).isEmpty()
    }

    @Test
    fun testLintInDoWhileCondition() {
        assertThat(
            ArgumentListWrappingRule().lint(
                """
                fun foo(i: Int) = true

                fun test(i: Int) {
                    do {
                        println()
                    } while (foo(
                            i
                        )
                    )
                }
                """.trimIndent()
            )
        ).isEmpty()
    }

    @Test
    fun `test lint column is correctly calculated for string templates`() {
        assertThat(
            ArgumentListWrappingRule().lint(
                """
                class Test {

                  fun someMethod(str: String) = Unit

                  val someString = someMethod(
                      ""${'"'}
                      longtext longtext longtext longtext longtext longtext longtext longtext
                      longtext longtext longtext longtext longtext longtext longtext longtext
                      longtext longtext longtext longtext longtext longtext longtext longtext
                      longtext longtext longtext longtext longtext longtext longtext longtext
                      longtext longtext longtext longtext longtext longtext longtext longtext
                      ""${'"'}.trimIndent('|')
                  )

                  val someString2 = someMethod(""${'"'}stuff""${'"'})
                }

                fun foo() {
                    function("arg1", "arg2") {
                        ""${'"'}
                        WORDS
                        WORDS c.property = ${"interpolation".length}
                        ${'$'}{FUNCTION_2(TestClassA::startDateTime, descending = true)}
                        ""${'"'}
                    }
                }

                fun bar() {
                    json(
                        ""${'"'}
                        {
                            "array": [
                                ${'$'}{function(arg1, arg2, arg3)}
                            ]
                        }
                        ""${'"'}.trimIndent()
                    )
                }
                """.trimIndent(),
                userData = mapOf("max_line_length" to "100")
            )
        ).isEmpty()
    }

    @Test
    fun `lint argument list after multiline dot qualified expression`() {
        assertThat(
            ArgumentListWrappingRule().lint(
                """
                class Logging(mode: Any, appInstanceIdentity: String, org: String)

                class StateManager {
                    var firebaseLogger: Logging? = null
                }

                private fun replaceLogger(deviceId: String, orgName: String) {
                    val stateManager: StateManager = StateManager()
                    stateManager
                        .firebaseLogger(
                        mode = 0,
                        appInstanceIdentity = deviceId,
                        org = orgName
                    )
                }
                """.trimIndent()
            )
        ).isEmpty()
    }

    @Test
    fun `lint argument list after multiline type argument list`() {
        assertThat(
            ArgumentListWrappingRule().lint(
                """
                fun test() {
                    generic<
                        Int,
                        Int>(
                        1,
                        2
                    )
                }
                """.trimIndent()
            )
        ).isEmpty()
    }
}
