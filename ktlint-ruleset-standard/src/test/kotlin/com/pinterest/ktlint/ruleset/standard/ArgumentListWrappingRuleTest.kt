package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.core.api.DefaultEditorConfigProperties.maxLineLengthProperty
import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThat
import com.pinterest.ktlint.test.LintViolation
import com.pinterest.ktlint.test.MULTILINE_STRING_QUOTE
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class ArgumentListWrappingRuleTest {
    private val argumentListWrappingRuleAssertThat = ArgumentListWrappingRule().assertThat()

    @Test
    fun `Given a function call and not all arguments are on the same line`() {
        val code =
            """
            val x = f(
                a,
                b, c
            )
            """.trimIndent()
        val formattedCode =
            """
            val x = f(
                a,
                b,
                c
            )
            """.trimIndent()
        argumentListWrappingRuleAssertThat(code)
            .hasLintViolation(
                3,
                8,
                "Argument should be on a separate line (unless all arguments can fit a single line)"
            )
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a nested function call and not all parameters in the nested call are on the same line`() {
        val code =
            """
            val x = test(
                one("a", "b",
                "c"),
                "Two"
            )
            """.trimIndent()
        val formattedCode =
            """
            val x = test(
                one(
                    "a",
                    "b",
                    "c"
                ),
                "Two"
            )
            """.trimIndent()
        argumentListWrappingRuleAssertThat(code)
            .addAdditionalRules(IndentationRule())
            .hasLintViolations(
                LintViolation(2, 9, "Argument should be on a separate line (unless all arguments can fit a single line)"),
                LintViolation(2, 14, "Argument should be on a separate line (unless all arguments can fit a single line)"),
                LintViolation(3, 8, "Missing newline before \")\"")
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given that not all parameters in a function call fit on a single line`() {
        val code =
            """
            val x = f(a, b, c)
            """.trimIndent()
        val formattedCode =
            """
            val x = f(
                a,
                b,
                c
            )
            """.trimIndent()
        argumentListWrappingRuleAssertThat(code)
            .withEditorConfigOverride(maxLineLengthProperty to 10)
            .hasLintViolations(
                LintViolation(1, 11, "Argument should be on a separate line (unless all arguments can fit a single line)"),
                LintViolation(1, 14, "Argument should be on a separate line (unless all arguments can fit a single line)"),
                LintViolation(1, 17, "Argument should be on a separate line (unless all arguments can fit a single line)"),
                LintViolation(1, 18, "Missing newline before \")\"")
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given lambda arguments containing a line break are ignored`() {
        val code =
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
        argumentListWrappingRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Issue 928 - Given a lambda argument containing a line break`() {
        val code =
            """
            val foo =
                foo(
                    bar.apply {
                    // stuff
                    }
                )
            """.trimIndent()
        argumentListWrappingRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Issue 1010 - Given a lambda containing a line break and a line in that lambda exceeds the max line limit `() {
        val code =
            """
            abstract class A(init: String.() -> Int)
            class B : A({
                toInt() // This line exceeds the line limit but will not be reported
            })

            val foo1 =
                test(a = "1", b = {
                    it.toString() // This line exceeds the line limit but will not be reported
                })
            val foo2 =
                test(a = "1")
            val foo3 =
                test(a = "1", b = "2")
            """.trimIndent()
        val formattedCode =
            """
            abstract class A(init: String.() -> Int)
            class B : A({
                toInt() // This line exceeds the line limit but will not be reported
            })

            val foo1 =
                test(a = "1", b = {
                    it.toString() // This line exceeds the line limit but will not be reported
                })
            val foo2 =
                test(a = "1")
            val foo3 =
                test(
                    a = "1",
                    b = "2"
                )
            """.trimIndent()
        argumentListWrappingRuleAssertThat(code)
            .withEditorConfigOverride(maxLineLengthProperty to 20)
            .hasLintViolations(
                LintViolation(13, 10, "Argument should be on a separate line (unless all arguments can fit a single line)"),
                LintViolation(13, 19, "Argument should be on a separate line (unless all arguments can fit a single line)"),
                LintViolation(13, 26, "Missing newline before \")\"")
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a lambda in an argument of a function call which also contains a line break outside of the lambda`() {
        val code =
            """
            val foo1 =
                foo(
                    a = "1", b = {
                    it.toString()
                })
            val foo2 =
                foo("1",
                    { it.toString() })
            """.trimIndent()
        val formattedCode =
            """
            val foo1 =
                foo(
                    a = "1",
                    b = {
                        it.toString()
                    }
                )
            val foo2 =
                foo(
                    "1",
                    { it.toString() }
                )
            """.trimIndent()
        argumentListWrappingRuleAssertThat(code)
            .addAdditionalRules(IndentationRule())
            .hasLintViolations(
                LintViolation(3, 18, "Argument should be on a separate line (unless all arguments can fit a single line)"),
                LintViolation(5, 6, "Missing newline before \")\""),
                LintViolation(7, 9, "Argument should be on a separate line (unless all arguments can fit a single line)"),
                LintViolation(8, 26, "Missing newline before \")\"")
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a lambda in an argument of a function call which does not contains a line break outside of the lambda`() {
        val code =
            """
            val foo =
                foo(a = "1", b = {
                    it.toString()
                }, c = "3")
            """.trimIndent()
        argumentListWrappingRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Given some nested lambda parameter argument`() {
        val code =
            """
            val foo =
                foo(
                    "1",
                    {
                        bar(1,
                            { "foobar" }, 3)
                    },
                    123
                )
            """.trimIndent()
        val formattedCode =
            """
            val foo =
                foo(
                    "1",
                    {
                        bar(
                            1,
                            { "foobar" },
                            3
                        )
                    },
                    123
                )
            """.trimIndent()
        argumentListWrappingRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(5, 17, "Argument should be on a separate line (unless all arguments can fit a single line)"),
                LintViolation(6, 31, "Argument should be on a separate line (unless all arguments can fit a single line)"),
                LintViolation(6, 32, "Missing newline before \")\"")
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a parameter (after a multiline lambda parameter) which already does start on a new line`() {
        val code =
            """
            fun test(a: Any, b: (Any) -> Any, c: Any) {
                test(a = "1", b = {
                    it.toString()
                },
                c = 123)
            }
            """.trimIndent()
        val formattedCode =
            """
            fun test(a: Any, b: (Any) -> Any, c: Any) {
                test(
                    a = "1",
                    b = {
                        it.toString()
                    },
                    c = 123
                )
            }
            """.trimIndent()
        argumentListWrappingRuleAssertThat(code)
            .addAdditionalRules(IndentationRule())
            .hasLintViolations(
                LintViolation(2, 10, "Argument should be on a separate line (unless all arguments can fit a single line)"),
                LintViolation(2, 19, "Argument should be on a separate line (unless all arguments can fit a single line)"),
                LintViolation(5, 12, "Missing newline before \")\"")
            ).isFormattedAs(formattedCode)
    }

    @Nested
    inner class IgnoreFunctionCallWithTooManyArguments {
        @Test
        fun `Given many (eg more than 8) arguments spread on a single line but exceeding max line length`() {
            val code =
                """
                val foo = foo(1, 2, 3, 4, 5, 6, 7, 8, 9)
                """.trimIndent()
            argumentListWrappingRuleAssertThat(code)
                .withEditorConfigOverride(maxLineLengthProperty to 20)
                .hasNoLintViolations()
        }

        @Test
        fun `Given many (eg more than 8) arguments spread on multiple lines and not exceeding max line length`() {
            val code =
                """
                val foo = foo(
                    1, 2, 3, 4,
                    5, 6, 7, 8,
                    9
                )
                """.trimIndent()
            argumentListWrappingRuleAssertThat(code).hasNoLintViolations()
        }
    }

    @Test
    fun `Given multiline annotations with inner indents`() {
        val code =
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
        val formattedCode =
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
        argumentListWrappingRuleAssertThat(code)
            .addAdditionalRules(IndentationRule())
            .hasLintViolations(
                LintViolation(4, 21, "Argument should be on a separate line (unless all arguments can fit a single line)"),
                LintViolation(7, 10, "Missing newline before \")\"")
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Issue 927 - Allow inline block comment before argument`() {
        val code =
            """
            fun main() {
                someMethod(
                    /* firstName= */ "John",
                    /* lastName= */ "Doe",
                    /* age= */ 30
                )
            }
            """.trimIndent()
        argumentListWrappingRuleAssertThat(code).hasNoLintViolations()
    }

    @Nested
    inner class IfStatement {
        @Test
        fun `Issue 929 - Given some if condition calling a function with indented parameters`() {
            val code =
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
            argumentListWrappingRuleAssertThat(code).hasNoLintViolations()
        }

        @Test
        fun `Issue 929 - Given some if condition calling a function with indented parameters (preceded by line break)`() {
            val code =
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
            argumentListWrappingRuleAssertThat(code).hasNoLintViolations()
        }

        @Test
        fun `Given some function call in the else branch`() {
            val code =
                """
                fun foo(i: Int, j: Int) = 1

                fun test() {
                    val x = if (true) 1 else foo(
                        2,
                        3
                    )
                }
                """.trimIndent()
            argumentListWrappingRuleAssertThat(code).hasNoLintViolations()
        }
    }

    @Test
    fun `Issue 929 - Given a when-statement with a function call`() {
        val code =
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
        argumentListWrappingRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Issue 929 - Given a while-statement with a function call`() {
        val code =
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
        argumentListWrappingRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Issue 929 - Given a do-while-statement with a function call`() {
        val code =
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
        argumentListWrappingRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Given some multiline raw string literal (not containing a string template) which is exceeding the max line length then do not wrap`() {
        val code =
            """
            val foo1 = someMethod(
                $MULTILINE_STRING_QUOTE
                longtext longtext longtext longtext longtext longtext longtext longtext
                longtext longtext longtext longtext longtext longtext longtext longtext
                $MULTILINE_STRING_QUOTE.trimIndent()
            )

            val foo2 = someMethod(${MULTILINE_STRING_QUOTE}stuff$MULTILINE_STRING_QUOTE)
            """.trimIndent()
        val formattedCode =
            """
            val foo1 = someMethod(
                $MULTILINE_STRING_QUOTE
                longtext longtext longtext longtext longtext longtext longtext longtext
                longtext longtext longtext longtext longtext longtext longtext longtext
                $MULTILINE_STRING_QUOTE.trimIndent()
            )

            val foo2 = someMethod(
                ${MULTILINE_STRING_QUOTE}stuff$MULTILINE_STRING_QUOTE
            )
            """.trimIndent()
        argumentListWrappingRuleAssertThat(code)
            .withEditorConfigOverride(maxLineLengthProperty to 33)
            .hasLintViolations(
                LintViolation(8, 23, "Argument should be on a separate line (unless all arguments can fit a single line)"),
                LintViolation(8, 34, "Missing newline before \")\"")
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given some multiline raw string literal containing a string template and the the content of the string template exceeds the max line length then do wrap`() {
        val code =
            """
            val foo = someMethod(
                $MULTILINE_STRING_QUOTE
                some text
                ${'$'}{println("longtext longtext longtext longtext longtext longtext")}
                $MULTILINE_STRING_QUOTE.trimIndent()
            )
            """.trimIndent()
        val formattedCode =
            """
            val foo = someMethod(
                $MULTILINE_STRING_QUOTE
                some text
                ${'$'}{println(
                    "longtext longtext longtext longtext longtext longtext"
                )}
                $MULTILINE_STRING_QUOTE.trimIndent()
            )
            """.trimIndent()
        argumentListWrappingRuleAssertThat(code)
            // TODO: It is not clear how the length 65 is related to the lint errors below. Starting from length 66 the
            //  lint errors are not reported anymore.
            .withEditorConfigOverride(maxLineLengthProperty to 65)
            .hasLintViolations(
                LintViolation(4, 15, "Argument should be on a separate line (unless all arguments can fit a single line)"),
                LintViolation(4, 70, "Missing newline before \")\"")
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given some multiline raw string literal containing a string template not exceeding the max line length then do not wrap`() {
        val code =
            """
            fun foo() {
                json(
                    $MULTILINE_STRING_QUOTE
                    {
                        "array": [
                            ${'$'}{function(arg1, arg2, arg3)}
                        ]
                    }
                    $MULTILINE_STRING_QUOTE.trimIndent()
                )
            }
            """.trimIndent()
        argumentListWrappingRuleAssertThat(code)
            // TODO: With max line length of 43 or below, lint errors occur. It is not clear how that is related to
            //  example above
            .withEditorConfigOverride(maxLineLengthProperty to 44)
            .hasNoLintViolations()
    }

    @Nested
    inner class MultilineDotQualifiedExpression {
        @Test
        fun `Issue 1025 - Given an argument list after multiline dot qualified expression`() {
            val code =
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
            argumentListWrappingRuleAssertThat(code).hasNoLintViolations()
        }

        @Test
        fun `Issue 1025 - Given an argument list after dot qualified expression with assignment`() {
            val code =
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
            argumentListWrappingRuleAssertThat(code).hasNoLintViolations()
        }

        @Test
        fun `Issue 1196 - Given an argument list after multiline dot qualified expression`() {
            val code =
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
            argumentListWrappingRuleAssertThat(code).hasNoLintViolations()
        }
    }

    @Test
    fun `Issue 1025 - Given an argument list after multiline type argument list`() {
        val code =
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
        argumentListWrappingRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Issue 1081 - Given `() {
        val code =
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
        val formattedCode =
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
        argumentListWrappingRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(16, 22, "Argument should be on a separate line (unless all arguments can fit a single line)"),
                LintViolation(19, 22, "Argument should be on a separate line (unless all arguments can fit a single line)"),
                LintViolation(20, 22, "Argument should be on a separate line (unless all arguments can fit a single line)"),
                LintViolation(21, 26, "Argument should be on a separate line (unless all arguments can fit a single line)"),
                LintViolation(24, 26, "Argument should be on a separate line (unless all arguments can fit a single line)"),
                LintViolation(27, 26, "Argument should be on a separate line (unless all arguments can fit a single line)"),
                LintViolation(28, 22, "Argument should be on a separate line (unless all arguments can fit a single line)"),
                LintViolation(29, 22, "Argument should be on a separate line (unless all arguments can fit a single line)"),
                LintViolation(32, 22, "Argument should be on a separate line (unless all arguments can fit a single line)")
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Issue 1112 - Given an argument list in lambda in dot qualified expression`() {
        val code =
            """
            fun main() {
                listOf(1, 2, 3).map {
                    println(
                        it
                    )
                }
            }
            """.trimIndent()
        argumentListWrappingRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Issue 1198 - Given a parameter list with assignment and no dot qualified expression`() {
        val code =
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
        argumentListWrappingRuleAssertThat(code).hasNoLintViolations()
    }
}
