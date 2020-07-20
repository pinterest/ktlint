package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.core.LintError
import com.pinterest.ktlint.test.format
import com.pinterest.ktlint.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class NoMultipleSpacesRuleTest {

    @Test
    fun testLint() {
        assertThat(NoMultipleSpacesRule().lint("fun main() { x(1,3);  x(1, 3)\n  \n  }"))
            .isEqualTo(
                listOf(
                    LintError(1, 22, "no-multi-spaces", "Unnecessary space(s)")
                )
            )
    }

    @Test
    fun testFormat() {
        assertThat(NoMultipleSpacesRule().format("fun main() { x(1,3);  x(1, 3)\n  \n  }"))
            .isEqualTo("fun main() { x(1,3); x(1, 3)\n  \n  }")
    }

    @Test
    fun `lint multiple spaces in kdoc allowed`() {
        assertThat(
            NoMultipleSpacesRule().lint(
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
            )
        ).isEmpty()
    }

    @Test
    fun `format multiple spaces in kdoc allowed`() {
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
        assertThat(NoMultipleSpacesRule().format(code)).isEqualTo(code)
    }

    @Test
    fun `test multiple spaces in the beginning of the file`() {
        assertThat(NoMultipleSpacesRule().lint("  package my.company.android"))
            .isEqualTo(
                listOf(
                    LintError(1, 2, "no-multi-spaces", "Unnecessary space(s)")
                )
            )
    }
}
