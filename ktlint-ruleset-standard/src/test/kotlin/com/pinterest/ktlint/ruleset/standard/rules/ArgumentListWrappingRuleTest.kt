package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.rule.engine.core.api.editorconfig.MAX_LINE_LENGTH_PROPERTY
import com.pinterest.ktlint.ruleset.standard.StandardRuleSetProvider
import com.pinterest.ktlint.ruleset.standard.rules.ArgumentListWrappingRule.Companion.IGNORE_WHEN_PARAMETER_COUNT_GREATER_OR_EQUAL_THAN_PROPERTY
import com.pinterest.ktlint.ruleset.standard.rules.ClassSignatureRule.Companion.FORCE_MULTILINE_WHEN_PARAMETER_COUNT_GREATER_OR_EQUAL_THAN_PROPERTY
import com.pinterest.ktlint.test.KtLintAssertThat.Companion.EOL_CHAR
import com.pinterest.ktlint.test.KtLintAssertThat.Companion.MAX_LINE_LENGTH_MARKER
import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRuleBuilder
import com.pinterest.ktlint.test.LintViolation
import com.pinterest.ktlint.test.MULTILINE_STRING_QUOTE
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class ArgumentListWrappingRuleTest {
    private val argumentListWrappingRuleAssertThat =
        assertThatRuleBuilder { ArgumentListWrappingRule() }
            .addRequiredRuleProviderDependenciesFrom(StandardRuleSetProvider())
            .assertThat()

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
        @Suppress("ktlint:standard:argument-list-wrapping", "ktlint:standard:max-line-length")
        argumentListWrappingRuleAssertThat(code)
            .hasLintViolation(3, 8, "Argument should be on a separate line (unless all arguments can fit a single line)")
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
            .addAdditionalRuleProvider { IndentationRule() }
            .hasLintViolations(
                LintViolation(2, 9, "Argument should be on a separate line (unless all arguments can fit a single line)"),
                LintViolation(2, 14, "Argument should be on a separate line (unless all arguments can fit a single line)"),
                LintViolation(3, 8, "Missing newline before \")\""),
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given that not all parameters in a function call fit on a single line`() {
        val code =
            """
            // $MAX_LINE_LENGTH_MARKER      $EOL_CHAR
            val foobar = foobar(foo, bar, baz)
            """.trimIndent()
        val formattedCode =
            """
            // $MAX_LINE_LENGTH_MARKER      $EOL_CHAR
            val foobar = foobar(
                foo,
                bar,
                baz
            )
            """.trimIndent()
        argumentListWrappingRuleAssertThat(code)
            .setMaxLineLength()
            .hasLintViolations(
                LintViolation(2, 21, "Argument should be on a separate line (unless all arguments can fit a single line)"),
                LintViolation(2, 26, "Argument should be on a separate line (unless all arguments can fit a single line)"),
                LintViolation(2, 31, "Argument should be on a separate line (unless all arguments can fit a single line)"),
                LintViolation(2, 34, "Missing newline before \")\""),
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
            .withEditorConfigOverride(MAX_LINE_LENGTH_PROPERTY to 20)
            .hasLintViolations(
                LintViolation(13, 10, "Argument should be on a separate line (unless all arguments can fit a single line)"),
                LintViolation(13, 19, "Argument should be on a separate line (unless all arguments can fit a single line)"),
                LintViolation(13, 26, "Missing newline before \")\""),
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
            .addAdditionalRuleProvider { IndentationRule() }
            .hasLintViolations(
                LintViolation(3, 18, "Argument should be on a separate line (unless all arguments can fit a single line)"),
                LintViolation(5, 6, "Missing newline before \")\""),
                LintViolation(7, 9, "Argument should be on a separate line (unless all arguments can fit a single line)"),
                LintViolation(8, 26, "Missing newline before \")\""),
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
                LintViolation(6, 32, "Missing newline before \")\""),
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
            .addAdditionalRuleProvider { IndentationRule() }
            .hasLintViolations(
                LintViolation(2, 10, "Argument should be on a separate line (unless all arguments can fit a single line)"),
                LintViolation(2, 19, "Argument should be on a separate line (unless all arguments can fit a single line)"),
                LintViolation(5, 12, "Missing newline before \")\""),
            ).isFormattedAs(formattedCode)
    }

    @Nested
    inner class `Given a function call with too many (eg more than 8) arguments` {
        @Test
        fun `Given that arguments are on a single line but exceeding max line length`() {
            val code =
                """
                // $MAX_LINE_LENGTH_MARKER      $EOL_CHAR
                val foo = foo(1, 2, 3, 4, 5, 6, 7, 8, 9)
                """.trimIndent()
            argumentListWrappingRuleAssertThat(code)
                .setMaxLineLength()
                .withEditorConfigOverride(IGNORE_WHEN_PARAMETER_COUNT_GREATER_OR_EQUAL_THAN_PROPERTY to 8)
                .hasNoLintViolations()
        }

        @Test
        fun `Given that arguments are spread on multiple lines and not exceeding max line length`() {
            val code =
                """
                val foo = foo(
                    1, 2, 3, 4,
                    5, 6, 7, 8,
                    9
                )
                """.trimIndent()
            argumentListWrappingRuleAssertThat(code)
                .withEditorConfigOverride(IGNORE_WHEN_PARAMETER_COUNT_GREATER_OR_EQUAL_THAN_PROPERTY to 8)
                .hasNoLintViolations()
        }

        @Test
        fun `Given that arguments always have to be wrapped to a separate line`() {
            val code =
                """
                val foo = foo(
                    1, 2
                )
                """.trimIndent()
            argumentListWrappingRuleAssertThat(code)
                .withEditorConfigOverride(IGNORE_WHEN_PARAMETER_COUNT_GREATER_OR_EQUAL_THAN_PROPERTY to "unset")
                .hasLintViolation(2, 8, "Argument should be on a separate line (unless all arguments can fit a single line)")
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
            .addAdditionalRuleProvider { IndentationRule() }
            .hasLintViolations(
                LintViolation(4, 21, "Argument should be on a separate line (unless all arguments can fit a single line)"),
                LintViolation(7, 10, "Missing newline before \")\""),
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
    inner class `Given an if statement` {
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
            .withEditorConfigOverride(MAX_LINE_LENGTH_PROPERTY to 33)
            .hasLintViolations(
                LintViolation(8, 23, "Argument should be on a separate line (unless all arguments can fit a single line)"),
                LintViolation(8, 34, "Missing newline before \")\""),
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
            .withEditorConfigOverride(MAX_LINE_LENGTH_PROPERTY to 65)
            .hasLintViolations(
                LintViolation(4, 15, "Argument should be on a separate line (unless all arguments can fit a single line)"),
                LintViolation(4, 70, "Missing newline before \")\""),
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
            .withEditorConfigOverride(MAX_LINE_LENGTH_PROPERTY to 45)
            .hasNoLintViolations()
    }

    @Nested
    inner class `Given a multiline dot qualified expression` {
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
                LintViolation(32, 22, "Argument should be on a separate line (unless all arguments can fit a single line)"),
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

    @Test
    fun `Issue 1643 - do not wrap arguments if after wrapping rule the max line is no longer exceeded`() {
        val code =
            """
            // $MAX_LINE_LENGTH_MARKER                                                   $EOL_CHAR
            class Bar1 {
                val barrrrrrrrrrrrrrrrrrrrrrrrrrrrrrr by lazy {
                    fooooooooooooo("fooooooooooooooooooooooooooooooooooooooooooooo", true)
                }
            }
            class Bar2 {
                val barrrrrrrrrrrrrrrrrrrrrrrrrrrrrrr by lazy { fooooooooooooo("fooooooooooooooooooooooooooooooooooooooooooooo", true) }
            }
            """.trimIndent()
        val formattedCode =
            """
            // $MAX_LINE_LENGTH_MARKER                                                   $EOL_CHAR
            class Bar1 {
                val barrrrrrrrrrrrrrrrrrrrrrrrrrrrrrr by lazy {
                    fooooooooooooo("fooooooooooooooooooooooooooooooooooooooooooooo", true)
                }
            }
            class Bar2 {
                val barrrrrrrrrrrrrrrrrrrrrrrrrrrrrrr by lazy {
                    fooooooooooooo("fooooooooooooooooooooooooooooooooooooooooooooo", true)
                }
            }
            """.trimIndent()
        argumentListWrappingRuleAssertThat(code)
            .setMaxLineLength()
            .addAdditionalRuleProvider { WrappingRule() }
            .hasLintViolations(
                // During lint the violations below are reported because during linting, each rule reports its
                // violations independently and can not anticipate on autocorrects which would have been made by other
                // rules if those rules (i.e. the 'wrapping' rule) would have executed before the current rule (i.e. the
                // 'argument-list-wrapping' rule)
                LintViolation(8, 68, "Argument should be on a separate line (unless all arguments can fit a single line)"),
                LintViolation(8, 118, "Argument should be on a separate line (unless all arguments can fit a single line)"),
                LintViolation(8, 122, "Missing newline before \")\""),
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a property assignment with a binary expression for which the left hand side operator is a function call then binary expression wrapping takes precedence`() {
        val code =
            """
            // $MAX_LINE_LENGTH_MARKER         $EOL_CHAR
            val foo1 = foobar(foo * bar) + "foo"
            val foo2 = foobar(foo * bar) + "fooo"
            val foo3 = foobar("fooooooo", bar) + "foo"
            val foo4 = foobar("foooooooo", bar) + "foo"
            val foo5 = foobar("fooooooooo", bar) + "foo"
            val foo6 = foobar("foooo", foo * bar) + "foo"
            val foo7 = foobar("fooooooooooo", foo * bar) + "foo"
            """.trimIndent()
        val formattedCode =
            """
            // $MAX_LINE_LENGTH_MARKER         $EOL_CHAR
            val foo1 = foobar(foo * bar) + "foo"
            val foo2 =
                foobar(foo * bar) + "fooo"
            val foo3 =
                foobar("fooooooo", bar) + "foo"
            val foo4 =
                foobar("foooooooo", bar) + "foo"
            val foo5 =
                foobar("fooooooooo", bar) +
                    "foo"
            val foo6 =
                foobar("foooo", foo * bar) +
                    "foo"
            val foo7 =
                foobar(
                    "fooooooooooo",
                    foo * bar
                ) +
                    "foo"
            """.trimIndent()
        argumentListWrappingRuleAssertThat(code)
            .setMaxLineLength()
            .addAdditionalRuleProvider { BinaryExpressionWrappingRule() }
            .addAdditionalRuleProvider { WrappingRule() }
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a super type call entry with arguments which after wrapping to the next line does not fit on that line then keep start of body on same line as end of super type call entry`() {
        val code =
            """
            // $MAX_LINE_LENGTH_MARKER                            $EOL_CHAR
            // Note that the super type call does not fit the line after it has been wrapped
            // ...Foo(a: Any, b: Any, c: Any) :
            //  FooBar(a, "longggggggggggggggggggggggggggggggggggg")
            class Foo(a: Any, b: Any, c: Any) : FooBar(a, "longggggggggggggggggggggggggggggggggggg") {
                // body
            }
            """.trimIndent()
        val formattedCode =
            """
            // $MAX_LINE_LENGTH_MARKER                            $EOL_CHAR
            // Note that the super type call does not fit the line after it has been wrapped
            // ...Foo(a: Any, b: Any, c: Any) :
            //  FooBar(a, "longggggggggggggggggggggggggggggggggggg")
            class Foo(a: Any, b: Any, c: Any) :
                FooBar(
                    a,
                    "longggggggggggggggggggggggggggggggggggg"
                ) {
                // body
            }
            """.trimIndent()
        argumentListWrappingRuleAssertThat(code)
            .setMaxLineLength()
            .addAdditionalRuleProvider { ClassSignatureRule() }
            .addRequiredRuleProviderDependenciesFrom(StandardRuleSetProvider())
            .withEditorConfigOverride(FORCE_MULTILINE_WHEN_PARAMETER_COUNT_GREATER_OR_EQUAL_THAN_PROPERTY to 4)
            .hasLintViolationForAdditionalRule(5, 37, "Super type should start on a newline")
            .hasLintViolations(
                LintViolation(5, 44, "Argument should be on a separate line (unless all arguments can fit a single line)"),
                LintViolation(5, 47, "Argument should be on a separate line (unless all arguments can fit a single line)"),
                LintViolation(5, 88, "Missing newline before \")\""),
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Issue 2445 - Given a value argument followed by EOL comment after comma`() {
        val code =
            """
            val foo = foo(
                bar1 = "bar1", // some comment 1
                bar2 = "bar2", // some comment 2
            )
            """.trimIndent()
        @Suppress("ktlint:standard:argument-list-wrapping", "ktlint:standard:max-line-length")
        argumentListWrappingRuleAssertThat(code)
            .addAdditionalRuleProvider { ValueArgumentCommentRule() }
            .hasLintViolationsForAdditionalRule(
                LintViolation(2, 20, "A comment in a 'value_argument_list' is only allowed when placed on a separate line", false),
                LintViolation(3, 20, "A comment in a 'value_argument_list' is only allowed when placed on a separate line", false),
            )
        // When ValueArgumentCommentRule is not loaded or enabled
        argumentListWrappingRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Issue 2462 - Given a call expression with value argument list inside a binary expression, then first wrap the binary expression`() {
        val code =
            """
            // $MAX_LINE_LENGTH_MARKER                 $EOL_CHAR
            fun foo() {
                every { foo.bar(bazbazbazbazbazbazbazbazbaz) } returns bar
            }
            """.trimIndent()
        val formattedCode =
            """
            // $MAX_LINE_LENGTH_MARKER                 $EOL_CHAR
            fun foo() {
                every {
                    foo.bar(bazbazbazbazbazbazbazbazbaz)
                } returns bar
            }
            """.trimIndent()
        argumentListWrappingRuleAssertThat(code)
            .setMaxLineLength()
            .addAdditionalRuleProvider { BinaryExpressionWrappingRule() }
            .addAdditionalRuleProvider { WrappingRule() }
            // Lint violations from BinaryExpressionWrappingRule and WrappingRule are reported during linting only. When formatting, the
            // wrapping of the braces of the function literal by the BinaryExpressionWrapping prevents those violations from occurring.
            .hasLintViolationsForAdditionalRule(
                LintViolation(3, 12, "Newline expected after '{'"),
                LintViolation(3, 12, "Missing newline after \"{\""),
                LintViolation(3, 50, "Newline expected before '}'"),
                // Lint violation below only occurs during linting. Resolving violations above, prevents the next violation from occurring
                LintViolation(3, 59, "Line is exceeding max line length. Break line after 'returns' in binary expression"),
            )
            // The lint violation below is only reported during lint. When formatting, the violation above is resolved first, and as a
            // result this violation will no longer occur.
            .hasLintViolations(
                LintViolation(3, 21, "Argument should be on a separate line (unless all arguments can fit a single line)"),
                LintViolation(3, 48, "Missing newline before \")\""),
            ).isFormattedAs(formattedCode)
    }
}
