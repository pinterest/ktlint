package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.test.KtLintAssertThat
import com.pinterest.ktlint.test.KtLintAssertThat.Companion.EOL_CHAR
import com.pinterest.ktlint.test.KtLintAssertThat.Companion.MAX_LINE_LENGTH_MARKER
import com.pinterest.ktlint.test.LintViolation
import org.junit.jupiter.api.Test

class BinaryExpressionWrappingRuleTest {
    private val binaryExpressionWrappingRuleAssertThat = KtLintAssertThat.assertThatRule { BinaryExpressionWrappingRule() }

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
                LintViolation(2, 36, "Line is exceeding max line length. Break line after operator of binary expression"),
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
            .hasLintViolation(3, 30, "Line is exceeding max line length. Break line after operator of binary expression")
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
            .hasLintViolation(3, 37, "Line is exceeding max line length. Break line after operator of binary expression")
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
                LintViolation(2, 38, "Line is exceeding max line length. Break line after operator of binary expression"),
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
            .hasLintViolation(3, 30, "Line is exceeding max line length. Break line after operator of binary expression")
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
            .hasLintViolation(3, 34, "Line is exceeding max line length. Break line after operator of binary expression")
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
                // are evaluated and only when needed wrapped again at the operation reference.
                LintViolation(3, 35, "Line is exceeding max line length. Break line after operator of binary expression"),
                LintViolation(3, 63, "Line is exceeding max line length. Break line after operator of binary expression"),
                LintViolation(3, 92, "Line is exceeding max line length", canBeAutoCorrected = false),
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
            .hasLintViolationWithoutAutoCorrect(3, 36, "Line is exceeding max line length")
    }
}
