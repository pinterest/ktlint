package com.pinterest.ktlint.ruleset.experimental

import com.pinterest.ktlint.core.LintError
import com.pinterest.ktlint.test.format
import com.pinterest.ktlint.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class FunctionReturnTypeWrappingRuleTest {
    @Test
    fun `Given a single line function signature which is smaller than the max line length, and function is followed by a block, then do no change the signature`() {
        val code =
            """
            // Max line length marker:             $EOL_CHAR
            fun f(a: Any, b: Any, c: Any): String {
                // body
            }
            """.trimIndent()
        assertThat(
            FunctionReturnTypeWrappingRule().lint(code, code.setMaxLineLength())
        ).isEmpty()
        assertThat(
            FunctionReturnTypeWrappingRule().format(code, code.setMaxLineLength())
        ).isEqualTo(code)
    }

    @Test
    fun `Given a single line function signature which is smaller than or equal to the max line length, and function is followed by an expression, then do no change the signature`() {
        val code =
            """
            // Max line length marker:    $EOL_CHAR
            fun f(string: String): String = string.toUpperCase()
            """.trimIndent()
        assertThat(
            FunctionReturnTypeWrappingRule().lint(code, code.setMaxLineLength())
        ).isEmpty()
        assertThat(
            FunctionReturnTypeWrappingRule().format(code, code.setMaxLineLength())
        ).isEqualTo(code)
    }

    @Test
    fun `Given a single line function signature followed by a block with a length greater than the max line length, and function is followed by a block, then reformat`() {
        val code =
            """
            // Max line length marker:        $EOL_CHAR
            fun f(a: Any, b: Any, c: Any): String {
                // body
            }
            """.trimIndent()
        val formattedCode =
            // Note: parameters will be wrapped to separate lines by the parameter-list-wrapping rule
            """
            // Max line length marker:        $EOL_CHAR
            fun f(
                a: Any, b: Any, c: Any
            ): String {
                // body
            }
            """.trimIndent()
        assertThat(
            FunctionReturnTypeWrappingRule().lint(code, code.setMaxLineLength())
        ).containsExactly(
            LintError(2, 6, "function-return-type-wrapping", "Parameters should be on a separate line (unless entire function signature fits on a single line)"),
            LintError(2, 29, "function-return-type-wrapping", "Return type should be on separate line with closing parentheses (unless entire function signature fits on a single line)")
        )
        assertThat(
            FunctionReturnTypeWrappingRule().format(code, code.setMaxLineLength())
        ).isEqualTo(formattedCode)
    }

    @Test
    fun `Given a single line function signature which is greater than the max line length, and parameter list contains a block comment, then reformat`() {
        val code =
            """
            // Max line length marker:   $EOL_CHAR
            fun f(string: String): String = string.toUpperCase()
            """.trimIndent()
        val formattedCode =
            """
            // Max line length marker:   $EOL_CHAR
            fun f(
                string: String
            ): String = string.toUpperCase()
            """.trimIndent()
        assertThat(
            FunctionReturnTypeWrappingRule().format(
                code,
                userData = code.setMaxLineLength()
            )
        ).isEqualTo(formattedCode)
    }

    @Test
    fun `Given a single line function signature contain a block comment in the parameter list and signature greater than the max line length, then reformat`() {
        val code =
            """
            // Max line length marker:      $EOL_CHAR
            fun f( /* some comment */ ): String {
                // body
            }
            """.trimIndent()
        val formattedCode =
            """
            // Max line length marker:      $EOL_CHAR
            fun f(
                /* some comment */
            ): String {
                // body
            }
            """.trimIndent()
        assertThat(
            FunctionReturnTypeWrappingRule().format(code, code.setMaxLineLength())
        ).isEqualTo(formattedCode)
    }

    @Test
    fun `Given a multiline function signature with newline between closing parentheses and colon then reformat`() {
        val code =
            """
            fun f(a: Any, b: Any, c: Any)
                : String {
                // body
            }
            """.trimIndent()
        val formattedCode =
            // Note: parameters will be wrapped to separate lines by the parameter-list-wrapping rule
            """
            fun f(
                a: Any, b: Any, c: Any
            ): String {
                // body
            }
            """.trimIndent()
        assertThat(
            FunctionReturnTypeWrappingRule().lint(code)
        ).containsExactly(
            LintError(1, 6, "function-return-type-wrapping", "Parameters should be on a separate line (unless entire function signature fits on a single line)"),
            LintError(1, 29, "function-return-type-wrapping", "Return type should be on separate line with closing parentheses (unless entire function signature fits on a single line)")
        )
        assertThat(
            FunctionReturnTypeWrappingRule().format(code)
        ).isEqualTo(formattedCode)
    }

    @Test
    fun `Given a multiline function signature with newline between colon and the return type then reformat`() {
        val code =
            """
            fun f(a: Any, b: Any, c: Any):
                String {
                // body
            }
            """.trimIndent()
        val formattedCode =
            // Note: parameters will be wrapped to separate lines by the parameter-list-wrapping rule
            """
            fun f(
                a: Any, b: Any, c: Any
            ): String {
                // body
            }
            """.trimIndent()
        assertThat(
            FunctionReturnTypeWrappingRule().lint(code)
        ).containsExactly(
            LintError(1, 6, "function-return-type-wrapping", "Parameters should be on a separate line (unless entire function signature fits on a single line)"),
            LintError(1, 29, "function-return-type-wrapping", "Return type should be on separate line with closing parentheses (unless entire function signature fits on a single line)")
        )
        assertThat(
            FunctionReturnTypeWrappingRule().format(code)
        ).isEqualTo(formattedCode)
    }

    private fun String.setMaxLineLength(): Map<String, String> =
        split("\n")
            .first { it.startsWith("//") && it.contains(EOL_CHAR) }
            .indexOf(EOL_CHAR)
            .let { index ->
                mapOf(
                    "max_line_length" to (index + 1).toString()
                )
            }

    private companion object {
        const val EOL_CHAR = '#'
    }
}
