package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRule
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class FunctionTypeModifierSpacingRuleTest {
    private val assertThatRule = assertThatRule { FunctionTypeModifierSpacingRule() }

    @Nested
    inner class `Missing space before the function type` {
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
    }

    @Nested
    inner class `Exactly one space before the function type` {
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

    @Nested
    inner class `Too many spaces before the function type` {
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
    }

    @Nested
    inner class `Given unexpected newline before the function type` {
        @Test
        fun `Given unexpected newline between the modifier list and the function property type`() {
            val code =
                """
                val foo: suspend
                         () -> Unit = {}
                """.trimIndent()
            val formattedCode =
                """
                val foo: suspend () -> Unit = {}
                """.trimIndent()
            assertThatRule(code)
                .hasLintViolation(2, 10, "Expected a single space between the modifier list and the function type")
                .isFormattedAs(formattedCode)
        }

        @Test
        fun `Given unexpected newline between the modifier list and the function parameter type`() {
            val code =
                """
                suspend fun bar(
                    baz: suspend
                         () -> Unit
                ) = baz()
                """.trimIndent()
            val formattedCode =
                """
                suspend fun bar(
                    baz: suspend () -> Unit
                ) = baz()
                """.trimIndent()
            assertThatRule(code)
                .hasLintViolation(3, 10, "Expected a single space between the modifier list and the function type")
                .isFormattedAs(formattedCode)
        }
    }
}
