package com.pinterest.ktlint.ruleset.experimental

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThat
import com.pinterest.ktlint.test.LintViolation
import org.junit.jupiter.api.Test

class FunctionTypeReferenceSpacingRuleTest {
    private val functionTypeReferenceSpacingRuleAssertThat = FunctionTypeReferenceSpacingRule().assertThat()

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
                LintViolation(2, 11, "Unexpected whitespace")
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
                LintViolation(2, 11, "Unexpected whitespace")
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
                LintViolation(2, 12, "Unexpected whitespace")
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
}
