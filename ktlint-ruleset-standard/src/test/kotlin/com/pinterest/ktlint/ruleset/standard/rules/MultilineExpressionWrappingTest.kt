package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.rule.engine.core.api.editorconfig.CODE_STYLE_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.CodeStyleValue
import com.pinterest.ktlint.test.KtLintAssertThat
import com.pinterest.ktlint.test.LintViolation
import com.pinterest.ktlint.test.MULTILINE_STRING_QUOTE
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class MultilineExpressionWrappingTest {
    private val multilineExpressionWrappingAssertThat = KtLintAssertThat.assertThatRule { MultilineExpressionWrapping() }

    @Nested
    inner class `Given a function call using a named parameter` {
        @Test
        fun `Given value argument for a named parameter in a function with a multiline dot qualified expression on the same line as the assignment`() {
            val code =
                """
                val foo = foo(
                    parameterName = "The quick brown fox "
                        .plus("jumps ")
                        .plus("over the lazy dog"),
                )
                """.trimIndent()
            val formattedCode =
                """
                val foo =
                    foo(
                        parameterName =
                            "The quick brown fox "
                                .plus("jumps ")
                                .plus("over the lazy dog"),
                    )
                """.trimIndent()
            multilineExpressionWrappingAssertThat(code)
                .addAdditionalRuleProvider { IndentationRule() }
                .withEditorConfigOverride(CODE_STYLE_PROPERTY to CodeStyleValue.ktlint_official)
                .hasLintViolations(
                    LintViolation(1, 11, "A multiline expression should start on a new line"),
                    LintViolation(2, 21, "A multiline expression should start on a new line"),
                ).isFormattedAs(formattedCode)
        }

        @Test
        fun `Given value argument in a function with a multiline safe access expression on the same line as the assignment`() {
            val code =
                """
                val foo = foo(
                    parameterName = theQuickBrownFoxOrNull
                        ?.plus("jumps ")
                        ?.plus("over the lazy dog"),
                )
                """.trimIndent()
            val formattedCode =
                """
                val foo =
                    foo(
                        parameterName =
                            theQuickBrownFoxOrNull
                                ?.plus("jumps ")
                                ?.plus("over the lazy dog"),
                    )
                """.trimIndent()
            multilineExpressionWrappingAssertThat(code)
                .addAdditionalRuleProvider { IndentationRule() }
                .withEditorConfigOverride(CODE_STYLE_PROPERTY to CodeStyleValue.ktlint_official)
                .hasLintViolations(
                    LintViolation(1, 11, "A multiline expression should start on a new line"),
                    LintViolation(2, 21, "A multiline expression should start on a new line"),
                ).isFormattedAs(formattedCode)
        }

        @Test
        fun `Given value argument in a function with a multiline combination of a safe access expression and a call expression on the same line as the assignment`() {
            val code =
                """
                val foo = foo(
                    parameterName = theQuickBrownFoxOrNull()
                        ?.plus("jumps ")
                        ?.plus("over the lazy dog"),
                )
                """.trimIndent()
            val formattedCode =
                """
                val foo =
                    foo(
                        parameterName =
                            theQuickBrownFoxOrNull()
                                ?.plus("jumps ")
                                ?.plus("over the lazy dog"),
                    )
                """.trimIndent()
            multilineExpressionWrappingAssertThat(code)
                .addAdditionalRuleProvider { IndentationRule() }
                .withEditorConfigOverride(CODE_STYLE_PROPERTY to CodeStyleValue.ktlint_official)
                .hasLintViolations(
                    LintViolation(1, 11, "A multiline expression should start on a new line"),
                    LintViolation(2, 21, "A multiline expression should start on a new line"),
                ).isFormattedAs(formattedCode)
        }

        @Test
        fun `Given value argument in a function with a multiline combination of a dot qualified and a safe access expression on the same line as the assignment`() {
            val code =
                """
                val foo = foo(
                    parameterName = "The quick brown fox "
                        .takeIf { it.jumps }
                        ?.plus("jumps over the lazy dog"),
                )
                """.trimIndent()
            val formattedCode =
                """
                val foo =
                    foo(
                        parameterName =
                            "The quick brown fox "
                                .takeIf { it.jumps }
                                ?.plus("jumps over the lazy dog"),
                    )
                """.trimIndent()
            multilineExpressionWrappingAssertThat(code)
                .addAdditionalRuleProvider { IndentationRule() }
                .withEditorConfigOverride(CODE_STYLE_PROPERTY to CodeStyleValue.ktlint_official)
                .hasLintViolations(
                    LintViolation(1, 11, "A multiline expression should start on a new line"),
                    LintViolation(2, 21, "A multiline expression should start on a new line"),
                ).isFormattedAs(formattedCode)
        }

        @Test
        fun `Given value argument in a function with a multiline call expression on the same line as the assignment`() {
            val code =
                """
                val foo = foo(
                    parameterName = bar(
                        "bar"
                    )
                )
                """.trimIndent()
            val formattedCode =
                """
                val foo =
                    foo(
                        parameterName =
                            bar(
                                "bar"
                            )
                    )
                """.trimIndent()
            multilineExpressionWrappingAssertThat(code)
                .addAdditionalRuleProvider { IndentationRule() }
                .withEditorConfigOverride(CODE_STYLE_PROPERTY to CodeStyleValue.ktlint_official)
                .hasLintViolations(
                    LintViolation(1, 11, "A multiline expression should start on a new line"),
                    LintViolation(2, 21, "A multiline expression should start on a new line"),
                ).isFormattedAs(formattedCode)
        }
    }

    @Nested
    inner class `Given a function call using an unnamed parameter` {
        @Test
        fun `Given value argument in a function with a multiline binary expression on the same line as the assignment`() {
            val code =
                """
                val foo = foo("The quick brown fox " +
                    "jumps " +
                    "over the lazy dog",
                )
                """.trimIndent()
            val formattedCode =
                """
                val foo =
                    foo(
                        "The quick brown fox " +
                            "jumps " +
                            "over the lazy dog",
                    )
                """.trimIndent()
            multilineExpressionWrappingAssertThat(code)
                .addAdditionalRuleProvider { IndentationRule() }
                .withEditorConfigOverride(CODE_STYLE_PROPERTY to CodeStyleValue.ktlint_official)
                .hasLintViolations(
                    LintViolation(1, 11, "A multiline expression should start on a new line"),
                    LintViolation(1, 15, "A multiline expression should start on a new line"),
                ).isFormattedAs(formattedCode)
        }

        @Test
        fun `Given value argument in a function with a multiline safe access expression on the same line as the assignment`() {
            val code =
                """
                val foo = foo(theQuickBrownFoxOrNull
                    ?.plus("jumps ")
                    ?.plus("over the lazy dog"),
                )
                """.trimIndent()
            val formattedCode =
                """
                val foo =
                    foo(
                        theQuickBrownFoxOrNull
                            ?.plus("jumps ")
                            ?.plus("over the lazy dog"),
                    )
                """.trimIndent()
            multilineExpressionWrappingAssertThat(code)
                .addAdditionalRuleProvider { IndentationRule() }
                .withEditorConfigOverride(CODE_STYLE_PROPERTY to CodeStyleValue.ktlint_official)
                .hasLintViolations(
                    LintViolation(1, 11, "A multiline expression should start on a new line"),
                    LintViolation(1, 15, "A multiline expression should start on a new line"),
                ).isFormattedAs(formattedCode)
        }

        @Test
        fun `Given value argument in a function with a multiline combination of a safe access expression and a call expression on the same line as the assignment`() {
            val code =
                """
                val foo = foo(theQuickBrownFoxOrNull()
                    ?.plus("jumps ")
                    ?.plus("over the lazy dog"),
                )
                """.trimIndent()
            val formattedCode =
                """
                val foo =
                    foo(
                        theQuickBrownFoxOrNull()
                            ?.plus("jumps ")
                            ?.plus("over the lazy dog"),
                    )
                """.trimIndent()
            multilineExpressionWrappingAssertThat(code)
                .addAdditionalRuleProvider { IndentationRule() }
                .withEditorConfigOverride(CODE_STYLE_PROPERTY to CodeStyleValue.ktlint_official)
                .hasLintViolations(
                    LintViolation(1, 11, "A multiline expression should start on a new line"),
                    LintViolation(1, 15, "A multiline expression should start on a new line"),
                ).isFormattedAs(formattedCode)
        }

        @Test
        fun `Given value argument in a function with a multiline combination of a dot qualified and a safe access expression on the same line as the assignment`() {
            val code =
                """
                val foo = foo("The quick brown fox "
                    .takeIf { it.jumps }
                    ?.plus("jumps over the lazy dog"),
                )
                """.trimIndent()
            val formattedCode =
                """
                val foo =
                    foo(
                        "The quick brown fox "
                            .takeIf { it.jumps }
                            ?.plus("jumps over the lazy dog"),
                    )
                """.trimIndent()
            multilineExpressionWrappingAssertThat(code)
                .addAdditionalRuleProvider { IndentationRule() }
                .withEditorConfigOverride(CODE_STYLE_PROPERTY to CodeStyleValue.ktlint_official)
                .hasLintViolations(
                    LintViolation(1, 11, "A multiline expression should start on a new line"),
                    LintViolation(1, 15, "A multiline expression should start on a new line"),
                ).isFormattedAs(formattedCode)
        }

        @Test
        fun `Given value argument in a function with a multiline call expression on the same line as the assignment`() {
            val code =
                """
                val foo = foo(bar(
                    "bar"
                ))
                """.trimIndent()
            val formattedCode =
                """
                val foo =
                    foo(
                        bar(
                            "bar"
                        )
                    )
                """.trimIndent()
            multilineExpressionWrappingAssertThat(code)
                .addAdditionalRuleProvider { IndentationRule() }
                .addAdditionalRuleProvider { ParameterWrappingRule() }
                .withEditorConfigOverride(CODE_STYLE_PROPERTY to CodeStyleValue.ktlint_official)
                .hasLintViolations(
                    LintViolation(1, 11, "A multiline expression should start on a new line"),
                    LintViolation(1, 15, "A multiline expression should start on a new line"),
                ).isFormattedAs(formattedCode)
        }
    }

    @Test
    fun `Given a return statement with a multiline expression then do not reformat as it would result in a compilation error`() {
        val code =
            """
            fun foo() {
                return bar(
                    "bar"
                )
            }
            """.trimIndent()
        multilineExpressionWrappingAssertThat(code)
            .withEditorConfigOverride(CODE_STYLE_PROPERTY to CodeStyleValue.ktlint_official)
            .hasNoLintViolations()
    }

    @Test
    fun `Given a function with a multiline body expression`() {
        val code =
            """
            fun foo() = bar(
                "bar"
            )
            """.trimIndent()
        val formattedCode =
            """
            fun foo() =
                bar(
                    "bar"
                )
            """.trimIndent()
        multilineExpressionWrappingAssertThat(code)
            .addAdditionalRuleProvider { IndentationRule() }
            .withEditorConfigOverride(CODE_STYLE_PROPERTY to CodeStyleValue.ktlint_official)
            .hasLintViolation(1, 13, "A multiline expression should start on a new line")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a function with a multiline signature without a return type but with a multiline expression body starting on same line as closing parenthesis of function`() {
        val code =
            """
            fun foo(
                foobar: String
            ) = bar(
                foobar
            )
            """.trimIndent()
        multilineExpressionWrappingAssertThat(code)
            .addAdditionalRuleProvider { IndentationRule() }
            .withEditorConfigOverride(CODE_STYLE_PROPERTY to CodeStyleValue.ktlint_official)
            .hasNoLintViolations()
    }

    @Test
    fun `Given a function with a multiline lambda expression containing a binary expression`() {
        val code =
            """
            val string: String
                by lazy { "The quick brown fox " +
                              "jumps " +
                              "over the lazy dog"
                }
            """.trimIndent()
        val formattedCode =
            """
            val string: String
                by lazy {
                    "The quick brown fox " +
                        "jumps " +
                        "over the lazy dog"
                }
            """.trimIndent()
        multilineExpressionWrappingAssertThat(code)
            .addAdditionalRuleProvider { IndentationRule() }
            .withEditorConfigOverride(CODE_STYLE_PROPERTY to CodeStyleValue.ktlint_official)
            .hasLintViolation(2, 15, "A multiline expression should start on a new line")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a function with a lambda expression containing a multiline string template`() {
        val code =
            """
            val string: String
                by lazy { ${MULTILINE_STRING_QUOTE}The quick brown fox
                    jumps
                    over the lazy dog$MULTILINE_STRING_QUOTE.trimIndent()
                }
            """.trimIndent()
        val formattedCode =
            """
            val string: String
                by lazy {
                    ${MULTILINE_STRING_QUOTE}The quick brown fox
                    jumps
                    over the lazy dog$MULTILINE_STRING_QUOTE.trimIndent()
                }
            """.trimIndent()
        multilineExpressionWrappingAssertThat(code)
            .addAdditionalRuleProvider { IndentationRule() }
            .withEditorConfigOverride(CODE_STYLE_PROPERTY to CodeStyleValue.ktlint_official)
            .hasLintViolation(2, 15, "A multiline expression should start on a new line")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a function with a multiline lambda expression`() {
        val code =
            """
            val string =
                listOf("The quick brown fox", "jumps", "over the lazy dog")
                    .map { it
                        .lowercase()
                        .substringAfter("o")
                    }
            """.trimIndent()
        val formattedCode =
            """
            val string =
                listOf("The quick brown fox", "jumps", "over the lazy dog")
                    .map {
                        it
                            .lowercase()
                            .substringAfter("o")
                    }
            """.trimIndent()
        multilineExpressionWrappingAssertThat(code)
            .addAdditionalRuleProvider { IndentationRule() }
            .withEditorConfigOverride(CODE_STYLE_PROPERTY to CodeStyleValue.ktlint_official)
            .hasLintViolation(3, 16, "A multiline expression should start on a new line")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a multiline string template in a function parameter to a new line`() {
        val code =
            """
            fun someFunction() {
                println($MULTILINE_STRING_QUOTE
                        The quick brown fox
                        jumps over the lazy dog
                        $MULTILINE_STRING_QUOTE.trimIndent())
            }
            """.trimIndent()
        val formattedCode =
            """
            fun someFunction() {
                println(
                    $MULTILINE_STRING_QUOTE
                        The quick brown fox
                        jumps over the lazy dog
                    $MULTILINE_STRING_QUOTE.trimIndent()
                )
            }
            """.trimIndent()
        multilineExpressionWrappingAssertThat(code)
            .addAdditionalRuleProvider { IndentationRule() }
            .withEditorConfigOverride(CODE_STYLE_PROPERTY to CodeStyleValue.ktlint_official)
            .hasLintViolation(2, 13, "A multiline expression should start on a new line")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a multiline string template after an arrow`() {
        val code =
            """
            fun foo(bar: String) =
                when (bar) {
                    "bar bar bar bar bar bar bar bar bar" -> $MULTILINE_STRING_QUOTE
                        The quick brown fox
                        jumps over the lazy dog
                        $MULTILINE_STRING_QUOTE.trimIndent()
                    else -> ""
                }
            """.trimIndent()
        val formattedCode =
            """
            fun foo(bar: String) =
                when (bar) {
                    "bar bar bar bar bar bar bar bar bar" ->
                        $MULTILINE_STRING_QUOTE
                        The quick brown fox
                        jumps over the lazy dog
                        $MULTILINE_STRING_QUOTE.trimIndent()
                    else -> ""
                }
            """.trimIndent()
        multilineExpressionWrappingAssertThat(code)
            .addAdditionalRuleProvider { IndentationRule() }
            .withEditorConfigOverride(CODE_STYLE_PROPERTY to CodeStyleValue.ktlint_official)
            .hasLintViolation(3, 50, "A multiline expression should start on a new line")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Move a multiline when statement as part of an assignment`() {
        val code =
            """
            fun foo(bar: String) = when (bar) {
                "bar" -> true
                else -> false
            }
            """.trimIndent()
        val formattedCode =
            """
            fun foo(bar: String) =
                when (bar) {
                    "bar" -> true
                    else -> false
                }
            """.trimIndent()
        multilineExpressionWrappingAssertThat(code)
            .addAdditionalRuleProvider { IndentationRule() }
            .withEditorConfigOverride(CODE_STYLE_PROPERTY to CodeStyleValue.ktlint_official)
            .hasLintViolation(1, 24, "A multiline expression should start on a new line")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Move a multiline if statement as part of an assignment`() {
        val code =
            """
            fun foo(bar: Boolean) = if (bar) {
                "bar"
            } else {
                "foo"
            }
            """.trimIndent()
        val formattedCode =
            """
            fun foo(bar: Boolean) =
                if (bar) {
                    "bar"
                } else {
                    "foo"
                }
            """.trimIndent()
        multilineExpressionWrappingAssertThat(code)
            .addAdditionalRuleProvider { IndentationRule() }
            .withEditorConfigOverride(CODE_STYLE_PROPERTY to CodeStyleValue.ktlint_official)
            .hasLintViolation(1, 25, "A multiline expression should start on a new line")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Move a multiline try catch as part of an assignment`() {
        val code =
            """
            fun foo() = try {
                // do something that might cause an exception
            } catch(e: Exception) {
                // handle exception
            }
            """.trimIndent()
        val formattedCode =
            """
            fun foo() =
                try {
                    // do something that might cause an exception
                } catch(e: Exception) {
                    // handle exception
                }
            """.trimIndent()
        multilineExpressionWrappingAssertThat(code)
            .addAdditionalRuleProvider { IndentationRule() }
            .withEditorConfigOverride(CODE_STYLE_PROPERTY to CodeStyleValue.ktlint_official)
            .hasLintViolation(1, 13, "A multiline expression should start on a new line")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Move a multiline is-expression as part of an assignment`() {
        val code =
            """
            fun foo(any: Any) = any is
                Foo
            """.trimIndent()
        // TODO: Fix formatting by indent rule
        val formattedCode =
            """
            fun foo(any: Any) =
                any is
                Foo
            """.trimIndent()
        multilineExpressionWrappingAssertThat(code)
            .addAdditionalRuleProvider { IndentationRule() }
            .withEditorConfigOverride(CODE_STYLE_PROPERTY to CodeStyleValue.ktlint_official)
            .hasLintViolation(1, 21, "A multiline expression should start on a new line")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Move a multiline binary with type as part of an assignment`() {
        val code =
            """
            fun foo(any: Any) = any as
                Foo
            """.trimIndent()
        val formattedCode =
            """
            fun foo(any: Any) =
                any as
                    Foo
            """.trimIndent()
        multilineExpressionWrappingAssertThat(code)
            .addAdditionalRuleProvider { IndentationRule() }
            .withEditorConfigOverride(CODE_STYLE_PROPERTY to CodeStyleValue.ktlint_official)
            .hasLintViolation(1, 21, "A multiline expression should start on a new line")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Move a multiline prefix expression as part of an assignment`() {
        val code =
            """
            fun foo(any: Int) = ++
                42
            """.trimIndent()
        // TODO: Fix formatting by indent rule
        val formattedCode =
            """
            fun foo(any: Int) =
                ++
                42
            """.trimIndent()
        multilineExpressionWrappingAssertThat(code)
            .addAdditionalRuleProvider { IndentationRule() }
            .withEditorConfigOverride(CODE_STYLE_PROPERTY to CodeStyleValue.ktlint_official)
            .hasLintViolation(1, 21, "A multiline expression should start on a new line")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Move a multiline array access expression as part of an assignment`() {
        val code =
            """
            fun foo(any: Array<String>) = any[
                42
            ]
            """.trimIndent()
        val formattedCode =
            """
            fun foo(any: Array<String>) =
                any[
                    42
                ]
            """.trimIndent()
        multilineExpressionWrappingAssertThat(code)
            .addAdditionalRuleProvider { IndentationRule() }
            .withEditorConfigOverride(CODE_STYLE_PROPERTY to CodeStyleValue.ktlint_official)
            .hasLintViolation(1, 31, "A multiline expression should start on a new line")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Move a multiline object literal as part of an assignment`() {
        val code =
            """
            fun foo() = object :
                Foo() {}
            """.trimIndent()
        val formattedCode =
            """
            fun foo() =
                object :
                    Foo() {}
            """.trimIndent()
        multilineExpressionWrappingAssertThat(code)
            .addAdditionalRuleProvider { IndentationRule() }
            .withEditorConfigOverride(CODE_STYLE_PROPERTY to CodeStyleValue.ktlint_official)
            .hasLintViolation(1, 13, "A multiline expression should start on a new line")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a lambda expression with a multiline expression starting on a new line then do not report a violation`() {
        val code =
            """
            val foo =
                listOf("foo")
                    .let { bar ->
                        if (fooBar > 42) {
                            "foo"
                        } else {
                            "bar"
                        }
                    }
            """.trimIndent()
        multilineExpressionWrappingAssertThat(code)
            .addAdditionalRuleProvider { IndentationRule() }
            .withEditorConfigOverride(CODE_STYLE_PROPERTY to CodeStyleValue.ktlint_official)
            .hasNoLintViolations()
    }

    @Test
    fun `Given a multiline expression with an EOL comment on the last line`() {
        val code =
            """
            val foo = bar
                .length() // some-comment

            val foobar = "foobar"
            """.trimIndent()
        val formattedCode =
            """
            val foo =
                bar
                    .length() // some-comment

            val foobar = "foobar"
            """.trimIndent()
        multilineExpressionWrappingAssertThat(code)
            .addAdditionalRuleProvider { IndentationRule() }
            .withEditorConfigOverride(CODE_STYLE_PROPERTY to CodeStyleValue.ktlint_official)
            .hasLintViolation(1, 11, "A multiline expression should start on a new line")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given an assignment to variable`() {
        val code =
            """
            fun foo() {
                var givenCode: String

                givenCode = $MULTILINE_STRING_QUOTE
                    some text
                    $MULTILINE_STRING_QUOTE.trimIndent()
            }
            """.trimIndent()
        val formattedCode =
            """
            fun foo() {
                var givenCode: String

                givenCode =
                    $MULTILINE_STRING_QUOTE
                    some text
                    $MULTILINE_STRING_QUOTE.trimIndent()
            }
            """.trimIndent()
        multilineExpressionWrappingAssertThat(code)
            .addAdditionalRuleProvider { IndentationRule() }
            .withEditorConfigOverride(CODE_STYLE_PROPERTY to CodeStyleValue.ktlint_official)
            .hasLintViolation(4, 17, "A multiline expression should start on a new line")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a comparison in which the right hand side is a multiline expression`() {
        val code =
            """
            fun foo(bar: String): Boolean {
                return bar != $MULTILINE_STRING_QUOTE
                    some text
                $MULTILINE_STRING_QUOTE.trimIndent()
            }
            """.trimIndent()
        val formattedCode =
            """
            fun foo(bar: String): Boolean {
                return bar !=
                    $MULTILINE_STRING_QUOTE
                    some text
                    $MULTILINE_STRING_QUOTE.trimIndent()
            }
            """.trimIndent()
        multilineExpressionWrappingAssertThat(code)
            .addAdditionalRuleProvider { IndentationRule() }
            .withEditorConfigOverride(CODE_STYLE_PROPERTY to CodeStyleValue.ktlint_official)
            .hasLintViolation(2, 19, "A multiline expression should start on a new line")
            .isFormattedAs(formattedCode)
    }

    fun fooBar(foobar: String?) =
        foobar
            ?.lowercase()
            ?: """
                foo
                """
                .uppercase()
                .trimIndent()

    @Test
    fun `Given an elvis operator followed by a multiline expression then do not reformat`() {
        val code =
            """
            fun fooBar(foobar: String?, bar: String) =
                foo
                    ?.lowercase()
                    ?: bar
                        .uppercase()
                        .trimIndent()
            """.trimIndent()
        multilineExpressionWrappingAssertThat(code)
            .addAdditionalRuleProvider { IndentationRule() }
            .hasNoLintViolations()
    }
}
