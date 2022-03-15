package com.pinterest.ktlint.ruleset.experimental

import com.pinterest.ktlint.core.LintError
import com.pinterest.ktlint.test.format
import com.pinterest.ktlint.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ParameterListSpacingRuleTest {
    @Test
    fun `Given a function signature which does not contain redundant spaces then do no reformat`() {
        val code =
            """
            fun foo(a: Any, vararg b: Any) = "some-result"
            """.trimIndent()
        assertThat(ParameterListSpacingRule().lint(code)).isEmpty()
        assertThat(ParameterListSpacingRule().format(code)).isEqualTo(code)
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
        assertThat(ParameterListSpacingRule().lint(code)).containsExactly(
            LintError(1, 9, "parameter-list-spacing", "Unexpected whitespace")
        )
        assertThat(ParameterListSpacingRule().format(code)).isEqualTo(formattedCode)
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
        assertThat(ParameterListSpacingRule().lint(code)).containsExactly(
            LintError(1, 9, "parameter-list-spacing", "Unexpected whitespace")
        )
        assertThat(ParameterListSpacingRule().format(code)).isEqualTo(formattedCode)
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
        assertThat(ParameterListSpacingRule().lint(code)).containsExactly(
            LintError(1, 9, "parameter-list-spacing", "Unexpected whitespace")
        )
        assertThat(ParameterListSpacingRule().format(code)).isEqualTo(formattedCode)
    }

    @Test
    fun `Given a function signature with a newline before the first parameter then do not reformat`() {
        val code =
            """
            fun foo(
                a: Any
            ) = "some-result"
            """.trimIndent()
        assertThat(ParameterListSpacingRule().lint(code)).isEmpty()
        assertThat(ParameterListSpacingRule().format(code)).isEqualTo(code)
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
        assertThat(ParameterListSpacingRule().lint(code)).containsExactly(
            LintError(1, 15, "parameter-list-spacing", "Unexpected whitespace")
        )
        assertThat(ParameterListSpacingRule().format(code)).isEqualTo(formattedCode)
    }

    @Test
    fun `Given a function signature with a newline after the last parameter then do not reformat`() {
        val code =
            """
            fun foo(
                a: Any
            ) = "some-result"
            """.trimIndent()
        assertThat(ParameterListSpacingRule().lint(code)).isEmpty()
        assertThat(ParameterListSpacingRule().format(code)).isEqualTo(code)
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
        assertThat(ParameterListSpacingRule().lint(code)).containsExactly(
            LintError(1, 10, "parameter-list-spacing", "Unexpected whitespace")
        )
        assertThat(ParameterListSpacingRule().format(code)).isEqualTo(formattedCode)
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
        assertThat(ParameterListSpacingRule().lint(code)).containsExactly(
            LintError(1, 10, "parameter-list-spacing", "Unexpected whitespace")
        )
        assertThat(ParameterListSpacingRule().format(code)).isEqualTo(formattedCode)
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
        assertThat(ParameterListSpacingRule().lint(code)).containsExactly(
            LintError(1, 10, "parameter-list-spacing", "Whitespace after ':' is missing")
        )
        assertThat(ParameterListSpacingRule().format(code)).isEqualTo(formattedCode)
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
        assertThat(ParameterListSpacingRule().lint(code)).containsExactly(
            LintError(1, 11, "parameter-list-spacing", "Expected a single space")
        )
        assertThat(ParameterListSpacingRule().format(code)).isEqualTo(formattedCode)
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
        assertThat(ParameterListSpacingRule().lint(code)).containsExactly(
            LintError(1, 11, "parameter-list-spacing", "Expected a single space")
        )
        assertThat(ParameterListSpacingRule().format(code)).isEqualTo(formattedCode)
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
        assertThat(ParameterListSpacingRule().lint(code)).containsExactly(
            LintError(1, 16, "parameter-list-spacing", "Expected a single space"),
            LintError(2, 25, "parameter-list-spacing", "Expected a single space"),
            LintError(5, 28, "parameter-list-spacing", "Expected a single space")
        )
        assertThat(ParameterListSpacingRule().format(code)).isEqualTo(formattedCode)
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
        assertThat(ParameterListSpacingRule().lint(code)).containsExactly(
            LintError(1, 16, "parameter-list-spacing", "Expected a single space"),
            LintError(3, 25, "parameter-list-spacing", "Expected a single space"),
            LintError(7, 28, "parameter-list-spacing", "Expected a single space")
        )
        assertThat(ParameterListSpacingRule().format(code)).isEqualTo(formattedCode)
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
        assertThat(ParameterListSpacingRule().lint(code)).containsExactly(
            LintError(2, 15, "parameter-list-spacing", "Expected a single space"),
            LintError(2, 25, "parameter-list-spacing", "Expected a single space"),
            LintError(2, 38, "parameter-list-spacing", "Expected a single space")
        )
        assertThat(ParameterListSpacingRule().format(code)).isEqualTo(formattedCode)
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
        assertThat(ParameterListSpacingRule().lint(code)).containsExactly(
            LintError(3, 11, "parameter-list-spacing", "Expected a single space"),
            LintError(4, 13, "parameter-list-spacing", "Expected a single space"),
            LintError(5, 16, "parameter-list-spacing", "Expected a single space")
        )
        assertThat(ParameterListSpacingRule().format(code)).isEqualTo(formattedCode)
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
        assertThat(ParameterListSpacingRule().lint(code)).containsExactly(
            LintError(1, 15, "parameter-list-spacing", "Unexpected whitespace")
        )
        assertThat(ParameterListSpacingRule().format(code)).isEqualTo(formattedCode)
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
        assertThat(ParameterListSpacingRule().lint(code)).containsExactly(
            LintError(1, 15, "parameter-list-spacing", "Unexpected whitespace")
        )
        assertThat(ParameterListSpacingRule().format(code)).isEqualTo(formattedCode)
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
        assertThat(ParameterListSpacingRule().lint(code)).containsExactly(
            LintError(1, 15, "parameter-list-spacing", "Whitespace after ',' is missing")
        )
        assertThat(ParameterListSpacingRule().format(code)).isEqualTo(formattedCode)
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
        assertThat(ParameterListSpacingRule().lint(code)).containsExactly(
            LintError(1, 16, "parameter-list-spacing", "Expected a single space")
        )
        assertThat(ParameterListSpacingRule().format(code)).isEqualTo(formattedCode)
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
        assertThat(ParameterListSpacingRule().lint(code)).isEmpty()
        assertThat(ParameterListSpacingRule().format(code)).isEqualTo(code)
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
        assertThat(ParameterListSpacingRule().format(code)).isEqualTo(formattedCode)
    }

    private companion object {
        const val TOO_MANY_SPACES = "  "
    }
}
