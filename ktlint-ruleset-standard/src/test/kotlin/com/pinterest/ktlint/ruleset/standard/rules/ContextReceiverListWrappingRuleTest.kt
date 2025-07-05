package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.EOL_CHAR
import com.pinterest.ktlint.test.KtLintAssertThat.Companion.MAX_LINE_LENGTH_MARKER
import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRuleBuilder
import com.pinterest.ktlint.test.LintViolation
import org.junit.jupiter.api.Test

class ContextReceiverListWrappingRuleTest {
    private val contextReceiverListWrappingRuleAssertThat =
        assertThatRuleBuilder { ContextReceiverListWrappingRule() }
            .addAdditionalRuleProvider { MaxLineLengthRule() }
            .assertThat()

    @Test
    fun `Given a function without modifiers and with a context parameter on the same line as the fun keyword then wrap the fun keyword to a newline`() {
        val code =
            """
            context(_: Foo) fun fooBar()
            """.trimIndent()
        val formattedCode =
            """
            context(_: Foo)
            fun fooBar()
            """.trimIndent()
        contextReceiverListWrappingRuleAssertThat(code)
            .hasLintViolation(1, 17, "Expected a newline after the context parameter")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a function without modifiers and with a context parameter followed by a block comment and the fun keyword on the same line then wrap the fun keyword to a newline`() {
        val code =
            """
            context(_: Foo) /* some comment */ fun fooBar1()
            context(_: Foo) /** some comment */ fun fooBar2()
            """.trimIndent()
        val formattedCode =
            """
            context(_: Foo) /* some comment */
            fun fooBar1()
            context(_: Foo) /** some comment */
            fun fooBar2()
            """.trimIndent()
        contextReceiverListWrappingRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(1, 36, "Expected a newline after the context parameter"),
                LintViolation(2, 37, "Expected a newline after the context parameter"),
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a function with a context parameter followed by an EOL comment on the same line then do not reformat`() {
        val code =
            """
            context(_: Foo) // some comment
            fun fooBar2()
            """.trimIndent()
        contextReceiverListWrappingRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Given a function with a context parameter on the same line as the first modifier (keyword) then wrap the first modifier to a newline`() {
        val code =
            """
            context(_: Foo) public fun fooBar()
            """.trimIndent()
        val formattedCode =
            """
            context(_: Foo)
            public fun fooBar()
            """.trimIndent()
        contextReceiverListWrappingRuleAssertThat(code)
            .hasLintViolation(1, 17, "Expected a newline after the context parameter")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a function with a context parameter on the same line as the first modifier (annotation) the wrap the first modifier to a newline`() {
        val code =
            """
            context(_: Foo) @Bar fun fooBar()
            """.trimIndent()
        val formattedCode =
            """
            context(_: Foo)
            @Bar fun fooBar()
            """.trimIndent()
        contextReceiverListWrappingRuleAssertThat(code)
            .hasLintViolation(1, 17, "Expected a newline after the context parameter")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a function with a context parameter with a single argument and the context receiver line exceeds the maximum line length, then wrap the context receiver entry to a separate line`() {
        val code =
            """
            // $MAX_LINE_LENGTH_MARKER     $EOL_CHAR
            context(_: Foooooooooooooooooooo)
            fun fooBar()

            class Bar {
                context(_: Foooooooooooooooo)
                fun fooBar()
            }
            """.trimIndent()
        val formattedCode =
            """
            // $MAX_LINE_LENGTH_MARKER     $EOL_CHAR
            context(
                _: Foooooooooooooooooooo
            )
            fun fooBar()

            class Bar {
                context(
                    _: Foooooooooooooooo
                )
                fun fooBar()
            }
            """.trimIndent()
        contextReceiverListWrappingRuleAssertThat(code)
            .setMaxLineLength()
            .hasLintViolations(
                LintViolation(2, 9, "Newline expected before context parameter as max line length is violated"),
                LintViolation(2, 33, "Newline expected before closing parenthesis as max line length is violated"),
                LintViolation(6, 13, "Newline expected before context parameter as max line length is violated"),
                LintViolation(6, 33, "Newline expected before closing parenthesis as max line length is violated"),
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a function with a context parameter with multiple arguments and the context parameter line exceeds the maximum line length, then wrap each context parameter entry to a separate line`() {
        val code =
            """
            // $MAX_LINE_LENGTH_MARKER     $EOL_CHAR
            context(_: Fooooooooooooooooooo1, _: Foooooooooooooooooooooooooooooo2)
            fun fooBar()

            class Bar {
                context(_: Fooooooooooooooo1, _: Foooooooooooooooooooooooooo2)
                fun fooBar()
            }
            """.trimIndent()
        val formattedCode =
            """
            // $MAX_LINE_LENGTH_MARKER     $EOL_CHAR
            context(
                _: Fooooooooooooooooooo1,
                _: Foooooooooooooooooooooooooooooo2
            )
            fun fooBar()

            class Bar {
                context(
                    _: Fooooooooooooooo1,
                    _: Foooooooooooooooooooooooooo2
                )
                fun fooBar()
            }
            """.trimIndent()
        contextReceiverListWrappingRuleAssertThat(code)
            .setMaxLineLength()
            .hasLintViolations(
                LintViolation(2, 9, "Newline expected before context parameter as max line length is violated"),
                LintViolation(2, 35, "Newline expected before context parameter as max line length is violated"),
                LintViolation(2, 70, "Newline expected before closing parenthesis as max line length is violated"),
                LintViolation(6, 13, "Newline expected before context parameter as max line length is violated"),
                LintViolation(6, 35, "Newline expected before context parameter as max line length is violated"),
                LintViolation(6, 66, "Newline expected before closing parenthesis as max line length is violated"),
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a function with a context parameter having a generic type and the context receiver line (including it projection types) is exceeding the max line length then wrap the type projections to separate lines`() {
        val code =
            """
            // $MAX_LINE_LENGTH_MARKER  $EOL_CHAR
            context(_: Foooooooooooooooo<Foo, Bar>)
            fun fooBar()
            """.trimIndent()
        // Actually, the closing ">" should be de-indented in same way as is done with ")", "]" and "}". It is
        // however indented to keep it in sync with other TYPE_ARGUMENT_LISTs which are formatted in this way.
        val formattedCode =
            """
            // $MAX_LINE_LENGTH_MARKER  $EOL_CHAR
            context(
                _: Foooooooooooooooo<
                    Foo,
                    Bar
                    >
            )
            fun fooBar()
            """.trimIndent()
        contextReceiverListWrappingRuleAssertThat(code)
            .setMaxLineLength()
            .hasLintViolations(
                LintViolation(2, 9, "Newline expected before context parameter as max line length is violated"),
                LintViolation(2, 39, "Newline expected before closing parenthesis as max line length is violated"),
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Issue 2854 - Given a function parameter with a context parameter then do not wrap after the context receiver`() {
        val code =
            """
            fun bar1(foo: context(_: Foo) () -> Unit = { foobar() }) {}
            fun bar2(
                foo: context(_: Foo) () -> Unit = { foobar() }
            ) {}
            """.trimIndent()
        contextReceiverListWrappingRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Issue 3028 - Given some functions with context receiver list on the same line as the fun keyword then only wrap context parameter to a newline`() {
        val code =
            """
            context(Foo) fun foo()
            context(_: Foo) fun foo()

            context(Foooooooooooooooo<Foo, Bar>) fun fooBar()
            context(_: Foooooooooooooooo<Foo, Bar>) fun fooBar()
            """.trimIndent()
        contextReceiverListWrappingRuleAssertThat(code)
            // Find violations on the context parameter, but skip the context receivers
            .hasLintViolations(
                LintViolation(2, 17, "Expected a newline after the context parameter"),
                LintViolation(5, 41, "Expected a newline after the context parameter"),
            )
    }
}
