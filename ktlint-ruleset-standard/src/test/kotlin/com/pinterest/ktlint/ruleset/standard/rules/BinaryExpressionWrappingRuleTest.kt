package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.ruleset.standard.StandardRuleSetProvider
import com.pinterest.ktlint.test.KtLintAssertThat.Companion.EOL_CHAR
import com.pinterest.ktlint.test.KtLintAssertThat.Companion.MAX_LINE_LENGTH_MARKER
import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRule
import com.pinterest.ktlint.test.LintViolation
import com.pinterest.ktlint.test.MULTILINE_STRING_QUOTE
import org.junit.jupiter.api.Test

class BinaryExpressionWrappingRuleTest {
    private val binaryExpressionWrappingRuleAssertThat = assertThatRule { BinaryExpressionWrappingRule() }

    @Test
    fun `Given a property with a binary expression on same line as equals, and it exceeds the max line length then wrap before the expression`() {
        val code =
            """
            // $MAX_LINE_LENGTH_MARKER                               $EOL_CHAR
            val bar = leftHandSideExpression && rightHandSideExpression
            """.trimIndent()
        val formattedCode =
            """
            // $MAX_LINE_LENGTH_MARKER                               $EOL_CHAR
            val bar =
                leftHandSideExpression && rightHandSideExpression
            """.trimIndent()
        binaryExpressionWrappingRuleAssertThat(code)
            .setMaxLineLength()
            .hasLintViolations(
                LintViolation(2, 11, "Line is exceeding max line length. Break line between assignment and expression"),
                // Next violation only happens during linting. When formatting the violation does not occur because fix of previous
                // violation prevent that the remainder of the line exceeds the maximum
                LintViolation(2, 36, "Line is exceeding max line length. Break line after operator in binary expression"),
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a property with a binary expression not on same line as equals, and it exceeds the max line length then wrap the expression at the operator`() {
        val code =
            """
            // $MAX_LINE_LENGTH_MARKER                         $EOL_CHAR
            val bar =
                leftHandSideExpression && rightHandSideExpression
            """.trimIndent()
        val formattedCode =
            """
            // $MAX_LINE_LENGTH_MARKER                         $EOL_CHAR
            val bar =
                leftHandSideExpression &&
                    rightHandSideExpression
            """.trimIndent()
        binaryExpressionWrappingRuleAssertThat(code)
            .setMaxLineLength()
            .hasLintViolation(3, 30, "Line is exceeding max line length. Break line after operator in binary expression")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a return with a binary expression, and it exceeds the max line length then wrap the expression at the operator`() {
        val code =
            """
            // $MAX_LINE_LENGTH_MARKER                                $EOL_CHAR
            fun foo() {
                return leftHandSideExpression && rightHandSideExpression
            }
            """.trimIndent()
        val formattedCode =
            """
            // $MAX_LINE_LENGTH_MARKER                                $EOL_CHAR
            fun foo() {
                return leftHandSideExpression &&
                    rightHandSideExpression
            }
            """.trimIndent()
        binaryExpressionWrappingRuleAssertThat(code)
            .setMaxLineLength()
            .hasLintViolation(3, 37, "Line is exceeding max line length. Break line after operator in binary expression")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a function with a binary body expression on same line as function signature, and it exceeds the max line length then wrap the expression at the operator`() {
        val code =
            """
            // $MAX_LINE_LENGTH_MARKER                                 $EOL_CHAR
            fun foo() = leftHandSideExpression && rightHandSideExpression
            """.trimIndent()
        val formattedCode =
            """
            // $MAX_LINE_LENGTH_MARKER                                 $EOL_CHAR
            fun foo() =
                leftHandSideExpression && rightHandSideExpression
            """.trimIndent()
        binaryExpressionWrappingRuleAssertThat(code)
            .setMaxLineLength()
            .hasLintViolations(
                LintViolation(2, 13, "Line is exceeding max line length. Break line between assignment and expression"),
                // Next violation only happens during linting. When formatting the violation does not occur because fix of previous
                // violation prevent that the remainder of the line exceeds the maximum
                LintViolation(2, 38, "Line is exceeding max line length. Break line after operator in binary expression"),
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a function with a binary body expression not on same line as function signature, and it exceeds the max line length then wrap the expression at the operator`() {
        val code =
            """
            // $MAX_LINE_LENGTH_MARKER                         $EOL_CHAR
            fun foo() =
                leftHandSideExpression && rightHandSideExpression
            """.trimIndent()
        val formattedCode =
            """
            // $MAX_LINE_LENGTH_MARKER                         $EOL_CHAR
            fun foo() =
                leftHandSideExpression &&
                    rightHandSideExpression
            """.trimIndent()
        binaryExpressionWrappingRuleAssertThat(code)
            .setMaxLineLength()
            .hasLintViolation(3, 30, "Line is exceeding max line length. Break line after operator in binary expression")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given an if-statement with a binary expression condition, and it exceeds the max line length then wrap the expression at the operator`() {
        val code =
            """
            // $MAX_LINE_LENGTH_MARKER                                $EOL_CHAR
            fun foo() {
                if (leftHandSideExpression && rightHandSideExpression) {
                    // do something
                }
            }
            """.trimIndent()
        val formattedCode =
            """
            // $MAX_LINE_LENGTH_MARKER                                $EOL_CHAR
            fun foo() {
                if (leftHandSideExpression &&
                    rightHandSideExpression) {
                    // do something
                }
            }
            """.trimIndent()
        binaryExpressionWrappingRuleAssertThat(code)
            .setMaxLineLength()
            .hasLintViolation(3, 34, "Line is exceeding max line length. Break line after operator in binary expression")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given an if-statement with a nested binary expression condition, and it exceeds the max line length then wrap the expression at the operator`() {
        val code =
            """
            // $MAX_LINE_LENGTH_MARKER                                   $EOL_CHAR
            fun foo() {
                if ((leftHandSideExpression && rightHandSideExpression) || (leftHandSideLongExpression && rightHandSideLongExpression)) {
                    // do something
                }
            }
            """.trimIndent()
        val formattedCode =
            """
            // $MAX_LINE_LENGTH_MARKER                                   $EOL_CHAR
            fun foo() {
                if ((leftHandSideExpression && rightHandSideExpression) ||
                    (leftHandSideLongExpression &&
                        rightHandSideLongExpression)) {
                    // do something
                }
            }
            """.trimIndent()
        binaryExpressionWrappingRuleAssertThat(code)
            .setMaxLineLength()
            .hasLintViolations(
                // When linting, a violation is reported for each operation reference. While when formatting, the nested binary expression
                // is evaluated (working from outside to inside). After wrapping an outer binary expression, the inner binary expressions
                // are evaluated and only wrapped again at the operation reference when needed.
                LintViolation(3, 1, "Line is exceeding max line length", canBeAutoCorrected = false),
                LintViolation(3, 35, "Line is exceeding max line length. Break line after operator in binary expression"),
                LintViolation(3, 63, "Line is exceeding max line length. Break line after operator in binary expression"),
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a binary expression for which the left hand side including the operation reference is exceeding the max line length`() {
        val code =
            """
            // $MAX_LINE_LENGTH_MARKER        $EOL_CHAR
            fun foo(): String {
                return "some longgggggggg txt" +
                    "more text"
            }
            """.trimIndent()
        binaryExpressionWrappingRuleAssertThat(code)
            .setMaxLineLength()
            .hasLintViolationWithoutAutoCorrect(3, 1, "Line is exceeding max line length")
    }

    @Test
    fun `Given a binary expression for which wrapping of the operator reference would still violates the max-line-length`() {
        val code =
            """
            // $MAX_LINE_LENGTH_MARKER        $EOL_CHAR
            val foo1 = foo() ?: "fooooooooooo" +
                    "bar"
            // Do not remove blank line below, it is relevant as both the newline of the blank line and the indent before property foo2 have to be accounted for

            val foo2 = foo() ?: "foooooooooo" +
                    "bar"
            """.trimIndent()
        binaryExpressionWrappingRuleAssertThat(code)
            .setMaxLineLength()
            .hasLintViolations(
                LintViolation(2, 1, "Line is exceeding max line length", canBeAutoCorrected = false),
                LintViolation(2, 12, "Line is exceeding max line length. Break line between assignment and expression"),
            )
    }

    @Test
    fun `Given a binary expression inside a raw string literal then do not wrap it`() {
        val code =
            """
            // $MAX_LINE_LENGTH_MARKER           $EOL_CHAR
            val foo =
                $MULTILINE_STRING_QUOTE
                foooooooooooooo ${'$'}{bar * bar - 123}
                $MULTILINE_STRING_QUOTE
            """.trimIndent()
        binaryExpressionWrappingRuleAssertThat(code)
            .setMaxLineLength()
            .hasNoLintViolations()
    }

    @Test
    fun `Given a value argument containing a binary expression`() {
        val code =
            """
            // $MAX_LINE_LENGTH_MARKER    $EOL_CHAR
            val foobar = Foo(bar(1 * 2 * 3))
            """.trimIndent()
        val formattedCode =
            """
            // $MAX_LINE_LENGTH_MARKER    $EOL_CHAR
            val foobar = Foo(
                bar(1 * 2 * 3)
            )
            """.trimIndent()
        binaryExpressionWrappingRuleAssertThat(code)
            .addAdditionalRuleProvider { ArgumentListWrappingRule() }
            .addAdditionalRuleProvider { WrappingRule() }
            .addRequiredRuleProviderDependenciesFrom(StandardRuleSetProvider())
            .setMaxLineLength()
            .hasLintViolations(
                // Although violations below are reported by the Linter, they will not be enforced by the formatter. After the
                // ArgumentListWrapping rule has wrapped the argument, there is no more need to wrap the expression as well.
                LintViolation(2, 25, "Line is exceeding max line length. Break line after operator in binary expression"),
                LintViolation(2, 29, "Line is exceeding max line length. Break line after operator in binary expression"),
            ).hasLintViolationsForAdditionalRules(
                LintViolation(2, 18, "Argument should be on a separate line (unless all arguments can fit a single line)"),
                LintViolation(2, 22, "Argument should be on a separate line (unless all arguments can fit a single line)"),
                LintViolation(2, 31, "Missing newline before \")\""),
                LintViolation(2, 32, "Missing newline before \")\""),
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a property assignment with a binary expression for which the left hand side operator is a function call`() {
        val code =
            """
            // $MAX_LINE_LENGTH_MARKER         $EOL_CHAR
            val foo1 = foobar(foo * bar) + "foo"
            val foo2 = foobar("foo", foo * bar) + "foo"
            val foo3 = foobar("fooo", foo * bar) + "foo"
            """.trimIndent()
        val formattedCode =
            """
            // $MAX_LINE_LENGTH_MARKER         $EOL_CHAR
            val foo1 = foobar(foo * bar) + "foo"
            val foo2 =
                foobar("foo", foo * bar) + "foo"
            val foo3 =
                foobar("fooo", foo * bar) +
                    "foo"
            """.trimIndent()
        binaryExpressionWrappingRuleAssertThat(code)
            .setMaxLineLength()
            .addAdditionalRuleProvider { ArgumentListWrappingRule() }
            .addRequiredRuleProviderDependenciesFrom(StandardRuleSetProvider())
            .addAdditionalRuleProvider { WrappingRule() }
            // Although the argument-list-wrapping runs before binary-expression-wrapping, it may not wrap the argument values of a
            // function call in case that call is part of a binary expression. It might be better to break the line at the operation
            // reference instead.
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a call expression followed by lambda argument`() {
        val code =
            """
            // $MAX_LINE_LENGTH_MARKER                                       $EOL_CHAR
            fun foo() {
                require(bar != "bar") { "some longgggggggggggggggggg message" }
            }
            """.trimIndent()
        binaryExpressionWrappingRuleAssertThat(code)
            .setMaxLineLength()
            .addAdditionalRuleProvider { ArgumentListWrappingRule() }
            .addAdditionalRuleProvider { WrappingRule() }
            .addRequiredRuleProviderDependenciesFrom(StandardRuleSetProvider())
            .hasLintViolationWithoutAutoCorrect(3, 1, "Line is exceeding max line length")
    }

    @Test
    fun `Issue 2128 - Given an elvis expression exceeding the line length`() {
        val code =
            """
            // $MAX_LINE_LENGTH_MARKER                            $EOL_CHAR
            val foo =
                bar
                    ?: throw UnsupportedOperationException("foobar")
            """.trimIndent()
        binaryExpressionWrappingRuleAssertThat(code)
            .setMaxLineLength()
            .addAdditionalRuleProvider { ArgumentListWrappingRule() }
            .addAdditionalRuleProvider { WrappingRule() }
            .addAdditionalRuleProvider { MaxLineLengthRule() }
            .addRequiredRuleProviderDependenciesFrom(StandardRuleSetProvider())
            .hasLintViolationWithoutAutoCorrect(4, 1, "Line is exceeding max line length")
    }
}
