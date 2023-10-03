package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRule
import com.pinterest.ktlint.test.LintViolation
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class DiscouragedCommentLocationRuleTest {
    private val discouragedCommentLocationRuleAssertThat = assertThatRule { DiscouragedCommentLocationRule() }

    @Nested
    inner class `Given a comment after a type parameter then report a discouraged comment location` {
        @Test
        fun `Given an EOL comment`() {
            val code =
                """
                fun <T> // some comment
                foo(t: T) = "some-result"
                """.trimIndent()
            discouragedCommentLocationRuleAssertThat(code)
                .hasLintViolationWithoutAutoCorrect(1, 9, "No comment expected at this location")
        }

        @Test
        fun `Given an EOL comment on a newline`() {
            val code =
                """
                fun <T>
                // some comment
                foo(t: T) = "some-result"
                """.trimIndent()
            discouragedCommentLocationRuleAssertThat(code)
                .hasLintViolationWithoutAutoCorrect(2, 1, "No comment expected at this location")
        }

        @Test
        fun `Given a block comment`() {
            val code =
                """
                fun <T> /* some comment */
                foo(t: T) = "some-result"
                """.trimIndent()
            discouragedCommentLocationRuleAssertThat(code)
                .hasLintViolationWithoutAutoCorrect(1, 9, "No comment expected at this location")
        }

        @Test
        fun `Given a block comment on a newline`() {
            val code =
                """
                fun <T>
                /* some comment */
                foo(t: T) = "some-result"
                """.trimIndent()
            discouragedCommentLocationRuleAssertThat(code)
                .hasLintViolationWithoutAutoCorrect(2, 1, "No comment expected at this location")
        }

        @Test
        fun `Given a KDOC comment`() {
            val code =
                """
                fun <T> /** some comment */
                foo(t: T) = "some-result"
                """.trimIndent()
            discouragedCommentLocationRuleAssertThat(code)
                .hasLintViolationWithoutAutoCorrect(1, 9, "No comment expected at this location")
        }

        @Test
        fun `Given a KDOC comment on a newline`() {
            val code =
                """
                fun <T>
                /**
                  * some comment
                  */
                foo(t: T) = "some-result"
                """.trimIndent()
            discouragedCommentLocationRuleAssertThat(code)
                .hasLintViolationWithoutAutoCorrect(2, 1, "No comment expected at this location")
        }
    }

    @Nested
    inner class `Given a comment between IF CONDITION and THEN` {
        @Test
        fun `Given EOL comment on same line as CONDITION`() {
            val code =
                """
                fun foo() {
                    if (true) // some comment
                        bar()
                }
                """.trimIndent()
            discouragedCommentLocationRuleAssertThat(code)
                .hasLintViolationWithoutAutoCorrect(2, 15, "No comment expected at this location")
        }

        @Test
        fun `Given EOL comment on line below CONDITION`() {
            val code =
                """
                fun foo() {
                    if (true)
                        // some comment
                        bar()
                }
                """.trimIndent()
            discouragedCommentLocationRuleAssertThat(code)
                .hasLintViolationWithoutAutoCorrect(3, 9, "No comment expected at this location")
        }

        @Test
        fun `Given block comment on same line as CONDITION`() {
            val code =
                """
                fun foo() {
                    if (true) /* some comment */
                        bar()
                }
                """.trimIndent()
            discouragedCommentLocationRuleAssertThat(code)
                .hasLintViolationWithoutAutoCorrect(2, 15, "No comment expected at this location")
        }

        @Test
        fun `Given block comment on line below CONDITION`() {
            val code =
                """
                fun foo() {
                    if (true)
                        /* some comment */
                        bar()
                }
                """.trimIndent()
            discouragedCommentLocationRuleAssertThat(code)
                .hasLintViolationWithoutAutoCorrect(3, 9, "No comment expected at this location")
        }

        @Test
        fun `Given KDOC on same line as CONDITION`() {
            val code =
                """
                fun foo() {
                    if (true) /** some comment */
                        bar()
                }
                """.trimIndent()
            discouragedCommentLocationRuleAssertThat(code)
                .hasLintViolationWithoutAutoCorrect(2, 15, "No comment expected at this location")
        }

        @Test
        fun `Given KDOC on line below CONDITION`() {
            val code =
                """
                fun foo() {
                    if (true)
                        /** some comment */
                        bar()
                }
                """.trimIndent()
            discouragedCommentLocationRuleAssertThat(code)
                .hasLintViolationWithoutAutoCorrect(3, 9, "No comment expected at this location")
        }
    }

    @Nested
    inner class `Given a comment between THEN and ELSE` {
        @Test
        fun `Given EOL comment on same line as THEN`() {
            val code =
                """
                fun foobar() {
                    if (true)
                        foo() // some comment
                    else bar()
                }
                """.trimIndent()
            discouragedCommentLocationRuleAssertThat(code)
                .hasLintViolationWithoutAutoCorrect(3, 15, "No comment expected at this location")
        }

        @Test
        fun `Given EOL comment on line below THEN`() {
            val code =
                """
                fun foobar() {
                    if (true)
                        foo()
                    // some comment
                    else bar()
                }
                """.trimIndent()
            discouragedCommentLocationRuleAssertThat(code)
                .hasLintViolationWithoutAutoCorrect(4, 5, "No comment expected at this location")
        }

        @Test
        fun `Given block comment on same line as THEN`() {
            val code =
                """
                fun foobar() {
                    if (true)
                        foo() /* some comment */
                    else bar()
                }
                """.trimIndent()
            discouragedCommentLocationRuleAssertThat(code)
                .hasLintViolationWithoutAutoCorrect(3, 15, "No comment expected at this location")
        }

        @Test
        fun `Given block comment on line below THEN`() {
            val code =
                """
                fun foobar() {
                    if (true)
                        foo()
                    /* some comment */
                    else bar()
                }
                """.trimIndent()
            discouragedCommentLocationRuleAssertThat(code)
                .hasLintViolationWithoutAutoCorrect(4, 5, "No comment expected at this location")
        }

        @Test
        fun `Given KDOC on same line as THEN`() {
            val code =
                """
                fun foobar() {
                    if (true)
                        foo() /** some comment */
                    else bar()
                }
                """.trimIndent()
            discouragedCommentLocationRuleAssertThat(code)
                .hasLintViolationWithoutAutoCorrect(3, 15, "No comment expected at this location")
        }

        @Test
        fun `Given KDOC on line below THEN`() {
            val code =
                """
                fun foobar() {
                    if (true)
                        foo()
                    /** some comment */
                    else bar()
                }
                """.trimIndent()
            discouragedCommentLocationRuleAssertThat(code)
                .hasLintViolationWithoutAutoCorrect(4, 5, "No comment expected at this location")
        }
    }

    @Nested
    inner class `Given a comment between ELSE KEYWORD and ELSE block` {
        @Test
        fun `Given EOL comment on same line as THEN`() {
            val code =
                """
                fun foobar() {
                    if (true)
                        foo()
                    else // some comment
                    if (false) bar()
                }
                """.trimIndent()
            discouragedCommentLocationRuleAssertThat(code)
                .hasLintViolationWithoutAutoCorrect(4, 10, "No comment expected at this location")
        }

        @Test
        fun `Given EOL comment on line below THEN`() {
            val code =
                """
                fun foobar() {
                    if (true)
                        foo()
                    else
                    // some comment
                    if (false) bar()
                }
                """.trimIndent()
            discouragedCommentLocationRuleAssertThat(code)
                .hasLintViolationWithoutAutoCorrect(5, 5, "No comment expected at this location")
        }

        @Test
        fun `Given block comment on same line as THEN`() {
            val code =
                """
                fun foobar() {
                    if (true)
                        foo()
                    else /* some comment */
                    if (false) bar()
                }
                """.trimIndent()
            discouragedCommentLocationRuleAssertThat(code)
                .hasLintViolationWithoutAutoCorrect(4, 10, "No comment expected at this location")
        }

        @Test
        fun `Given block comment on line below THEN`() {
            val code =
                """
                fun foobar() {
                    if (true)
                        foo()
                    else
                    /* some comment */
                    if (false) bar()
                }
                """.trimIndent()
            discouragedCommentLocationRuleAssertThat(code)
                .hasLintViolationWithoutAutoCorrect(5, 5, "No comment expected at this location")
        }

        @Test
        fun `Given KDOC on same line as THEN`() {
            val code =
                """
                fun foobar() {
                    if (true)
                        foo()
                    else /** some comment */
                    if (false) bar()
                }
                """.trimIndent()
            discouragedCommentLocationRuleAssertThat(code)
                .hasLintViolationWithoutAutoCorrect(4, 10, "No comment expected at this location")
        }

        @Test
        fun `Given KDOC on line below THEN`() {
            val code =
                """
                fun foobar() {
                    if (true)
                        foo()
                    else
                    /** some comment */
                    if (false) bar()
                }
                """.trimIndent()
            discouragedCommentLocationRuleAssertThat(code)
                .hasLintViolationWithoutAutoCorrect(5, 5, "No comment expected at this location")
        }
    }

    @Nested
    inner class `Given a value argument list ast node` {
        @Test
        fun `Given a kdoc as child of value argument list`() {
            val code =
                """
                val foo = foo(
                    /** some comment */
                )
                """.trimIndent()
            discouragedCommentLocationRuleAssertThat(code)
                .hasLintViolationWithoutAutoCorrect(2, 5, "A KDoc is not allowed on a 'value_argument_list'")
        }

        @Test
        fun `Given a comment as only child of value argument list`() {
            val code =
                """
                val foo1 = foo(
                    // some comment
                )
                val foo2 = foo(
                    /* some comment */
                )
                """.trimIndent()
            discouragedCommentLocationRuleAssertThat(code).hasNoLintViolations()
        }

        @Test
        fun `Given a comment on separate line before value argument ast node`() {
            val code =
                """
                val foo1 = foo(
                    // some comment
                    "bar"
                )
                val foo2 = foo(
                    /* some comment */
                    "bar"
                )
                """.trimIndent()
            discouragedCommentLocationRuleAssertThat(code).hasNoLintViolations()
        }

        @Test
        fun `Given a comment as last node of value argument ast node`() {
            val code =
                """
                val foo1 = foo(
                    "bar" // some comment
                )
                val foo2 = foo(
                    "bar" /* some comment */
                )
                """.trimIndent()
            @Suppress("ktlint:standard:argument-list-wrapping", "ktlint:standard:max-line-length")
            discouragedCommentLocationRuleAssertThat(code)
                .hasLintViolationsWithoutAutoCorrect(
                    LintViolation(2, 11, "A comment in a 'value_argument_list' is only allowed when placed on a separate line"),
                    LintViolation(5, 11, "A comment in a 'value_argument_list' is only allowed when placed on a separate line"),
                )
        }

        @Test
        fun `Given a comment after a comma on the same line as an value argument ast node`() {
            val code =
                """
                val foo1 = foo(
                    "bar", // some comment
                )
                val foo2 = foo(
                    "bar", /* some comment */
                )
                """.trimIndent()
            @Suppress("ktlint:standard:argument-list-wrapping", "ktlint:standard:max-line-length")
            discouragedCommentLocationRuleAssertThat(code)
                .hasLintViolationsWithoutAutoCorrect(
                    LintViolation(2, 12, "A comment in a 'value_argument_list' is only allowed when placed on a separate line"),
                    LintViolation(5, 12, "A comment in a 'value_argument_list' is only allowed when placed on a separate line"),
                )
        }

        @Test
        fun `Given a comment as last node of value argument list`() {
            val code =
                """
                val foo1 = foo(
                    "bar"
                    // some comment
                )
                val foo1 = foo(
                    "bar"
                    /* some comment */
                )
                """.trimIndent()
            discouragedCommentLocationRuleAssertThat(code).hasNoLintViolations()
        }
    }

    @Nested
    inner class `Given a value parameter list ast node` {
        @Test
        fun `Given a kdoc as child of value parameter list`() {
            val code =
                """
                class Foo(
                    /** some comment */
                )
                """.trimIndent()
            discouragedCommentLocationRuleAssertThat(code)
                .hasLintViolationWithoutAutoCorrect(2, 5, "A KDoc is not allowed on a 'value_parameter_list'")
        }

        @Test
        fun `Given a kdoc as only child of value parameter list`() {
            val code =
                """
                class Foo1(
                    // some comment
                )
                class Foo2(
                    /* some comment */
                )

                """.trimIndent()
            discouragedCommentLocationRuleAssertThat(code).hasNoLintViolations()
        }

        @Test
        fun `Given a comment as only child of value parameter list`() {
            val code =
                """
                class Foo1(
                    // some comment
                )
                class Foo2(
                    /* some comment */
                )

                """.trimIndent()
            discouragedCommentLocationRuleAssertThat(code).hasNoLintViolations()
        }

        @Test
        fun `Given a comment on separate line before value parameter ast node`() {
            val code =
                """
                class Foo1(
                    // some comment
                    val bar: Bar,
                    // some comment
                    val bar: Bar,
                )
                class Foo2(
                    /* some comment */
                    val bar: Bar,
                    /* some comment */
                    val bar: Bar,
                )
                class Foo3(
                    /** some comment */
                    val bar: Bar,
                    /** some comment */
                    val bar: Bar,
                )
                """.trimIndent()
            discouragedCommentLocationRuleAssertThat(code).hasNoLintViolations()
        }

        @Test
        fun `Given a comment inside value parameter ast node`() {
            val code =
                """
                class Foo1(
                    val bar:
                        // some comment
                        Bar
                )
                class Foo2(
                    val bar: /* some comment */ Bar
                )
                class Foo3(
                    val bar: /** some comment */ Bar
                )
                """.trimIndent()
            @Suppress("ktlint:standard:parameter-list-wrapping", "ktlint:standard:max-line-length")
            discouragedCommentLocationRuleAssertThat(code)
                .hasLintViolationsWithoutAutoCorrect(
                    LintViolation(3, 9, "A (block or EOL) comment inside or on same line after a 'value_parameter' is not allowed. It may be placed on a separate line above."),
                    LintViolation(7, 14, "A (block or EOL) comment inside or on same line after a 'value_parameter' is not allowed. It may be placed on a separate line above."),
                    LintViolation(10, 14, "A kdoc in a 'value_parameter' is only allowed when placed on a new line before this element"),
                )
        }

        @Test
        fun `Given a comment as last node of value parameter ast node`() {
            val code =
                """
                class Foo1(
                    val bar: Bar // some comment
                )
                class Foo2(
                    val bar: Bar /* some comment */
                )
                class Foo3(
                    val bar: Bar /* some comment */
                )
                """.trimIndent()
            @Suppress("ktlint:standard:parameter-list-wrapping", "ktlint:standard:max-line-length")
            discouragedCommentLocationRuleAssertThat(code)
                .hasLintViolationsWithoutAutoCorrect(
                    LintViolation(2, 18, "A (block or EOL) comment inside or on same line after a 'value_parameter' is not allowed. It may be placed on a separate line above."),
                    LintViolation(5, 18, "A (block or EOL) comment inside or on same line after a 'value_parameter' is not allowed. It may be placed on a separate line above."),
                    LintViolation(8, 18, "A (block or EOL) comment inside or on same line after a 'value_parameter' is not allowed. It may be placed on a separate line above."),
                )
        }

        @Test
        fun `Given a comment after a comma on the same line as an value parameter ast node`() {
            val code =
                """
                class Foo1(
                    val bar: Bar, // some comment
                )
                class Foo2(
                    val bar: Bar, /* some comment */
                )
                """.trimIndent()
            @Suppress("ktlint:standard:parameter-list-wrapping", "ktlint:standard:max-line-length")
            discouragedCommentLocationRuleAssertThat(code)
                .hasLintViolationsWithoutAutoCorrect(
                    LintViolation(2, 19, "A comment in a 'value_parameter_list' is only allowed when placed on a separate line"),
                    LintViolation(5, 19, "A comment in a 'value_parameter_list' is only allowed when placed on a separate line"),
                )
        }

        @Test
        fun `Given a comment as last node of value parameter list`() {
            val code =
                """
                class Foo(
                    val bar: Bar
                    // some comment
                )
                class Foo(
                    val bar: Bar
                    /* some comment */
                )
                """.trimIndent()
            discouragedCommentLocationRuleAssertThat(code).hasNoLintViolations()
        }
    }

    @Nested
    inner class `Given a type parameter list ast node` {
        @Test
        fun `Given a kdoc as child of type parameter list`() {
            val code =
                """
                class Foo<
                    /** some comment */
                    Bar>
                """.trimIndent()
            discouragedCommentLocationRuleAssertThat(code)
                .hasLintViolationWithoutAutoCorrect(2, 5, "A KDoc is not allowed on a 'type_parameter_list'")
        }

        @Test
        fun `Given a comment on separate line before type parameter ast node`() {
            val code =
                """
                class Foo1<
                    // some comment
                    Bar>
                class Foo2<
                    /* some comment */
                    Bar>
                """.trimIndent()
            discouragedCommentLocationRuleAssertThat(code).hasNoLintViolations()
        }

        @Test
        fun `Given a comment after, but on the same line as an type parameter ast node`() {
            val code =
                """
                class Foo1<
                    Bar // some comment
                    >
                class Foo2<Bar /* some comment */ >
                """.trimIndent()
            @Suppress("ktlint:standard:parameter-list-wrapping", "ktlint:standard:max-line-length")
            discouragedCommentLocationRuleAssertThat(code)
                .hasLintViolationsWithoutAutoCorrect(
                    LintViolation(2, 9, "A comment in a 'type_parameter_list' is only allowed when placed on a separate line"),
                    LintViolation(4, 16, "A comment in a 'type_parameter_list' is only allowed when placed on a separate line"),
                )
        }

        @Test
        fun `Given a comment after a comma on the same line as an type parameter ast node`() {
            val code =
                """
                class FooBar1<
                    Foo, // some comment
                    Bar>
                class FooBar2<Foo, /* some comment */ Bar>
                """.trimIndent()
            @Suppress("ktlint:standard:parameter-list-wrapping", "ktlint:standard:max-line-length")
            discouragedCommentLocationRuleAssertThat(code)
                .hasLintViolationsWithoutAutoCorrect(
                    LintViolation(2, 10, "A comment in a 'type_parameter_list' is only allowed when placed on a separate line"),
                    LintViolation(4, 20, "A comment in a 'type_parameter_list' is only allowed when placed on a separate line"),
                )
        }

        @Test
        fun `Given a comment as last node of type parameter list`() {
            val code =
                """
                class FooBar1<
                    Foo
                    // some comment
                    >
                class FooBar2<
                    Foo
                    /* some comment */
                    >
                """.trimIndent()
            discouragedCommentLocationRuleAssertThat(code).hasNoLintViolations()
        }
    }

    @Nested
    inner class `Given a type argument list ast node` {
        @Test
        fun `Given a kdoc as child of type argument list`() {
            val code =
                """
                val fooBar: FooBar<
                    /** some comment */
                    Foo, Bar>
                """.trimIndent()
            discouragedCommentLocationRuleAssertThat(code)
                .hasLintViolationWithoutAutoCorrect(2, 5, "A KDoc is not allowed on a 'type_argument_list'")
        }

        @Test
        fun `Given a comment on separate line before type projection ast node`() {
            val code =
                """
                val fooBar1: FooBar<
                    // some comment
                    Foo, Bar>
                val fooBar2: FooBar<
                    /* some comment */
                    Foo, Bar>
                """.trimIndent()
            discouragedCommentLocationRuleAssertThat(code).hasNoLintViolations()
        }

        @Test
        fun `Given a comment after a comma on the same line as an type projection ast node`() {
            val code =
                """
                val fooBar1: FooBar<Foo, // some comment
                    Bar>
                val fooBar2: FooBar<Foo, /* some comment */
                    Bar>
                """.trimIndent()
            @Suppress("ktlint:standard:parameter-list-wrapping", "ktlint:standard:max-line-length")
            discouragedCommentLocationRuleAssertThat(code)
                .hasLintViolationsWithoutAutoCorrect(
                    LintViolation(1, 26, "A comment in a 'type_argument_list' is only allowed when placed on a separate line"),
                    LintViolation(3, 26, "A comment in a 'type_argument_list' is only allowed when placed on a separate line"),
                )
        }

        @Test
        fun `Given a comment as last node of type argument list`() {
            val code =
                """
                val fooBar: FooBar<Foo, Bar
                    // some comment
                >
                val fooBar: FooBar<Foo, Bar
                    /* some comment */
                >
                """.trimIndent()
            discouragedCommentLocationRuleAssertThat(code).hasNoLintViolations()
        }
    }

    @Nested
    inner class `Given a DOT operator in a method chain` {
        @Test
        fun `Given a comment between the DOT and the next expression on same line`() {
            val code =
                """
                val foo1 = listOf("foo")./** some comment */size
                val foo2 = listOf("foo")./** some comment */single()
                val foo3 = listOf("foo")./* some comment */size
                val foo4 = listOf("foo")./* some comment */single()
                """.trimIndent()
            discouragedCommentLocationRuleAssertThat(code)
                .hasLintViolationsWithoutAutoCorrect(
                    LintViolation(1, 26, "No comment expected at this location"),
                    LintViolation(2, 26, "No comment expected at this location"),
                    LintViolation(3, 26, "No comment expected at this location"),
                    LintViolation(4, 26, "No comment expected at this location"),
                )
        }

        @Test
        fun `Given a comment between the DOT and the next expression on a separate line`() {
            val code =
                """
                val foo1 = listOf("foo").
                    /** some comment */
                    size
                val foo2 = listOf("foo").
                    /** some comment */
                    single()
                val foo3 = listOf("foo").
                    /* some comment */
                    size
                val foo4 = listOf("foo").
                    /* some comment */
                    single()
                val foo5 = listOf("foo").
                    // some comment
                    size
                val foo6 = listOf("foo").
                    // some comment
                    single()
                """.trimIndent()
            discouragedCommentLocationRuleAssertThat(code)
                .hasLintViolationsWithoutAutoCorrect(
                    LintViolation(2, 5, "No comment expected at this location"),
                    LintViolation(5, 5, "No comment expected at this location"),
                    LintViolation(8, 5, "No comment expected at this location"),
                    LintViolation(11, 5, "No comment expected at this location"),
                    LintViolation(14, 5, "No comment expected at this location"),
                    LintViolation(17, 5, "No comment expected at this location"),
                )
        }

        @Test
        fun `Given a comment after the DOT and the next expression on a separate line`() {
            val code =
                """
                val foo1 = listOf("foo"). /** some comment */
                    size
                val foo2 = listOf("foo"). /** some comment */
                    single()
                val foo3 = listOf("foo"). /* some comment */
                    size
                val foo4 = listOf("foo"). /* some comment */
                    single()
                val foo5 = listOf("foo"). // some comment
                    size
                val foo6 = listOf("foo"). // some comment
                    single()
                """.trimIndent()
            discouragedCommentLocationRuleAssertThat(code)
                .hasLintViolationsWithoutAutoCorrect(
                    LintViolation(1, 27, "No comment expected at this location"),
                    LintViolation(3, 27, "No comment expected at this location"),
                    LintViolation(5, 27, "No comment expected at this location"),
                    LintViolation(7, 27, "No comment expected at this location"),
                    LintViolation(9, 27, "No comment expected at this location"),
                    LintViolation(11, 27, "No comment expected at this location"),
                )
        }
    }

    @Test
    fun `Given value argument preceded by a KDOC plus an EOL or BLOCK comments on separate lines above`() {
        val code =
            """
            class Foo(
                /** Some kdoc */
                /* Some comment */
                val bar1: Int,
                /** Some kdoc */
                // Some comment
                val bar2: Int,
                /* Some comment */
                /** Some kdoc */
                val bar3: Int,
                // Some comment
                /** Some kdoc */
                val bar4: Int,
            )
            """.trimIndent()
        @Suppress("ktlint:standard:parameter-list-wrapping", "ktlint:standard:max-line-length")
        discouragedCommentLocationRuleAssertThat(code)
            .hasLintViolationsWithoutAutoCorrect(
                LintViolation(3, 5, "A (block or EOL) comment inside or on same line after a 'value_parameter' is not allowed. It may be placed on a separate line above."),
                LintViolation(6, 5, "A (block or EOL) comment inside or on same line after a 'value_parameter' is not allowed. It may be placed on a separate line above."),
            )
    }
}
