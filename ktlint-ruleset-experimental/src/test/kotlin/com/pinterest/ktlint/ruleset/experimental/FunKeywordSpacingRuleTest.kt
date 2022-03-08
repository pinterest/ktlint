package com.pinterest.ktlint.ruleset.experimental

import com.pinterest.ktlint.core.LintError
import com.pinterest.ktlint.test.format
import com.pinterest.ktlint.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class FunKeywordSpacingRuleTest {
    @Test
    fun `Given a function signature with multiple spaces between the fun keyword and the function name then remove the redundant spaces`() {
        val code =
            """
            fun  foo() = "some-result"
            """.trimIndent()
        val formattedCode =
            """
            fun foo() = "some-result"
            """.trimIndent()
        assertThat(FunKeywordSpacingRule().lint(code)).containsExactly(
            LintError(1, 4, "fun-keyword-spacing", "Single space expected after the fun keyword")
        )
        assertThat(FunKeywordSpacingRule().format(code)).isEqualTo(formattedCode)
    }

    @Test
    fun `Given a function signature with a newline between the fun keyword and the function name then remove the redundant newline`() {
        val code =
            """
            fun
            foo() = "some-result"
            """.trimIndent()
        val formattedCode =
            """
            fun foo() = "some-result"
            """.trimIndent()
        assertThat(FunKeywordSpacingRule().lint(code)).containsExactly(
            LintError(1, 4, "fun-keyword-spacing", "Single space expected after the fun keyword")
        )
        assertThat(FunKeywordSpacingRule().format(code)).isEqualTo(formattedCode)
    }
}
