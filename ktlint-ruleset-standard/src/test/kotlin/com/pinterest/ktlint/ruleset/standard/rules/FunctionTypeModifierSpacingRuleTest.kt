package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRule
import org.junit.jupiter.api.Test

class FunctionTypeModifierSpacingRuleTest {
    private val assertThatRule = assertThatRule { FunctionTypeModifierSpacingRule() }

    @Test
    fun `Given no space between the modifier list and the function property type`() {
        val code =
            """
            val foo: suspend() -> Unit = {}
            """.trimIndent()
        val formattedCode =
            """
            val foo: suspend () -> Unit = {}
            """.trimIndent()
        assertThatRule(code)
            .hasLintViolation(1, 17, "Expected a single space between the modifier list and the function type")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given no space between the modifier list and the function parameter type`() {
        val code =
            """
            suspend fun bar(baz: suspend() -> Unit) = baz()
            """.trimIndent()
        val formattedCode =
            """
            suspend fun bar(baz: suspend () -> Unit) = baz()
            """.trimIndent()
        assertThatRule(code)
            .hasLintViolation(1, 29, "Expected a single space between the modifier list and the function type")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given multiple spaces between the modifier list and the function property type`() {
        val code =
            """
            val foo: suspend  () -> Unit = {}
            """.trimIndent()
        val formattedCode =
            """
            val foo: suspend () -> Unit = {}
            """.trimIndent()
        assertThatRule(code)
            .hasLintViolation(1, 19, "Expected a single space between the modifier list and the function type")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given multiple spaces between the modifier list and the function parameter type`() {
        val code =
            """
            suspend fun bar(baz: suspend    () -> Unit) = baz()
            """.trimIndent()
        val formattedCode =
            """
            suspend fun bar(baz: suspend () -> Unit) = baz()
            """.trimIndent()
        assertThatRule(code)
            .hasLintViolation(1, 33, "Expected a single space between the modifier list and the function type")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a single space between the modifier list and the function property type`() {
        val code =
            """
            val foo: suspend () -> Unit = {}
            """.trimIndent()
        assertThatRule(code).hasNoLintViolations()
    }

    @Test
    fun `Given a single space between the modifier list and the function parameter type`() {
        val code =
            """
            suspend fun bar(baz: suspend () -> Unit) = baz()
            """.trimIndent()
        assertThatRule(code).hasNoLintViolations()
    }
}
