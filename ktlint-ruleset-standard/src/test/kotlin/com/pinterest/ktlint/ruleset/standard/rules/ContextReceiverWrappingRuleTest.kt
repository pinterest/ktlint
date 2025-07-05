package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.EOL_CHAR
import com.pinterest.ktlint.test.KtLintAssertThat.Companion.MAX_LINE_LENGTH_MARKER
import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRuleBuilder
import com.pinterest.ktlint.test.LintViolation
import org.junit.jupiter.api.Test

class ContextReceiverWrappingRuleTest {
    private val contextReceiverWrappingRuleAssertThat =
        assertThatRuleBuilder { ContextReceiverWrappingRule() }
            .addAdditionalRuleProvider { MaxLineLengthRule() }
            .assertThat()

    @Test
    fun `Given a function without modifiers and with a context receiver on the same line as the fun keyword then wrap the fun keyword to a newline`() {
        val code =
            """
            context(Foo) fun fooBar()
            """.trimIndent()
        val formattedCode =
            """
            context(Foo)
            fun fooBar()
            """.trimIndent()
        contextReceiverWrappingRuleAssertThat(code)
            .hasLintViolation(1, 14, "Expected a newline after the context receiver")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a function without modifiers and with a context receiver followed by a block comment and the fun keyword on the same line then wrap the fun keyword to a newline`() {
        val code =
            """
            context(Foo) /* some comment */ fun fooBar1()
            context(Foo) /** some comment */ fun fooBar2()
            """.trimIndent()
        val formattedCode =
            """
            context(Foo) /* some comment */
            fun fooBar1()
            context(Foo) /** some comment */
            fun fooBar2()
            """.trimIndent()
        contextReceiverWrappingRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(1, 33, "Expected a newline after the context receiver"),
                LintViolation(2, 34, "Expected a newline after the context receiver"),
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a function with a context receiver followed by an EOL comment on the same line then do not reformat`() {
        val code =
            """
            context(Foo) // some comment
            fun fooBar2()
            """.trimIndent()
        contextReceiverWrappingRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Given a function with a context receiver on the same line as the first modifier (keyword) the wrap the first modifier to a newline`() {
        val code =
            """
            context(Foo) public fun fooBar()
            """.trimIndent()
        val formattedCode =
            """
            context(Foo)
            public fun fooBar()
            """.trimIndent()
        contextReceiverWrappingRuleAssertThat(code)
            .hasLintViolation(1, 14, "Expected a newline after the context receiver")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a function with a context receiver on the same line as the first modifier (annotation) the wrap the first modifier to a newline`() {
        val code =
            """
            context(Foo) @Bar fun fooBar()
            """.trimIndent()
        val formattedCode =
            """
            context(Foo)
            @Bar fun fooBar()
            """.trimIndent()
        contextReceiverWrappingRuleAssertThat(code)
            .hasLintViolation(1, 14, "Expected a newline after the context receiver")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a function with a context receiver with a single argument and the context receiver line exceeds the maximum line length, then wrap the context receiver entry to a separate line`() {
        val code =
            """
            // $MAX_LINE_LENGTH_MARKER  $EOL_CHAR
            context(Foooooooooooooooooooo)
            fun fooBar()

            class Bar {
                context(Foooooooooooooooo)
                fun fooBar()
            }
            """.trimIndent()
        val formattedCode =
            """
            // $MAX_LINE_LENGTH_MARKER  $EOL_CHAR
            context(
                Foooooooooooooooooooo
            )
            fun fooBar()

            class Bar {
                context(
                    Foooooooooooooooo
                )
                fun fooBar()
            }
            """.trimIndent()
        contextReceiverWrappingRuleAssertThat(code)
            .setMaxLineLength()
            .hasLintViolations(
                LintViolation(2, 9, "Newline expected before context receiver as max line length is violated"),
                LintViolation(2, 30, "Newline expected before closing parenthesis as max line length is violated"),
                LintViolation(6, 13, "Newline expected before context receiver as max line length is violated"),
                LintViolation(6, 30, "Newline expected before closing parenthesis as max line length is violated"),
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a function with a context receiver with multiple arguments and the context receiver line exceeds the maximum line length, then wrap each context receiver entry to a separate line`() {
        val code =
            """
            // $MAX_LINE_LENGTH_MARKER  $EOL_CHAR
            context(Fooooooooooooooooooo1, Foooooooooooooooooooooooooooooo2)
            fun fooBar()

            class Bar {
                context(Fooooooooooooooo1, Foooooooooooooooooooooooooo2)
                fun fooBar()
            }
            """.trimIndent()
        val formattedCode =
            """
            // $MAX_LINE_LENGTH_MARKER  $EOL_CHAR
            context(
                Fooooooooooooooooooo1,
                Foooooooooooooooooooooooooooooo2
            )
            fun fooBar()

            class Bar {
                context(
                    Fooooooooooooooo1,
                    Foooooooooooooooooooooooooo2
                )
                fun fooBar()
            }
            """.trimIndent()
        contextReceiverWrappingRuleAssertThat(code)
            .setMaxLineLength()
            .hasLintViolations(
                LintViolation(2, 9, "Newline expected before context receiver as max line length is violated"),
                LintViolation(2, 32, "Newline expected before context receiver as max line length is violated"),
                LintViolation(2, 64, "Newline expected before closing parenthesis as max line length is violated"),
                LintViolation(6, 13, "Newline expected before context receiver as max line length is violated"),
                LintViolation(6, 32, "Newline expected before context receiver as max line length is violated"),
                LintViolation(6, 60, "Newline expected before closing parenthesis as max line length is violated"),
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a function with a context receiver having a generic type and the context receiver line (including it projection types) is exceeding the max line length then wrap the type projections to separate lines`() {
        val code =
            """
            // $MAX_LINE_LENGTH_MARKER  $EOL_CHAR
            context(Foooooooooooooooo<Foo, Bar>)
            fun fooBar()
            """.trimIndent()
        // Actually, the closing ">" should be de-indented in same way as is done with ")", "]" and "}". It is
        // however indented to keep it in sync with other TYPE_ARGUMENT_LISTs which are formatted in this way.
        val formattedCode =
            """
            // $MAX_LINE_LENGTH_MARKER  $EOL_CHAR
            context(
                Foooooooooooooooo<
                    Foo,
                    Bar
                    >
            )
            fun fooBar()
            """.trimIndent()
        contextReceiverWrappingRuleAssertThat(code)
            .setMaxLineLength()
            .hasLintViolations(
                LintViolation(2, 9, "Newline expected before context receiver as max line length is violated"),
                LintViolation(2, 36, "Newline expected before closing parenthesis as max line length is violated"),
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Issue 2854 - Given a function parameter with a context receiver then do not wrap after the context receiver`() {
        val code =
            """
            fun bar1(foo: context(Foo) () -> Unit = { foobar() }) {}
            fun bar2(
                foo: context(Foo) () -> Unit = { foobar() }
            ) {}
            """.trimIndent()
        contextReceiverWrappingRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Issue 3028 - Given some functions with context receiver list on the same line as the fun keyword then only wrap context receivers to a newline`() {
        val code =
            """
            context(Foo) fun foo()
            context(_: Foo) fun foo()

            context(Foooooooooooooooo<Foo, Bar>) fun fooBar()
            context(_: Foooooooooooooooo<Foo, Bar>) fun fooBar()
            """.trimIndent()
        contextReceiverWrappingRuleAssertThat(code)
            // Find violations on the context receivers, but skip the context parameters
            .hasLintViolations(
                LintViolation(1, 14, "Expected a newline after the context receiver"),
                LintViolation(4, 38, "Expected a newline after the context receiver"),
            )
    }
}
