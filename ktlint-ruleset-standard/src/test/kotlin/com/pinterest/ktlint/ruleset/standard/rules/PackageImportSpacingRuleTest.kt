package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRule
import org.junit.jupiter.api.Test

class PackageImportSpacingRuleTest {
    private val packageImportSpacingRuleAssertThat = assertThatRule { PackageImportSpacingRule() }

    @Test
    fun `Given a blank line exists between package and import then do not emit`() {
        val code =
            """
            package foo

            import bar.*
            """.trimIndent()
        packageImportSpacingRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Given no blank line between package and import then emit and autocorrect`() {
        val code =
            """
            package foo
            import bar.*
            """.trimIndent()
        val formattedCode =
            """
            package foo

            import bar.*
            """.trimIndent()
        packageImportSpacingRuleAssertThat(code)
            .hasLintViolation(2, 1, "Expected exactly one blank line between package statement and import statements")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given too many blank lines between package and import then emit and autocorrect`() {
        val code =
            """
            package foo


            import bar.*
            """.trimIndent()
        val formattedCode =
            """
            package foo

            import bar.*
            """.trimIndent()
        packageImportSpacingRuleAssertThat(code)
            .hasLintViolation(2, 1, "Expected exactly one blank line between package statement and import statements")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a comment above import and no blank line after package then emit and autocorrect`() {
        val code =
            """
            package foo
            // Some comment
            import bar.*
            """.trimIndent()
        val formattedCode =
            """
            package foo

            // Some comment
            import bar.*
            """.trimIndent()
        packageImportSpacingRuleAssertThat(code)
            .hasLintViolation(2, 1, "Expected exactly one blank line between package statement and import statements")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given no package statement then do not emit`() {
        val code =
            """
            import bar.*
            """.trimIndent()
        packageImportSpacingRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Given no imports then do not emit`() {
        val code =
            """
            package foo
            """.trimIndent()
        packageImportSpacingRuleAssertThat(code).hasNoLintViolations()
    }
}
