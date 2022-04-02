package com.pinterest.ktlint.ruleset.experimental

import com.pinterest.ktlint.core.LintError
import com.pinterest.ktlint.test.format
import com.pinterest.ktlint.test.lint
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test

class FunctionTypeReferenceSpacingRuleTest {
    @Test
    fun `Given a function signature with whitespace after a non nullable type reference of an extension function then remove this whitespace`() {
        val code =
            """
            fun String .foo1() = "some-result"
            fun String
                .foo2() = "some-result"
            """.trimIndent()
        val formattedCode =
            """
            fun String.foo1() = "some-result"
            fun String.foo2() = "some-result"
            """.trimIndent()
        Assertions.assertThat(
            FunctionTypeReferenceSpacingRule().lint(code)
        ).containsExactly(
            LintError(1, 11, "function-type-reference-spacing", "Unexpected whitespace"),
            LintError(2, 11, "function-type-reference-spacing", "Unexpected whitespace")
        )
        Assertions.assertThat(
            FunctionTypeReferenceSpacingRule().format(code)
        ).isEqualTo(formattedCode)
    }

    @Test
    fun `Given a function signature with whitespace in a nullable type reference of an extension function`() {
        val code =
            """
            fun String ?.foo1() = "some-result"
            fun String
                ?.foo2() = "some-result"
            """.trimIndent()
        val formattedCode =
            """
            fun String?.foo1() = "some-result"
            fun String?.foo2() = "some-result"
            """.trimIndent()
        Assertions.assertThat(
            FunctionTypeReferenceSpacingRule().lint(code)
        ).containsExactly(
            LintError(1, 11, "function-type-reference-spacing", "Unexpected whitespace"),
            LintError(2, 11, "function-type-reference-spacing", "Unexpected whitespace")
        )
        Assertions.assertThat(
            FunctionTypeReferenceSpacingRule().format(code)
        ).isEqualTo(formattedCode)
    }

    @Test
    fun `Given a function signature with whitespace after a nullable type reference of an extension function`() {
        val code =
            """
            fun String? .foo1() = "some-result"
            fun String?
                .foo2() = "some-result"
            """.trimIndent()
        val formattedCode =
            """
            fun String?.foo1() = "some-result"
            fun String?.foo2() = "some-result"
            """.trimIndent()
        Assertions.assertThat(
            FunctionTypeReferenceSpacingRule().lint(code)
        ).containsExactly(
            LintError(1, 12, "function-type-reference-spacing", "Unexpected whitespace"),
            LintError(2, 12, "function-type-reference-spacing", "Unexpected whitespace")
        )
        Assertions.assertThat(
            FunctionTypeReferenceSpacingRule().format(code)
        ).isEqualTo(formattedCode)
    }

    @Test
    fun `Given a function signature without a type reference before the function name then do not change the signature`() {
        val code =
            """
            fun foo1() = "some-result"
            """.trimIndent()
        Assertions.assertThat(
            FunctionTypeReferenceSpacingRule().lint(code)
        ).isEmpty()
        Assertions.assertThat(
            FunctionTypeReferenceSpacingRule().format(code)
        ).isEqualTo(code)
    }

    @Test
    fun `Issue 1440 - Given an anonymous function without receiver type then do not reformat`() {
        val code =
            """
            val anonymousFunction = fun(foo: Boolean): String? = if (foo) "Test string" else null
            """.trimIndent()
        Assertions.assertThat(
            FunctionTypeReferenceSpacingRule().lint(code)
        ).isEmpty()
        Assertions.assertThat(
            FunctionTypeReferenceSpacingRule().format(code)
        ).isEqualTo(code)
    }

    @Test
    fun `Given an anonymous function with receiver type then do not reformat`() {
        val code =
            """
            val anonymousFunction = fun Boolean.(): String? = if (this) "Test string" else null
            """.trimIndent()
        Assertions.assertThat(
            FunctionTypeReferenceSpacingRule().lint(code)
        ).isEmpty()
        Assertions.assertThat(
            FunctionTypeReferenceSpacingRule().format(code)
        ).isEqualTo(code)
    }

    @Test
    fun `Given an anonymous function with receiver type followed by an unexpected space then do reformat`() {
        val code =
            """
            val anonymousFunction = fun Boolean ? . (): String? = this?.let { "Test string" }
            """.trimIndent()
        val formattedCode =
            """
            val anonymousFunction = fun Boolean?.(): String? = this?.let { "Test string" }
            """.trimIndent()
        Assertions.assertThat(
            FunctionTypeReferenceSpacingRule().lint(code)
        ).containsExactly(
            LintError(1, 36, "function-type-reference-spacing", "Unexpected whitespace"),
            LintError(1, 38, "function-type-reference-spacing", "Unexpected whitespace"),
            LintError(1, 40, "function-type-reference-spacing", "Unexpected whitespace")
        )
        Assertions.assertThat(
            FunctionTypeReferenceSpacingRule().format(code)
        ).isEqualTo(formattedCode)
    }
}
