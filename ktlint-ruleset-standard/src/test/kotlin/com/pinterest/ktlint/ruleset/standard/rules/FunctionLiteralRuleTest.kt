package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.EOL_CHAR
import com.pinterest.ktlint.test.KtLintAssertThat.Companion.MAX_LINE_LENGTH_MARKER
import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRule
import com.pinterest.ktlint.test.LintViolation
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class FunctionLiteralRuleTest {
    private val functionLiteralRuleAssertThat = assertThatRule { FunctionLiteralRule() }

    @Test
    fun `Given a single line lambda without parameters`() {
        val code =
            """
            val foobar = { foo + bar }
            """.trimIndent()
        functionLiteralRuleAssertThat(code).hasNoLintViolations()
    }

    @Nested
    inner class `Given a multiline lambda without parameters` {
        @Test
        fun `Given max line length is not set then rewrite to single line is not enforced`() {
            val code =
                """
                val foobar =
                    {
                        foo + bar
                    }
                """.trimIndent()
            functionLiteralRuleAssertThat(code)
                .unsetMaxLineLength()
                .hasNoLintViolations()
        }

        @Test
        fun `Given max line length is set and expression fits on single line then rewrite to single line`() {
            val code =
                """
                // $MAX_LINE_LENGTH_MARKER           $EOL_CHAR
                val foobar =
                    {
                        fooooooooooooooo + bar
                    }
                """.trimIndent()
            val formattedCode =
                """
                // $MAX_LINE_LENGTH_MARKER           $EOL_CHAR
                val foobar =
                    { fooooooooooooooo + bar }
                """.trimIndent()
            functionLiteralRuleAssertThat(code)
                .setMaxLineLength()
                .hasLintViolations(
                    LintViolation(3, 6, "Unexpected newline as function literal fits on single line"),
                    LintViolation(4, 31, "Unexpected newline as function literal fits on single line"),
                ).isFormattedAs(formattedCode)
        }
    }

    @Nested
    inner class `Given a multiline lambda with a single parameter` {
        @Test
        fun `Given max line length is not set then rewrite to single line is not enforced`() {
            val code =
                """
                val foobar =
                    { foo: Foo ->
                        foo.repeat(2)
                    }
                """.trimIndent()
            functionLiteralRuleAssertThat(code)
                .unsetMaxLineLength()
                .hasNoLintViolations()
        }

        @Test
        fun `Given max line length is set and expression fits on single line then rewrite to single line`() {
            val code =
                """
                // $MAX_LINE_LENGTH_MARKER      $EOL_CHAR
                val foobar =
                    { foo: Foo ->
                        foo.repeat(2)
                    }
                """.trimIndent()
            val formattedCode =
                """
                // $MAX_LINE_LENGTH_MARKER      $EOL_CHAR
                val foobar =
                    { foo: Foo -> foo.repeat(2) }
                """.trimIndent()
            functionLiteralRuleAssertThat(code)
                .setMaxLineLength()
                .hasLintViolations(
                    LintViolation(3, 18, "Unexpected newline as function literal fits on single line"),
                    LintViolation(4, 22, "Unexpected newline as function literal fits on single line"),
                ).isFormattedAs(formattedCode)
        }

        @Test
        fun `Given max line length is set and expression does not fit on single line then do not rewrite to single line`() {
            val code =
                """
                // $MAX_LINE_LENGTH_MARKER   $EOL_CHAR
                val foobar =
                    { foo: Foo ->
                        foo.repeat(2).repeat(3)
                    }
                """.trimIndent()
            functionLiteralRuleAssertThat(code)
                .setMaxLineLength()
                .hasNoLintViolations()
        }
    }

    @Nested
    inner class `Given a multiline lambda with a single parameter not on same line as opening brace` {
        @Test
        fun `Given max line length is not set then place parameters on same line as opening brace but do not rewrite to single line`() {
            val code =
                """
                val foobar =
                    {
                        foo: Foo ->
                        foo.repeat(2)
                    }
                """.trimIndent()
            val formattedCode =
                """
                val foobar =
                    { foo: Foo ->
                        foo.repeat(2)
                    }
                """.trimIndent()
            functionLiteralRuleAssertThat(code)
                .unsetMaxLineLength()
                .hasLintViolation(3, 9, "Unexpected newline before parameter")
                .isFormattedAs(formattedCode)
        }

        @Test
        fun `Given max line length is set and expression fits on single line then rewrite to single line`() {
            val code =
                """
                // $MAX_LINE_LENGTH_MARKER      $EOL_CHAR
                val foobar =
                    {
                        foo: Foo ->
                        foo.repeat(2)
                    }
                """.trimIndent()
            val formattedCode =
                """
                // $MAX_LINE_LENGTH_MARKER      $EOL_CHAR
                val foobar =
                    { foo: Foo -> foo.repeat(2) }
                """.trimIndent()
            functionLiteralRuleAssertThat(code)
                .hasLintViolations(
                    LintViolation(3, 6, "Unexpected newline as function literal fits on single line"),
                    LintViolation(4, 20, "Unexpected newline as function literal fits on single line"),
                    LintViolation(5, 22, "Unexpected newline as function literal fits on single line"),
                ).isFormattedAs(formattedCode)
        }
    }

    @Nested
    inner class `Given a multiline lambda with a single parameter and arrow on separate line` {
        @Test
        fun `Given max line length is not set then move arrow to same line as parameter`() {
            val code =
                """
                val foobar =
                    { foo: Foo
                        ->
                        foo.repeat(2)
                    }
                """.trimIndent()
            val formattedCode =
                """
                val foobar =
                    { foo: Foo ->
                        foo.repeat(2)
                    }
                """.trimIndent()
            functionLiteralRuleAssertThat(code)
                .unsetMaxLineLength()
                .hasLintViolation(2, 15, "Unexpected newline after parameter")
                .isFormattedAs(formattedCode)
        }

        @Test
        fun `Given max line length is set and expression fits on single line then rewrite as single line expression`() {
            val code =
                """
                // $MAX_LINE_LENGTH_MARKER      $EOL_CHAR
                val foobar =
                    { foo: Foo
                        ->
                        foo.repeat(2)
                    }
                """.trimIndent()
            val formattedCode =
                """
                // $MAX_LINE_LENGTH_MARKER      $EOL_CHAR
                val foobar =
                    { foo: Foo -> foo.repeat(2) }
                """.trimIndent()
            functionLiteralRuleAssertThat(code)
                .hasLintViolations(
                    LintViolation(3, 15, "Unexpected newline as function literal fits on single line"),
                    LintViolation(4, 11, "Unexpected newline as function literal fits on single line"),
                    LintViolation(5, 22, "Unexpected newline as function literal fits on single line"),
                ).isFormattedAs(formattedCode)
        }

        @Test
        fun `Given max line length is set and expression does not fit on single line then move arrow to same line as parameter list`() {
            val code =
                """
                // $MAX_LINE_LENGTH_MARKER     $EOL_CHAR
                val foobar =
                    { foo: Foo
                        ->
                        foo.repeat(2)
                    }
                """.trimIndent()
            val formattedCode =
                """
                // $MAX_LINE_LENGTH_MARKER     $EOL_CHAR
                val foobar =
                    { foo: Foo ->
                        foo.repeat(2)
                    }
                """.trimIndent()
            functionLiteralRuleAssertThat(code)
                .setMaxLineLength()
                .hasLintViolation(3, 15, "Unexpected newline after parameter")
                .isFormattedAs(formattedCode)
        }
    }

    @Nested
    inner class `Given a multiline lambda with multiple parameters on same line` {
        @Test
        fun `Given max line length is not set then do not rewrite as single line`() {
            val code =
                """
                val foobar =
                    { foo: Foo, bar: Bar ->
                        foo + bar
                    }
                """.trimIndent()
            functionLiteralRuleAssertThat(code)
                .unsetMaxLineLength()
                .hasNoLintViolations()
        }

        @Test
        fun `Given max line length is set and parameter list fits on single line then do not rewrite`() {
            val code =
                """
                // $MAX_LINE_LENGTH_MARKER   $EOL_CHAR
                val foobar =
                    { fooooo: Foo, bar: Bar ->
                        foo + bar
                    }
                """.trimIndent()
            functionLiteralRuleAssertThat(code)
                .setMaxLineLength()
                .hasNoLintViolations()
        }

        @Test
        fun `Given max line length is set and parameter list does not fit on single line then rewrite as multiline expression`() {
            val code =
                """
                // $MAX_LINE_LENGTH_MARKER  $EOL_CHAR
                val foobar =
                    { fooooo: Foo, bar: Bar ->
                        foo + bar
                    }
                """.trimIndent()
            val formattedCode =
                """
                // $MAX_LINE_LENGTH_MARKER  $EOL_CHAR
                val foobar =
                    {
                        fooooo: Foo,
                        bar: Bar
                        ->
                        foo + bar
                    }
                """.trimIndent()
            functionLiteralRuleAssertThat(code)
                .setMaxLineLength()
                .hasLintViolations(
                    LintViolation(3, 7, "Newline expected before parameter"),
                    LintViolation(3, 20, "Newline expected before parameter"),
                    LintViolation(3, 29, "Newline expected before arrow"),
                ).isFormattedAs(formattedCode)
        }
    }

    @Test
    fun `Given a lambda with a multiline parameter list starting on same line as opening brace`() {
        val code =
            """
            // $MAX_LINE_LENGTH_MARKER  $EOL_CHAR
            val foobar =
                { foo: Foo,
                    bar: Bar ->
                    foo + bar
                }
            """.trimIndent()
        val formattedCode =
            """
            // $MAX_LINE_LENGTH_MARKER  $EOL_CHAR
            val foobar =
                {
                    foo: Foo,
                    bar: Bar
                    ->
                    foo + bar
                }
            """.trimIndent()
        functionLiteralRuleAssertThat(code)
            .setMaxLineLength()
            .hasLintViolations(
                LintViolation(3, 7, "Newline expected before parameter"),
                LintViolation(4, 18, "Newline expected before arrow"),
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a lambda with a multiline parameter list starting on the next line below the opening brace`() {
        val code =
            """
            // $MAX_LINE_LENGTH_MARKER  $EOL_CHAR
            val foobar =
                {
                    foo: Foo,
                    bar: Bar ->
                    foo + bar
                }
            """.trimIndent()
        val formattedCode =
            """
            // $MAX_LINE_LENGTH_MARKER  $EOL_CHAR
            val foobar =
                {
                    foo: Foo,
                    bar: Bar
                    ->
                    foo + bar
                }
            """.trimIndent()
        functionLiteralRuleAssertThat(code)
            .setMaxLineLength()
            .hasLintViolation(5, 18, "Newline expected before arrow")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a call expression followed by a lambda with a single parameter not fitting on the same line as the opening brace`() {
        val code =
            """
            // $MAX_LINE_LENGTH_MARKER  $EOL_CHAR
            val foobar =
                barrrrrrrrrr { foooooooooooo: Foo ->
                    foo.repeat(2)
                }
            """.trimIndent()
        val formattedCode =
            """
            // $MAX_LINE_LENGTH_MARKER  $EOL_CHAR
            val foobar =
                barrrrrrrrrr {
                    foooooooooooo: Foo
                    ->
                    foo.repeat(2)
                }
            """.trimIndent()
        functionLiteralRuleAssertThat(code)
            .setMaxLineLength()
            .hasLintViolations(
                LintViolation(3, 20, "Newline expected before parameter"),
                LintViolation(3, 39, "Newline expected before arrow"),
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a single line parameter list starting on the next line below the opening brace`() {
        val code =
            """
            // $MAX_LINE_LENGTH_MARKER  $EOL_CHAR
            val foobar =
                {
                  foo: Foo, bar: Bar ->
                    foo + bar
                }
            """.trimIndent()
        val formattedCode =
            """
            // $MAX_LINE_LENGTH_MARKER  $EOL_CHAR
            val foobar =
                { foo: Foo, bar: Bar ->
                    foo + bar
                }
            """.trimIndent()
        functionLiteralRuleAssertThat(code)
            .setMaxLineLength()
            .hasLintViolation(4, 7, "Unexpected newline before parameter")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a single line parameter list starting on the next line below the opening brace and arrow on separate line which can be merged to a single line after opening brace`() {
        val code =
            """
            // $MAX_LINE_LENGTH_MARKER          $EOL_CHAR
            val foobar =
                {
                  foo: Foo, bar: Bar, baz: Baz
                  ->
                    foo + bar
                }
            """.trimIndent()
        val formattedCode =
            """
            // $MAX_LINE_LENGTH_MARKER          $EOL_CHAR
            val foobar =
                { foo: Foo, bar: Bar, baz: Baz ->
                    foo + bar
                }
            """.trimIndent()
        functionLiteralRuleAssertThat(code)
            .setMaxLineLength()
            .hasLintViolations(
                LintViolation(4, 7, "Unexpected newline before parameter"),
                LintViolation(4, 35, "Unexpected newline after parameter"),
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a single line parameter list starting on the next line below the opening brace and arrow on separate line which can not be merged to a single line after opening brace`() {
        val code =
            """
            // $MAX_LINE_LENGTH_MARKER         $EOL_CHAR
            val foobar =
                {
                    foo: Foo, bar: Bar, baz: Baz
                    ->
                    foo + bar
                }
            """.trimIndent()
        val formattedCode =
            """
            // $MAX_LINE_LENGTH_MARKER         $EOL_CHAR
            val foobar =
                {
                    foo: Foo,
                    bar: Bar,
                    baz: Baz
                    ->
                    foo + bar
                }
            """.trimIndent()
        functionLiteralRuleAssertThat(code)
            .setMaxLineLength()
            .hasLintViolations(
                LintViolation(4, 19, "Newline expected before parameter"),
                LintViolation(4, 29, "Newline expected before parameter"),
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a single line function literal not exceeding the max line length and having a parameter list`() {
        val code =
            """
            // $MAX_LINE_LENGTH_MARKER                     $EOL_CHAR
            val foobar = { foo: Foo, bar: Bar -> foo + bar }
            """.trimIndent()
        functionLiteralRuleAssertThat(code)
            .setMaxLineLength()
            .hasNoLintViolations()
    }

    @Test
    fun `Given a single line function literal exceeding the max line length and having a parameter list`() {
        val code =
            """
            // $MAX_LINE_LENGTH_MARKER                    $EOL_CHAR
            val foobar = { foo: Foo, bar: Bar -> foo + bar }
            """.trimIndent()
        val formattedCode =
            """
            // $MAX_LINE_LENGTH_MARKER                    $EOL_CHAR
            val foobar = { foo: Foo, bar: Bar ->
                foo + bar
            }
            """.trimIndent()
        functionLiteralRuleAssertThat(code)
            .addAdditionalRuleProvider { MultilineExpressionWrappingRule() }
            .setMaxLineLength()
            .hasLintViolations(
                LintViolation(2, 36, "Newline expected after arrow"),
                LintViolation(2, 48, "Newline expected before closing brace"),
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a multiline code block starting on same line a arrow but it can not fit entirely on that line`() {
        val code =
            """
            // $MAX_LINE_LENGTH_MARKER               $EOL_CHAR
            val foobar = { foo: Foo, bar: Bar -> foo +
                bar
            }
            """.trimIndent()
        val formattedCode =
            """
            // $MAX_LINE_LENGTH_MARKER               $EOL_CHAR
            val foobar = { foo: Foo, bar: Bar ->
                foo +
                bar
            }
            """.trimIndent()
        functionLiteralRuleAssertThat(code)
            .addAdditionalRuleProvider { MultilineExpressionWrappingRule() }
            .setMaxLineLength()
            .hasLintViolation(2, 36, "Newline expected after arrow")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a function literal with a single, but multiline, parameter`() {
        val code =
            """
            val foo = {
                    bar:
                        @Baz("baz")
                        Bar
                -> bar()
            }
            """.trimIndent()
        val formattedCode =
            """
            val foo = {
                    bar:
                        @Baz("baz")
                        Bar
                ->
                bar()
            }
            """.trimIndent()
        functionLiteralRuleAssertThat(code)
            .hasLintViolation(5, 6, "Newline expected after arrow")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a single line function literal without parameters that exceeds the maximum line length`() {
        val code =
            """
            // $MAX_LINE_LENGTH_MARKER    $EOL_CHAR
            val foobar = { it.foo().bar().foobar() }
            val foo = bar.filter { it > 2 }!!.takeIf { it.count() > 100 }.map { it * it }
                ?.sum()!!
            """.trimIndent()
        val formattedCode =
            """
            // $MAX_LINE_LENGTH_MARKER    $EOL_CHAR
            val foobar = {
                it
                    .foo()
                    .bar()
                    .foobar()
            }
            val foo =
                bar
                    .filter { it > 2 }!!
                    .takeIf {
                        it.count() > 100
                    }.map { it * it }
                    ?.sum()!!
            """.trimIndent()
        functionLiteralRuleAssertThat(code)
            .addAdditionalRuleProvider { MultilineExpressionWrappingRule() }
            .addAdditionalRuleProvider { ChainMethodContinuationRule() }
            .addAdditionalRuleProvider { ArgumentListWrappingRule() }
            .addAdditionalRuleProvider { DiscouragedCommentLocationRule() }
            .addAdditionalRuleProvider { IndentationRule() }
            .setMaxLineLength()
            .hasLintViolations(
                LintViolation(2, 14, "Newline expected after opening brace"),
                LintViolation(2, 40, "Newline expected before closing brace"),
                LintViolation(3, 22, "Newline expected after opening brace"),
                LintViolation(3, 31, "Newline expected before closing brace"),
                LintViolation(3, 42, "Newline expected after opening brace"),
                LintViolation(3, 61, "Newline expected before closing brace"),
                LintViolation(3, 67, "Newline expected after opening brace"),
                LintViolation(3, 77, "Newline expected before closing brace"),
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a multiline line function literal with single statement on same line as lbrace not separated with whitespace`() {
        val code =
            """
            val foo = {doSomething()
            }
            """.trimIndent()
        val formattedCode =
            """
            val foo = { doSomething() }
            """.trimIndent()
        functionLiteralRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(1, 11, "Expected single space after opening curly brace"),
                LintViolation(1, 25, "Unexpected newline as function literal fits on single line"),
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a multiline line function literal with single statement on same line as rbrace not separated with whitespace`() {
        val code =
            """
            val foo = {
                doSomething()}
            """.trimIndent()
        val formattedCode =
            """
            val foo = { doSomething() }
            """.trimIndent()
        functionLiteralRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(1, 12, "Unexpected newline as function literal fits on single line"),
                LintViolation(2, 18, "Expected single space before closing curly brace"),
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `xx`() {
        val code =
            """
            fun foo() {
                node
                    .findChildByType(CONDITION)
                    ?.let { fromAstNode ->
                        startIndentContext(fromAstNode)
                    }
            }
            """.trimIndent()
        val formattedCode =
            """
            fun foo() {
                node
                    .findChildByType(CONDITION)
                    ?.let { fromAstNode -> startIndentContext(fromAstNode) }
            }
            """.trimIndent()
        functionLiteralRuleAssertThat(code)
//            .addAdditionalRuleProvider { WrappingRule() }
            .addAdditionalRuleProvider { StatementWrappingRule() }
            .addAdditionalRuleProvider { IndentationRule() }
            .isFormattedAs(formattedCode)
    }
}
