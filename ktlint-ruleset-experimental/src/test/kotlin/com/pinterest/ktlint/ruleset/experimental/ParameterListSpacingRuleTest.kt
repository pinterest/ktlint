package com.pinterest.ktlint.ruleset.experimental

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThat
import com.pinterest.ktlint.test.LintViolation
import org.junit.jupiter.api.Test

class ParameterListSpacingRuleTest {
    private val parameterListSpacingRuleAssertThat = ParameterListSpacingRule().assertThat()

    @Test
    fun `Given a function signature which does not contain redundant spaces then do no reformat`() {
        val code =
            """
            fun foo(a: Any, vararg b: Any) = "some-result"
            """.trimIndent()
        parameterListSpacingRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Given a function signature without parameters but with at least one space between the parenthesis then reformat`() {
        val code =
            """
            fun foo( ) = "some-result"
            """.trimIndent()
        val formattedCode =
            """
            fun foo() = "some-result"
            """.trimIndent()
        parameterListSpacingRuleAssertThat(code)
            .hasLintViolation(1, 9, "Unexpected whitespace")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a function signature without parameters but with at least one newline between the parenthesis then reformat`() {
        val code =
            """
            fun foo(
            ) = "some-result"
            """.trimIndent()
        val formattedCode =
            """
            fun foo() = "some-result"
            """.trimIndent()
        parameterListSpacingRuleAssertThat(code)
            .hasLintViolation(1, 9, "Unexpected whitespace")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a function signature with at least one space before the first parameter then reformat`() {
        val code =
            """
            fun foo( a: Any) = "some-result"
            """.trimIndent()
        val formattedCode =
            """
            fun foo(a: Any) = "some-result"
            """.trimIndent()
        parameterListSpacingRuleAssertThat(code)
            .hasLintViolation(1, 9, "Unexpected whitespace")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a function signature with a newline before the first parameter then do not reformat`() {
        val code =
            """
            fun foo(
                a: Any
            ) = "some-result"
            """.trimIndent()
        parameterListSpacingRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Given a function signature with at least one space after the last parameter then reformat`() {
        val code =
            """
            fun foo(a: Any ) = "some-result"
            """.trimIndent()
        val formattedCode =
            """
            fun foo(a: Any) = "some-result"
            """.trimIndent()
        parameterListSpacingRuleAssertThat(code)
            .hasLintViolation(1, 15, "Unexpected whitespace")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a function signature with a newline after the last parameter then do not reformat`() {
        val code =
            """
            fun foo(
                a: Any
            ) = "some-result"
            """.trimIndent()
        parameterListSpacingRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Given a function signature with at least one space between the parameter name and the colon then reformat`() {
        val code =
            """
            fun foo(a : Any) = "some-result"
            """.trimIndent()
        val formattedCode =
            """
            fun foo(a: Any) = "some-result"
            """.trimIndent()
        parameterListSpacingRuleAssertThat(code)
            .hasLintViolation(1, 10, "Unexpected whitespace")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a function signature with at least one newline between the parameter name and the colon then reformat`() {
        val code =
            """
            fun foo(a
                : Any) = "some-result"
            """.trimIndent()
        val formattedCode =
            """
            fun foo(a: Any) = "some-result"
            """.trimIndent()
        parameterListSpacingRuleAssertThat(code)
            .hasLintViolation(1, 10, "Unexpected whitespace")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a function signature without space between the colon and the parameter type then reformat`() {
        val code =
            """
            fun foo(a:Any) = "some-result"
            """.trimIndent()
        val formattedCode =
            """
            fun foo(a: Any) = "some-result"
            """.trimIndent()
        parameterListSpacingRuleAssertThat(code)
            .hasLintViolation(1, 10, "Whitespace after ':' is missing")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a function signature with too many spaces between the colon and the parameter type then reformat`() {
        val code =
            """
            fun foo(a:${TOO_MANY_SPACES}Any) = "some-result"
            """.trimIndent()
        val formattedCode =
            """
            fun foo(a: Any) = "some-result"
            """.trimIndent()
        parameterListSpacingRuleAssertThat(code)
            .hasLintViolation(1, 11, "Expected a single space")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a function signature with a new line between the colon and the parameter type then reformat`() {
        val code =
            """
            fun foo(a:
            Any) = "some-result"
            """.trimIndent()
        val formattedCode =
            """
            fun foo(a: Any) = "some-result"
            """.trimIndent()
        parameterListSpacingRuleAssertThat(code)
            .hasLintViolation(1, 11, "Expected a single space")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a function signature with too many spaces between the modifier list and parameter name then reformat`() {
        val code =
            """
            fun foo1(vararg${TOO_MANY_SPACES}a: Any) = "some-result"
            inline fun foo2(noinline${TOO_MANY_SPACES}bar: () -> Unit) {
                bar()
            }
            inline fun foo3(crossinline${TOO_MANY_SPACES}bar: () -> Unit) {
                bar()
            }
            """.trimIndent()
        val formattedCode =
            """
            fun foo1(vararg a: Any) = "some-result"
            inline fun foo2(noinline bar: () -> Unit) {
                bar()
            }
            inline fun foo3(crossinline bar: () -> Unit) {
                bar()
            }
            """.trimIndent()
        parameterListSpacingRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(1, 16, "Expected a single space"),
                LintViolation(2, 25, "Expected a single space"),
                LintViolation(5, 28, "Expected a single space")
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a function signature with at least one newline between the modifier list and parameter name then reformat`() {
        val code =
            """
            fun foo1(vararg
            a: Any) = "some-result"
            inline fun foo2(noinline
            bar: () -> Unit) {
                bar()
            }
            inline fun foo3(crossinline
            bar: () -> Unit) {
                bar()
            }
            """.trimIndent()
        val formattedCode =
            """
            fun foo1(vararg a: Any) = "some-result"
            inline fun foo2(noinline bar: () -> Unit) {
                bar()
            }
            inline fun foo3(crossinline bar: () -> Unit) {
                bar()
            }
            """.trimIndent()
        parameterListSpacingRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(1, 16, "Expected a single space"),
                LintViolation(3, 25, "Expected a single space"),
                LintViolation(7, 28, "Expected a single space")
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a function signature with too many spaces between the modifiers in the modifier list then reformat`() {
        val code =
            """
            // The code example below not make sense. Its importance is that modifier list can contain multiple elements
            fun foo(vararg  noinline  crossinline  a: Any) = "some-result"
            """.trimIndent()
        val formattedCode =
            """
            // The code example below not make sense. Its importance is that modifier list can contain multiple elements
            fun foo(vararg noinline crossinline a: Any) = "some-result"
            """.trimIndent()
        parameterListSpacingRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(2, 15, "Expected a single space"),
                LintViolation(2, 25, "Expected a single space"),
                LintViolation(2, 38, "Expected a single space")
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a function signature with newlines between the modifiers in the modifier list then reformat`() {
        val code =
            """
            // The code example below not make sense. Its importance is that modifier list can contain multiple elements
            fun foo(
                vararg
                noinline
                crossinline
                a: Any
            ) = "some-result"
            """.trimIndent()
        val formattedCode =
            """
            // The code example below not make sense. Its importance is that modifier list can contain multiple elements
            fun foo(
                vararg noinline crossinline a: Any
            ) = "some-result"
            """.trimIndent()
        parameterListSpacingRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(3, 11, "Expected a single space"),
                LintViolation(4, 13, "Expected a single space"),
                LintViolation(5, 16, "Expected a single space")
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a function signature with an annotated parameter and annotations separated by newlines then do not reformat`() {
        val code =
            """
            fun foo(
                @Bar1(value = "bar1")
                @Bar2(value = "bar2")
                a: Any
            ) = "some-result"
            """.trimIndent()
        parameterListSpacingRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Given a function signature with multiple parameters and at least one space before the comma separating the parameters then reformat`() {
        val code =
            """
            fun foo(a: Any${TOO_MANY_SPACES}, b: Any) = "some-result"
            """.trimIndent() // ktlint-disable string-template
        val formattedCode =
            """
            fun foo(a: Any, b: Any) = "some-result"
            """.trimIndent()
        parameterListSpacingRuleAssertThat(code)
            .hasLintViolation(1, 15, "Unexpected whitespace")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a function signature with multiple parameters and at least one newline before the comma separating the parameters then reformat`() {
        val code =
            """
            fun foo(a: Any
            , b: Any) = "some-result"
            """.trimIndent() // ktlint-disable string-template
        val formattedCode =
            """
            fun foo(a: Any, b: Any) = "some-result"
            """.trimIndent()
        parameterListSpacingRuleAssertThat(code)
            .hasLintViolation(1, 15, "Unexpected whitespace")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a function signature with multiple parameters and no whitespace after the comma separating the parameters then reformat`() {
        val code =
            """
            fun foo(a: Any,b: Any) = "some-result"
            """.trimIndent()
        val formattedCode =
            """
            fun foo(a: Any, b: Any) = "some-result"
            """.trimIndent()
        parameterListSpacingRuleAssertThat(code)
            .hasLintViolation(1, 15, "Whitespace after ',' is missing")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a function signature with multiple parameters and too many spaces after the comma separating the parameters then reformat`() {
        val code =
            """
            fun foo(a: Any,${TOO_MANY_SPACES}b: Any) = "some-result"
            """.trimIndent() // ktlint-disable string-template
        val formattedCode =
            """
            fun foo(a: Any, b: Any) = "some-result"
            """.trimIndent()
        parameterListSpacingRuleAssertThat(code)
            .hasLintViolation(1, 16, "Expected a single space")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a function signature with multiple parameters and a newline after the comma separating the parameters then do not reformat`() {
        val code =
            """
            fun foo(
                a: Any,
                b: Any
            ) = "some-result"
            """.trimIndent() // ktlint-disable string-template
        parameterListSpacingRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Given a function signature with multiple spacing errors in the parameters then reformat all`() {
        val code =
            """
            fun foo(c :Any, d :   Any,   b :  Any ,  vararg  a :  Any ) = "some-result"
            """.trimIndent()
        val formattedCode =
            """
            fun foo(c: Any, d: Any, b: Any, vararg a: Any) = "some-result"
            """.trimIndent()
        parameterListSpacingRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(1, 10, "Unexpected whitespace"),
                LintViolation(1, 11, "Whitespace after ':' is missing"),
                LintViolation(1, 18, "Unexpected whitespace"),
                LintViolation(1, 20, "Expected a single space"),
                LintViolation(1, 27, "Expected a single space"),
                LintViolation(1, 31, "Unexpected whitespace"),
                LintViolation(1, 33, "Expected a single space"),
                LintViolation(1, 38, "Unexpected whitespace"),
                LintViolation(1, 40, "Expected a single space"),
                LintViolation(1, 48, "Expected a single space"),
                LintViolation(1, 51, "Unexpected whitespace"),
                LintViolation(1, 53, "Expected a single space"),
                LintViolation(1, 58, "Unexpected whitespace")
            ).isFormattedAs(formattedCode)
    }

    private companion object {
        const val TOO_MANY_SPACES = "  "
    }
}
