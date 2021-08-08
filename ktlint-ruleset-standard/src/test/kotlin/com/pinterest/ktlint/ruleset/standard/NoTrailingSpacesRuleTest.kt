package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.core.LintError
import com.pinterest.ktlint.test.format
import com.pinterest.ktlint.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class NoTrailingSpacesRuleTest {

    @Test
    fun testLint() {
        assertThat(NoTrailingSpacesRule().lint("fun main() {\n    val a = 1\n\n \n} "))
            .isEqualTo(
                listOf(
                    LintError(4, 1, "no-trailing-spaces", "Trailing space(s)"),
                    LintError(5, 2, "no-trailing-spaces", "Trailing space(s)")
                )
            )
    }

    @Test
    fun testFormat() {
        assertThat(NoTrailingSpacesRule().format("fun main() {\n    val a = 1 \n  \n \n} "))
            .isEqualTo("fun main() {\n    val a = 1\n\n\n}")
    }

    @Test
    fun `lint trailing spaces inside block comments`() {
        val code =
            """
            /*${" "}
             * Some comment${" "}
             */
            """.trimIndent()

        val actual = NoTrailingSpacesRule().lint(code)

        assertThat(actual)
            .isEqualTo(
                listOf(
                    LintError(1, 1, "no-trailing-spaces", "Trailing space(s)"),
                    LintError(2, 1, "no-trailing-spaces", "Trailing space(s)")
                )
            )
    }

    @Test
    fun `format trailing spaces inside block comments`() {
        val code =
            """
            /*${" "}
             * Some comment${" "}
             */
            """.trimIndent()

        val actual = NoTrailingSpacesRule().format(code)

        assertThat(actual)
            .isEqualTo(
                """
                /*
                 * Some comment
                 */
                """.trimIndent()
            )
    }
}
