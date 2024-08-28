package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.ruleset.standard.StandardRuleSetProvider
import com.pinterest.ktlint.test.KtLintAssertThat.Companion.EOL_CHAR
import com.pinterest.ktlint.test.KtLintAssertThat.Companion.MAX_LINE_LENGTH_MARKER
import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRuleBuilder
import com.pinterest.ktlint.test.LintViolation
import com.pinterest.ktlint.test.MULTILINE_STRING_QUOTE
import org.junit.jupiter.api.Test

class BinaryExpressionWrappingRuleTest {
    private val binaryExpressionWrappingRuleAssertThat =
        assertThatRuleBuilder { BinaryExpressionWrappingRule() }
            .addAdditionalRuleProvider { MaxLineLengthRule() }
            .assertThat()

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
                LintViolation(2, 36, "Line is exceeding max line length. Break line after '&&' in binary expression"),
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
            .hasLintViolation(3, 30, "Line is exceeding max line length. Break line after '&&' in binary expression")
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
            .hasLintViolation(3, 37, "Line is exceeding max line length. Break line after '&&' in binary expression")
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
                LintViolation(2, 38, "Line is exceeding max line length. Break line after '&&' in binary expression"),
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
            .hasLintViolation(3, 30, "Line is exceeding max line length. Break line after '&&' in binary expression")
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
            .addAdditionalRuleProvider { ConditionWrappingRule() }
            .hasLintViolation(3, 34, "Line is exceeding max line length. Break line after '&&' in binary expression")
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
                LintViolation(3, 63, "Line is exceeding max line length. Break line after '||' in binary expression"),
                LintViolation(3, 94, "Line is exceeding max line length. Break line after '&&' in binary expression"),
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
            .hasNoLintViolationsExceptInAdditionalRules()
    }

    @Test
    fun `Given a binary expression containing an elvis operator`() {
        val code =
            """
            // $MAX_LINE_LENGTH_MARKER        $EOL_CHAR
            val foo1 = foo() ?: "foooooooooooooooooo" +
                    "bar"
            // Do not remove blank line below, it is relevant as both the newline of the blank line and the indent before property foo2 have to be accounted for

            val foo2 = foo() ?: "foooooooooooooooooo" +
                    "bar"
            """.trimIndent()
        val formattedCode =
            """
            // $MAX_LINE_LENGTH_MARKER        $EOL_CHAR
            val foo1 =
                foo()
                    ?: "foooooooooooooooooo" +
                    "bar"
            // Do not remove blank line below, it is relevant as both the newline of the blank line and the indent before property foo2 have to be accounted for

            val foo2 =
                foo()
                    ?: "foooooooooooooooooo" +
                    "bar"
            """.trimIndent()
        binaryExpressionWrappingRuleAssertThat(code)
            .setMaxLineLength()
            .hasLintViolations(
                LintViolation(2, 12, "Line is exceeding max line length. Break line between assignment and expression"),
                LintViolation(2, 18, "Line is exceeding max line length. Break line before '?:'"),
                LintViolation(6, 12, "Line is exceeding max line length. Break line between assignment and expression"),
                LintViolation(6, 18, "Line is exceeding max line length. Break line before '?:'"),
            ).isFormattedAs(formattedCode)
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
    fun `Given a value argument containing a binary expression which causes the line to exceeds the maximum line length`() {
        val code =
            """
            // $MAX_LINE_LENGTH_MARKER    $EOL_CHAR
            val foobar1 = Foo(1 * 2 * 3 * 4)
            val foobar2 = Foo(bar(1 * 2 * 3))
            val foobar3 = Foo(bar("bar" + "bazzzzzzzzzzz"))
            val foobar4 = Foo(bar("bar" + "bazzzzzzzzzzzz"))
            """.trimIndent()
        val formattedCode =
            """
            // $MAX_LINE_LENGTH_MARKER    $EOL_CHAR
            val foobar1 = Foo(
                1 * 2 * 3 * 4
            )
            val foobar2 = Foo(
                bar(1 * 2 * 3)
            )
            val foobar3 = Foo(
                bar(
                    "bar" + "bazzzzzzzzzzz"
                )
            )
            val foobar4 = Foo(
                bar(
                    "bar" +
                        "bazzzzzzzzzzzz"
                )
            )
            """.trimIndent()
        binaryExpressionWrappingRuleAssertThat(code)
            .addAdditionalRuleProvider { ArgumentListWrappingRule() }
            .addAdditionalRuleProvider { IndentationRule() }
            .addAdditionalRuleProvider { WrappingRule() }
            .addRequiredRuleProviderDependenciesFrom(StandardRuleSetProvider())
            .setMaxLineLength()
            .hasLintViolations(
                // Although violations below are reported by the Linter, they will not be enforced by the formatter. After the
                // ArgumentListWrapping rule has wrapped the argument, there is no more need to wrap the expression as well.
                LintViolation(4, 23, "Line is exceeding max line length. Break line before expression"),
                LintViolation(4, 30, "Line is exceeding max line length. Break line after '+' in binary expression"),
                LintViolation(5, 23, "Line is exceeding max line length. Break line before expression"),
                LintViolation(5, 30, "Line is exceeding max line length. Break line after '+' in binary expression"),
            ).hasLintViolationsForAdditionalRules(
                LintViolation(2, 19, "Argument should be on a separate line (unless all arguments can fit a single line)"),
                LintViolation(2, 32, "Missing newline before \")\""),
                LintViolation(2, 32, "Exceeded max line length (31)", canBeAutoCorrected = false),
                LintViolation(3, 19, "Argument should be on a separate line (unless all arguments can fit a single line)"),
                LintViolation(3, 23, "Argument should be on a separate line (unless all arguments can fit a single line)"),
                LintViolation(3, 32, "Missing newline before \")\""),
                LintViolation(3, 32, "Exceeded max line length (31)", canBeAutoCorrected = false),
                LintViolation(3, 33, "Missing newline before \")\""),
                LintViolation(4, 19, "Argument should be on a separate line (unless all arguments can fit a single line)"),
                LintViolation(4, 23, "Argument should be on a separate line (unless all arguments can fit a single line)"),
                LintViolation(4, 32, "Exceeded max line length (31)", canBeAutoCorrected = false),
                LintViolation(4, 46, "Missing newline before \")\""),
                LintViolation(4, 47, "Missing newline before \")\""),
                LintViolation(5, 19, "Argument should be on a separate line (unless all arguments can fit a single line)"),
                LintViolation(5, 23, "Argument should be on a separate line (unless all arguments can fit a single line)"),
                LintViolation(5, 32, "Exceeded max line length (31)", canBeAutoCorrected = false),
                LintViolation(5, 47, "Missing newline before \")\""),
                LintViolation(5, 48, "Missing newline before \")\""),
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
    fun `Given a call expression containing an binary expression value argument, followed by a lambda on the same line, then wrap the lambda`() {
        val code =
            """
            // $MAX_LINE_LENGTH_MARKER       $EOL_CHAR
            fun foo() {
                require(bar != "barrrrrrrr") { "some longgggggggggggggggggg message" }
            }
            """.trimIndent()
        val formattedCode =
            """
            // $MAX_LINE_LENGTH_MARKER       $EOL_CHAR
            fun foo() {
                require(bar != "barrrrrrrr") {
                    "some longgggggggggggggggggg message"
                }
            }
            """.trimIndent()
        binaryExpressionWrappingRuleAssertThat(code)
            .setMaxLineLength()
            .addAdditionalRuleProvider { ArgumentListWrappingRule() }
            .addAdditionalRuleProvider { WrappingRule() }
            .addRequiredRuleProviderDependenciesFrom(StandardRuleSetProvider())
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given an elvis expression exceeding the line length`() {
        val code =
            """
            // $MAX_LINE_LENGTH_MARKER                            $EOL_CHAR
            val foo = foobar ?: throw UnsupportedOperationException("foobar")
            """.trimIndent()
        val formattedCode =
            """
            // $MAX_LINE_LENGTH_MARKER                            $EOL_CHAR
            val foo =
                foobar
                    ?: throw UnsupportedOperationException(
                        "foobar"
                    )
            """.trimIndent()
        binaryExpressionWrappingRuleAssertThat(code)
            .setMaxLineLength()
            .addAdditionalRuleProvider { ArgumentListWrappingRule() }
            .addAdditionalRuleProvider { MaxLineLengthRule() }
            .hasLintViolations(
                LintViolation(2, 11, "Line is exceeding max line length. Break line between assignment and expression", true),
                LintViolation(2, 18, "Line is exceeding max line length. Break line before '?:'", true),
            ).isFormattedAs(formattedCode)
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
            .addAdditionalRuleProvider { MaxLineLengthRule() }
            .hasLintViolationForAdditionalRule(4, 56, "Exceeded max line length (55)", false)
            .hasNoLintViolationsExceptInAdditionalRules()
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
        binaryExpressionWrappingRuleAssertThat(code)
            .setMaxLineLength()
            .addAdditionalRuleProvider { ArgumentListWrappingRule() }
            .addAdditionalRuleProvider { WrappingRule() }
            // Lint violations from ArgumentListWrappingRule and WrappingRule are reported during linting only. When formatting, the
            // wrapping of the braces of the function literal by the BinaryExpressionWrapping prevents those violations from occurring.
            .hasLintViolationsForAdditionalRule(
                LintViolation(3, 12, "Missing newline after \"{\""),
                LintViolation(3, 21, "Argument should be on a separate line (unless all arguments can fit a single line)"),
                LintViolation(3, 45, "Exceeded max line length (44)", canBeAutoCorrected = false),
                LintViolation(3, 48, "Missing newline before \")\""),
            ).hasLintViolations(
                LintViolation(3, 12, "Newline expected after '{'"),
                LintViolation(3, 50, "Newline expected before '}'"),
                // Lint violation below only occurs during linting. Resolving violations above, prevents the next violation from occurring
                LintViolation(3, 59, "Line is exceeding max line length. Break line after 'returns' in binary expression"),
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Issue 2492 - Given a binary expression as value argument on a separate line`() {
        val code =
            """
            // $MAX_LINE_LENGTH_MARKER  $EOL_CHAR
            val foo =
                foo(
                    "longgggggggggggggg" +
                        "foo",
                )
            """.trimIndent()
        binaryExpressionWrappingRuleAssertThat(code)
            .setMaxLineLength()
            .hasNoLintViolationsExceptInAdditionalRules()
    }

    @Test
    fun `Issue 2450 - Given a binary expression followed by an EOL comment that causes the max line length to be exceeded, then only report a violation via max-line-length rule`() {
        val code =
            """
            // $MAX_LINE_LENGTH_MARKER                                  $EOL_CHAR
            val bar = leftHandSideExpression && rightHandSideExpression // Some comment
            val foo =
                foobar(
                    "foooooooooooooooooooooooooooooooooooooooooooooo" + // Some comment
                    "bar"
                )
            """.trimIndent()
        binaryExpressionWrappingRuleAssertThat(code)
            .setMaxLineLength()
            .addAdditionalRuleProvider { MaxLineLengthRule() }
            .hasLintViolationsForAdditionalRule(
                LintViolation(2, 62, "Exceeded max line length (61)", false),
                LintViolation(5, 62, "Exceeded max line length (61)", false),
            ).hasNoLintViolationsExceptInAdditionalRules()
    }

    @Test
    fun `Issue 2450 - Given a binary expression including an EOL-comment that causes the max line length to be exceeded then ignore the EOL-comment`() {
        val code =
            """
            // $MAX_LINE_LENGTH_MARKER                               $EOL_CHAR
            val foo1 = foo() ?: "foooooooooooooooooo" + // some comment
                    "bar"
            """.trimIndent()
        binaryExpressionWrappingRuleAssertThat(code)
            .setMaxLineLength()
            .hasNoLintViolationsExceptInAdditionalRules()
    }
}
