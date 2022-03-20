package com.pinterest.ktlint.ruleset.experimental

import com.pinterest.ktlint.core.LintError
import com.pinterest.ktlint.test.format
import com.pinterest.ktlint.test.lint
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test

class NullableTypeSpacingRuleTest {
    @Test
    fun `Given a simple nullable type with a space before the quest then remove this space`() {
        val code =
            """
            val foo : String ? = null
            """.trimIndent()
        val formattedCode =
            """
            val foo : String? = null
            """.trimIndent()
        Assertions.assertThat(NullableTypeSpacingRule().lint(code)).containsExactly(
            LintError(1, 17, "nullable-type-spacing", "Unexpected whitespace")
        )
        Assertions.assertThat(NullableTypeSpacingRule().format(code)).isEqualTo(formattedCode)
    }

    @Test
    fun `Given a non-nullable list of a nullable type with a space before the quest then remove this space`() {
        val code =
            """
            val foo : List<String ?> = listOf(null)
            """.trimIndent()
        val formattedCode =
            """
            val foo : List<String?> = listOf(null)
            """.trimIndent()
        Assertions.assertThat(NullableTypeSpacingRule().lint(code)).containsExactly(
            LintError(1, 22, "nullable-type-spacing", "Unexpected whitespace")
        )
        Assertions.assertThat(NullableTypeSpacingRule().format(code)).isEqualTo(formattedCode)
    }

    @Test
    fun `Given a nullable list of a non-nullable type with a space before the quest then remove this space`() {
        val code =
            """
            val foo : List<String> ? = null
            """.trimIndent()
        val formattedCode =
            """
            val foo : List<String>? = null
            """.trimIndent()
        Assertions.assertThat(NullableTypeSpacingRule().lint(code)).containsExactly(
            LintError(1, 23, "nullable-type-spacing", "Unexpected whitespace")
        )
        Assertions.assertThat(NullableTypeSpacingRule().format(code)).isEqualTo(formattedCode)
    }

    @Test
    fun `Given a type receiver of nullable simple type with a space before the quest then remove this space`() {
        val code =
            """
            fun String ?.foo() = "some-result"
            """.trimIndent()
        val formattedCode =
            """
            fun String?.foo() = "some-result"
            """.trimIndent()
        Assertions.assertThat(NullableTypeSpacingRule().lint(code)).containsExactly(
            LintError(1, 11, "nullable-type-spacing", "Unexpected whitespace")
        )
        Assertions.assertThat(NullableTypeSpacingRule().format(code)).isEqualTo(formattedCode)
    }

    @Test
    fun `Given a type receiver of a non-nullable list of a nullable type with a space before the quest then remove this space`() {
        val code =
            """
            fun List<String ?>.foo() = "some-result"
            """.trimIndent()
        val formattedCode =
            """
            fun List<String?>.foo() = "some-result"
            """.trimIndent()
        Assertions.assertThat(NullableTypeSpacingRule().lint(code)).containsExactly(
            LintError(1, 16, "nullable-type-spacing", "Unexpected whitespace")
        )
        Assertions.assertThat(NullableTypeSpacingRule().format(code)).isEqualTo(formattedCode)
    }

    @Test
    fun `Given a parameter of a nullable type with a space before the quest then remove this space`() {
        val code =
            """
            fun foo(string: String ?) = "some-result"
            """.trimIndent()
        val formattedCode =
            """
            fun foo(string: String?) = "some-result"
            """.trimIndent()
        Assertions.assertThat(NullableTypeSpacingRule().lint(code)).containsExactly(
            LintError(1, 23, "nullable-type-spacing", "Unexpected whitespace")
        )
        Assertions.assertThat(NullableTypeSpacingRule().format(code)).isEqualTo(formattedCode)
    }

    @Test
    fun `Given a parameter of a list of a nullable type with a space before the quest then remove this space`() {
        val code =
            """
            fun foo(string: List<String ?>) = "some-result"
            """.trimIndent()
        val formattedCode =
            """
            fun foo(string: List<String?>) = "some-result"
            """.trimIndent()
        Assertions.assertThat(NullableTypeSpacingRule().lint(code)).containsExactly(
            LintError(1, 28, "nullable-type-spacing", "Unexpected whitespace")
        )
        Assertions.assertThat(NullableTypeSpacingRule().format(code)).isEqualTo(formattedCode)
    }

    @Test
    fun `Given a nullable function return type with a space before the quest then remove this space`() {
        val code =
            """
            fun foo(): String ? = "some-result"
            """.trimIndent()
        val formattedCode =
            """
            fun foo(): String? = "some-result"
            """.trimIndent()
        Assertions.assertThat(NullableTypeSpacingRule().lint(code)).containsExactly(
            LintError(1, 18, "nullable-type-spacing", "Unexpected whitespace")
        )
        Assertions.assertThat(NullableTypeSpacingRule().format(code)).isEqualTo(formattedCode)
    }

    @Test
    fun `Given a return type of a non-nullable list of a nullable type with a space before the quest then remove this space`() {
        val code =
            """
            fun foo(): List<String ?> = listOf("some-result", null)
            """.trimIndent()
        val formattedCode =
            """
            fun foo(): List<String?> = listOf("some-result", null)
            """.trimIndent()
        Assertions.assertThat(NullableTypeSpacingRule().lint(code)).containsExactly(
            LintError(1, 23, "nullable-type-spacing", "Unexpected whitespace")
        )
        Assertions.assertThat(NullableTypeSpacingRule().format(code)).isEqualTo(formattedCode)
    }

    @Test
    fun `Given a return type of a nullable list with a space before the quest then remove this space`() {
        val code =
            """
            fun foo(): List<String> ? = null
            """.trimIndent()
        val formattedCode =
            """
            fun foo(): List<String>? = null
            """.trimIndent()
        Assertions.assertThat(NullableTypeSpacingRule().lint(code)).containsExactly(
            LintError(1, 24, "nullable-type-spacing", "Unexpected whitespace")
        )
        Assertions.assertThat(NullableTypeSpacingRule().format(code)).isEqualTo(formattedCode)
    }
}
