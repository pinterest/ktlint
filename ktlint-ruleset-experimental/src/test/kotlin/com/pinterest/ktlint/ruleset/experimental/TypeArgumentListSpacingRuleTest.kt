package com.pinterest.ktlint.ruleset.experimental

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRule
import com.pinterest.ktlint.test.LintViolation
import org.junit.jupiter.api.Test

class TypeArgumentListSpacingRuleTest {
    private val typeArgumentListSpacingRuleAssertThat = assertThatRule { TypeArgumentListSpacingRule() }

    @Test
    fun `Given a type argument list, containing unexpected spaces, in a function call then remove the redundant spaces`() {
        val code =
            """
            val res = ArrayList < LintError > ()
            """.trimIndent()
        val formattedCode =
            """
            val res = ArrayList<LintError>()
            """.trimIndent()
        typeArgumentListSpacingRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(1, 20, "No whitespace expected at this position"),
                LintViolation(1, 22, "No whitespace expected at this position"),
                LintViolation(1, 32, "No whitespace expected at this position"),
                LintViolation(1, 34, "No whitespace expected at this position")
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a type argument list, containing unexpected spaces, in a super type call then remove the redundant spaces`() {
        val code =
            """
            class B<T> : A< T >() {
                override fun x() = super< A >.x()
            }
            """.trimIndent()
        val formattedCode =
            """
            class B<T> : A<T>() {
                override fun x() = super<A>.x()
            }
            """.trimIndent()
        typeArgumentListSpacingRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(1, 16, "No whitespace expected at this position"),
                LintViolation(1, 18, "No whitespace expected at this position"),
                LintViolation(2, 30, "No whitespace expected at this position"),
                LintViolation(2, 32, "No whitespace expected at this position")
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a type argument list inside a type reference then allow space after the argument list`() {
        val code =
            """
            fun foo(): List<RuleSet> { }
            var bar: List<Bar> = emptyList()
            """.trimIndent()
        typeArgumentListSpacingRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Given a call expression with lambda then allow the space after the type argument list`() {
        val code =
            """
            val foo = compareBy<Foo> { foo -> foo.x() }
                .thenBy { 99 }
            """.trimIndent()
        typeArgumentListSpacingRuleAssertThat(code).hasNoLintViolations()
    }
}
