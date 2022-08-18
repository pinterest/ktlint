package com.pinterest.ktlint.ruleset.experimental

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRule
import com.pinterest.ktlint.test.LintViolation
import org.junit.jupiter.api.Test

class FunctionTypeReferenceSpacingRuleTest {
    private val functionTypeReferenceSpacingRuleAssertThat = assertThatRule { FunctionTypeReferenceSpacingRule() }

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
        functionTypeReferenceSpacingRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(1, 11, "Unexpected whitespace"),
                LintViolation(2, 11, "Unexpected whitespace"),
            ).isFormattedAs(formattedCode)
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
        functionTypeReferenceSpacingRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(1, 11, "Unexpected whitespace"),
                LintViolation(2, 11, "Unexpected whitespace"),
            ).isFormattedAs(formattedCode)
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
        functionTypeReferenceSpacingRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(1, 12, "Unexpected whitespace"),
                LintViolation(2, 12, "Unexpected whitespace"),
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a function signature without a type reference before the function name then do not change the signature`() {
        val code =
            """
            fun foo1() = "some-result"
            """.trimIndent()
        functionTypeReferenceSpacingRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Issue 1440 - Given an anonymous function without receiver type then do not reformat`() {
        val code =
            """
            val anonymousFunction = fun(foo: Boolean): String? = if (foo) "Test string" else null
            """.trimIndent()
        functionTypeReferenceSpacingRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Given an anonymous function with receiver type then do not reformat`() {
        val code =
            """
            val anonymousFunction = fun Boolean.(): String? = if (this) "Test string" else null
            """.trimIndent()
        functionTypeReferenceSpacingRuleAssertThat(code).hasNoLintViolations()
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
        functionTypeReferenceSpacingRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(1, 36, "Unexpected whitespace"),
                LintViolation(1, 38, "Unexpected whitespace"),
                LintViolation(1, 40, "Unexpected whitespace"),
            ).isFormattedAs(formattedCode)
    }
}
