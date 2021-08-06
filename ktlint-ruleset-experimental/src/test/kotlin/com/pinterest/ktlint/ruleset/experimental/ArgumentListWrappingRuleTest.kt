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

        assertThat(
            ArgumentListWrappingRule().lint(
                """
                class MyClass {
                    private fun initCoilOkHttp() {
                        Coil.setImageLoader(
                            ImageLoader.Builder(this)
                                .crossfade(true)
                                .okHttpClient(
                                    okHttpClient.newBuilder()
                                        .cache(CoilUtils.createDefaultCache(this))
                                        .build()
                                )
                                .build()
                        )
                    }
                }
                """.trimIndent()
            )
        ).isEmpty()
    }

    @Test
    fun `lint argument list after dot qualified expression with assignment`() {
        assertThat(
            ArgumentListWrappingRule().lint(
                """
                private fun replaceLogger(deviceId: String, orgName: String) {
                    stateManager
                        .firebaseLogger = Logging(
                        mode = if (BuildConfig.DEBUG) Logging.Companion.LogDestination.DEV else Logging.Companion.LogDestination.PROD,
                        appInstanceIdentity = deviceId,
                        org = orgName
                    )
                    stateManager.firebaseLogger.tellTheCloudAboutMe()
                    customisation.attachToFirebase(stateManager.firebaseLogger.appCloudPrefix)
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

    // https://github.com/pinterest/ktlint/issues/1081
    @Test
    fun testManyCorrections() {
        assertThat(
            ArgumentListWrappingRule().format(
                """
                package com.foo

                class MyClass() {
                    private fun doSomething() {
                        if (d == 0 || e == 0f) {
                            c.ee(hh, d, d, yy)
                        } else {
                            foo.blah()
                            val dr = t - u
                            val xx = -gg(dr) * rr(2f * d, dr.hh)
                            foo.bar(
                                dd,
                                g - d
                            )
                            foo.baz(
                                a.b, a.c - d
                            )
                            foo.biz(
                                a.b, a.c - d,
                                a.b, a.c,
                                a.b + d, a.c
                            )
                            foo.baz(
                                a.x - d, a.c
                            )
                            foo.biz(
                                a.x - d, a.c,
                                a.x, a.c,
                                a.x, a.c - d
                            )
                            foo.baz(
                                a.x, a.j + d
                            )
                        }
                    }
                }
                """.trimIndent()
            )
        ).isEqualTo(
            """
            package com.foo

            class MyClass() {
                private fun doSomething() {
                    if (d == 0 || e == 0f) {
                        c.ee(hh, d, d, yy)
                    } else {
                        foo.blah()
                        val dr = t - u
                        val xx = -gg(dr) * rr(2f * d, dr.hh)
                        foo.bar(
                            dd,
                            g - d
                        )
                        foo.baz(
                            a.b,
                            a.c - d
                        )
                        foo.biz(
                            a.b,
                            a.c - d,
                            a.b,
                            a.c,
                            a.b + d,
                            a.c
                        )
                        foo.baz(
                            a.x - d,
                            a.c
                        )
                        foo.biz(
                            a.x - d,
                            a.c,
                            a.x,
                            a.c,
                            a.x,
                            a.c - d
                        )
                        foo.baz(
                            a.x,
                            a.j + d
                        )
                    }
                }
            }
            """.trimIndent()
        )
    }

    // https://github.com/pinterest/ktlint/issues/1112
    @Test
    fun `lint argument list in lambda in dot qualified expression`() {
        assertThat(
            ArgumentListWrappingRule().lint(
                """
                fun main() {
                    listOf(1, 2, 3).map {
                        println(
                            it
                        )
                    }
                }
                """.trimIndent()
            )
        ).isEmpty()
    }

    @Test
    fun `lint parameter list with assignment and no dot qualified expression`() {
        assertThat(
            ArgumentListWrappingRule().lint(
                """
                fun foo() {
                    tasks.test {
                        systemProperties = mutableMapOf(
                            "junit.jupiter.displayname.generator.default" to
                                "org.junit.jupiter.api.DisplayNameGenerator\${'$'}ReplaceUnderscores",

                            "junit.jupiter.execution.parallel.enabled" to
                                doParallelTesting.toString() as Any,
                            "junit.jupiter.execution.parallel.mode.default" to
                                "concurrent",
                            "junit.jupiter.execution.parallel.mode.classes.default" to
                                "concurrent"
                        )
                    }
                }
                """.trimIndent()
            )
        ).isEmpty()
    }
}
