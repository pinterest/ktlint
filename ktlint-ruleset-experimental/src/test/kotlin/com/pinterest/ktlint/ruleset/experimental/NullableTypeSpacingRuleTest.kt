package com.pinterest.ktlint.ruleset.experimental

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThat
import org.junit.jupiter.api.Test

class NullableTypeSpacingRuleTest {
    private val nullableTypeSpacingRuleAssertThat = NullableTypeSpacingRule().assertThat()

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
        nullableTypeSpacingRuleAssertThat(code)
            .hasLintViolation(1, 17, "Unexpected whitespace")
            .isFormattedAs(formattedCode)
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
        nullableTypeSpacingRuleAssertThat(code)
            .hasLintViolation(1, 22, "Unexpected whitespace")
            .isFormattedAs(formattedCode)
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
        nullableTypeSpacingRuleAssertThat(code)
            .hasLintViolation(1, 23, "Unexpected whitespace")
            .isFormattedAs(formattedCode)
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
        nullableTypeSpacingRuleAssertThat(code)
            .hasLintViolation(1, 11, "Unexpected whitespace")
            .isFormattedAs(formattedCode)
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
        nullableTypeSpacingRuleAssertThat(code)
            .hasLintViolation(1, 16, "Unexpected whitespace")
            .isFormattedAs(formattedCode)
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
        nullableTypeSpacingRuleAssertThat(code)
            .hasLintViolation(1, 23, "Unexpected whitespace")
            .isFormattedAs(formattedCode)
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
        nullableTypeSpacingRuleAssertThat(code)
            .hasLintViolation(1, 28, "Unexpected whitespace")
            .isFormattedAs(formattedCode)
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
        nullableTypeSpacingRuleAssertThat(code)
            .hasLintViolation(1, 18, "Unexpected whitespace")
            .isFormattedAs(formattedCode)
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
        nullableTypeSpacingRuleAssertThat(code)
            .hasLintViolation(1, 23, "Unexpected whitespace")
            .isFormattedAs(formattedCode)
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
        nullableTypeSpacingRuleAssertThat(code)
            .hasLintViolation(1, 24, "Unexpected whitespace")
            .isFormattedAs(formattedCode)
    }
}
