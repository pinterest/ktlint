package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.EOL_CHAR
import com.pinterest.ktlint.test.KtLintAssertThat.Companion.MAX_LINE_LENGTH_MARKER
import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRuleBuilder
import com.pinterest.ktlint.test.LintViolation
import org.junit.jupiter.api.Test

internal class ParameterWrappingRuleTest {
    private val parameterWrappingRuleAssertThat =
        assertThatRuleBuilder { ParameterWrappingRule() }
            .addAdditionalRuleProvider { MaxLineLengthRule() }
            .assertThat()

    @Test
    fun `Given that the variable name and the following colon do not fit on the same line as val or var keyword then wrap after the colon`() {
        val code =
            """
            // $MAX_LINE_LENGTH_MARKER      $EOL_CHAR
            class Bar1(
                val foooooooooooooooooTooLong: Foo,
            )

            @Suppress("ktlint:standard:max-line-length")
            class Bar2(
                val foooooooooooooooooTooLong: Foo
            )

            fun bar1(
                foooooooooooooooooooooTooLong: Foo,
            )

            @Suppress("ktlint:standard:max-line-length")
            fun bar2(
                foooooooooooooooooooooTooLong: Foo
            )
            """.trimIndent()
        val formattedCode =
            """
            // $MAX_LINE_LENGTH_MARKER      $EOL_CHAR
            class Bar1(
                val foooooooooooooooooTooLong:
                    Foo,
            )

            @Suppress("ktlint:standard:max-line-length")
            class Bar2(
                val foooooooooooooooooTooLong: Foo
            )

            fun bar1(
                foooooooooooooooooooooTooLong:
                    Foo,
            )

            @Suppress("ktlint:standard:max-line-length")
            fun bar2(
                foooooooooooooooooooooTooLong: Foo
            )
            """.trimIndent()
        parameterWrappingRuleAssertThat(code)
            .setMaxLineLength()
            .addAdditionalRuleProvider { IndentationRule() }
            .hasLintViolations(
                LintViolation(3, 35, "Missing newline after \":\""),
                LintViolation(12, 35, "Missing newline after \":\""),
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given that the type does not fit on the same line as the variable name`() {
        val code =
            """
            // $MAX_LINE_LENGTH_MARKER           $EOL_CHAR
            class Bar(
                val foooooooooooooooooTooLong: Foo,
                val foooooooooooooNotTooLong: Foo,
            )
            fun bar(
                foooooooooooooooooooooTooLong: Foo,
                foooooooooooooooooNotTooLong: Foo,
            )
            """.trimIndent()
        val formattedCode =
            """
            // $MAX_LINE_LENGTH_MARKER           $EOL_CHAR
            class Bar(
                val foooooooooooooooooTooLong:
                    Foo,
                val foooooooooooooNotTooLong: Foo,
            )
            fun bar(
                foooooooooooooooooooooTooLong:
                    Foo,
                foooooooooooooooooNotTooLong: Foo,
            )
            """.trimIndent()
        parameterWrappingRuleAssertThat(code)
            .setMaxLineLength()
            .addAdditionalRuleProvider { IndentationRule() }
            .hasLintViolations(
                LintViolation(3, 35, "Missing newline before \"Foo\""),
                LintViolation(7, 35, "Missing newline before \"Foo\""),
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given that equals sign before the value does not fit on the same line as the type and variable name then wrap after the equals sign`() {
        val code =
            """
            // $MAX_LINE_LENGTH_MARKER            $EOL_CHAR
            class Bar1(
                val foooooooooooooooooTooLong: Foo = Foo(),
                val foooooooooooooNotTooLong: Foo = Foo(),
            )

            @Suppress("ktlint:standard:max-line-length")
            class Bar2(
                val foooooooooooooooooTooLong: Foo = Foo(),
                val foooooooooooooNotTooLong: Foo = Foo(),
            )

            fun bar1(
                foooooooooooooooooooooTooLong: Foo = Foo(),
                foooooooooooooooooNotTooLong: Foo = Foo(),
            )

            @Suppress("ktlint:standard:max-line-length")
            fun bar2(
                foooooooooooooooooooooTooLong: Foo = Foo(),
                foooooooooooooooooNotTooLong: Foo = Foo(),
            )
            """.trimIndent()
        val formattedCode =
            """
            // $MAX_LINE_LENGTH_MARKER            $EOL_CHAR
            class Bar1(
                val foooooooooooooooooTooLong: Foo =
                    Foo(),
                val foooooooooooooNotTooLong: Foo =
                    Foo(),
            )

            @Suppress("ktlint:standard:max-line-length")
            class Bar2(
                val foooooooooooooooooTooLong: Foo = Foo(),
                val foooooooooooooNotTooLong: Foo = Foo(),
            )

            fun bar1(
                foooooooooooooooooooooTooLong: Foo =
                    Foo(),
                foooooooooooooooooNotTooLong: Foo =
                    Foo(),
            )

            @Suppress("ktlint:standard:max-line-length")
            fun bar2(
                foooooooooooooooooooooTooLong: Foo = Foo(),
                foooooooooooooooooNotTooLong: Foo = Foo(),
            )
            """.trimIndent()
        parameterWrappingRuleAssertThat(code)
            .setMaxLineLength()
            .addAdditionalRuleProvider { IndentationRule() }
            .hasLintViolations(
                LintViolation(3, 41, "Missing newline after \"=\""),
                LintViolation(4, 40, "Missing newline before \"Foo()\""),
                LintViolation(14, 41, "Missing newline after \"=\""),
                LintViolation(15, 40, "Missing newline before \"Foo()\""),
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given that the value does not fit on the same line as the type and variable name then wrap after the equals sign`() {
        val code =
            """
            // $MAX_LINE_LENGTH_MARKER                   $EOL_CHAR
            class Bar(
                val foooooooooooooooooTooLong: Foo = Foo(),
                val foooooooooooooNotTooLong: Foo = Foo(),
            )
            fun bar(
                foooooooooooooooooooooTooLong: Foo = Foo(),
                foooooooooooooooooNotTooLong: Foo = Foo(),
            )
            """.trimIndent()
        val formattedCode =
            """
            // $MAX_LINE_LENGTH_MARKER                   $EOL_CHAR
            class Bar(
                val foooooooooooooooooTooLong: Foo =
                    Foo(),
                val foooooooooooooNotTooLong: Foo = Foo(),
            )
            fun bar(
                foooooooooooooooooooooTooLong: Foo =
                    Foo(),
                foooooooooooooooooNotTooLong: Foo = Foo(),
            )
            """.trimIndent()
        parameterWrappingRuleAssertThat(code)
            .setMaxLineLength()
            .addAdditionalRuleProvider { IndentationRule() }
            .hasLintViolations(
                LintViolation(3, 41, "Missing newline before \"Foo()\""),
                LintViolation(7, 41, "Missing newline before \"Foo()\""),
            ).isFormattedAs(formattedCode)
    }
}
