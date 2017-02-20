package com.github.shyiko.ktlint.ruleset.standard

import com.gihub.shyiko.ktlint.ruleset.standard.CapitalizedClassNamesRule
import com.github.shyiko.ktlint.core.LintError
import com.github.shyiko.ktlint.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.testng.annotations.Test

class CapitalizedClassNamesRuleTest {

    val rule = CapitalizedClassNamesRule()

    @Test
    fun `has error for non capitalized class`() {
        assertThat(
            """
            class myClass
            """.lint()
        ).isEqualTo(
            LintError(1, 7, "capitalized-class-name", "Class `myClass` should be capitalized").asList()
        )
    }

    @Test
    fun `has error for non capitalized interface`() {
        assertThat(
            """
            interface myClass { fun foo() {} }
            """.lint()
        ).isEqualTo(
            LintError(1, 11, "capitalized-class-name", "Class `myClass` should be capitalized").asList()
        )
    }

    @Test
    fun `has error for inner classes`() {
        assertThat(
            """
            class MyClass {
                data class inner(val x: String)
            }
            """.lint()
        ).isEqualTo(
            LintError(2, 16, "capitalized-class-name", "Class `inner` should be capitalized").asList()
        )
    }

    @Test
    fun `has no errors for capitalized classes`() {
        assertThat("""
            class MyClass {
                data class Inner(val x: String)
            }
            """.lint()
        ).isEqualTo(noErrors())
    }

    private fun String.lint(): List<LintError> = rule.lint(this.trimIndent())

    private fun LintError.asList(): List<LintError> = listOf(this)

    private fun noErrors(): List<LintError> = emptyList()
}
