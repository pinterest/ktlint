package com.pinterest.ktlint.ruleset.experimental

import com.pinterest.ktlint.core.LintError
import com.pinterest.ktlint.test.format
import com.pinterest.ktlint.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class TypeArgumentListSpacingRuleTest {
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
        assertThat(TypeArgumentListSpacingRule().lint(code)).containsExactly(
            LintError(1, 20, "type-argument-list-spacing", "No whitespace expected at this position"),
            LintError(1, 22, "type-argument-list-spacing", "No whitespace expected at this position"),
            LintError(1, 32, "type-argument-list-spacing", "No whitespace expected at this position"),
            LintError(1, 34, "type-argument-list-spacing", "No whitespace expected at this position")
        )
        assertThat(TypeArgumentListSpacingRule().format(code)).isEqualTo(formattedCode)
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
        assertThat(TypeArgumentListSpacingRule().lint(code)).containsExactly(
            LintError(1, 16, "type-argument-list-spacing", "No whitespace expected at this position"),
            LintError(1, 18, "type-argument-list-spacing", "No whitespace expected at this position"),
            LintError(2, 30, "type-argument-list-spacing", "No whitespace expected at this position"),
            LintError(2, 32, "type-argument-list-spacing", "No whitespace expected at this position")
        )
        assertThat(TypeArgumentListSpacingRule().format(code)).isEqualTo(formattedCode)
    }

    @Test
    fun `Given a type argument list inside a type reference then allow space after the argument list`() {
        val code =
            """
            fun foo(): List<RuleSet> { }
            var bar: List<Bar> = emptyList()
            """.trimIndent()
        assertThat(TypeArgumentListSpacingRule().lint(code)).isEmpty()
        assertThat(TypeArgumentListSpacingRule().format(code)).isEqualTo(code)
    }

    @Test
    fun `xxGiven a type argument list, containing unexpected spaces, in a super type call then remove the redundant spaces`() {
        val code =
            """
            val foo = compareBy<Foo> { foo -> foo.x() }
                .thenBy { 99 }
            """.trimIndent()
        assertThat(TypeArgumentListSpacingRule().lint(code)).isEmpty()
        assertThat(TypeArgumentListSpacingRule().format(code)).isEqualTo(code)
    }
}
