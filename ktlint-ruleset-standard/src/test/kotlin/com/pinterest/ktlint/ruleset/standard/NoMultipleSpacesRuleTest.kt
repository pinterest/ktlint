package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.core.LintError
import com.pinterest.ktlint.test.format
import com.pinterest.ktlint.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class NoMultipleSpacesRuleTest {
    @Test
    fun `Given a whitespace element not being an indent containing multiple spaces then replace it with a single space`() {
        val code =
            """
            fun main() {
                x(1,${SPACE}${SPACE}3)
            }
            """.trimIndent()
        val formattedCode =
            """
            fun main() {
                x(1,${SPACE}3)
            }
            """.trimIndent()
        assertThat(NoMultipleSpacesRule().lint(code))
            .isEqualTo(
                listOf(
                    LintError(2, 10, "no-multi-spaces", "Unnecessary long whitespace")
                )
            )
        assertThat(NoMultipleSpacesRule().format(code)).isEqualTo(formattedCode)
    }

    @Test
    fun `Given a whitespace element containing multiple tabs then replace it with a single space`() {
        val code =
            """
            fun main() {
                x(1,${TAB}${TAB}3)
                val fooBar = "Foo${TAB}${TAB}Bar"
            }
            """.trimIndent()
        val formattedCode =
            """
            fun main() {
                x(1,${SPACE}3)
                val fooBar = "Foo${TAB}${TAB}Bar"
            }
            """.trimIndent()
        assertThat(NoMultipleSpacesRule().lint(code))
            .isEqualTo(
                listOf(
                    LintError(2, 10, "no-multi-spaces", "Unnecessary long whitespace")
                )
            )
        assertThat(NoMultipleSpacesRule().format(code)).isEqualTo(formattedCode)
    }

    @Test
    fun `lint multiple spaces in kdoc allowed`() {
        val code =
            """
            /**
             * Gets Blabla from user.
             *
             * @param blabls12      1234
             * @param blabla123     6789
             * @param blabla        5678
             * @param longparam345  4567890
             * @param userId        567890
             * @return the user profile
             *
             */
            """.trimIndent()
        assertThat(NoMultipleSpacesRule().lint(code)).isEmpty()
        assertThat(NoMultipleSpacesRule().format(code)).isEqualTo(code)
    }

    @Test
    fun `test multiple spaces in the beginning of the file`() {
        val code = "  package my.company.android"
        assertThat(NoMultipleSpacesRule().lint(code)).containsExactly(
            LintError(1, 2, "no-multi-spaces", "Unnecessary long whitespace")
        )
    }

    private companion object {
        const val SPACE = " "
        const val TAB = "\t"
    }
}
