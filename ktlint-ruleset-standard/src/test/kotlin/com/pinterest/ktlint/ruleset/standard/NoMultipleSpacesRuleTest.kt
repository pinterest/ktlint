package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThat
import com.pinterest.ktlint.test.SPACE
import com.pinterest.ktlint.test.TAB
import org.junit.jupiter.api.Test

class NoMultipleSpacesRuleTest {
    private val noMultipleSpacesRuleAssertThat = NoMultipleSpacesRule().assertThat()

    @Test
    fun `Given a whitespace element not being an indent containing multiple spaces then replace it with a single space`() {
        val code =
            """
            fun main() {
                x(1,$SPACE${SPACE}3)
            }
            """.trimIndent()
        val formattedCode =
            """
            fun main() {
                x(1,${SPACE}3)
            }
            """.trimIndent()
        noMultipleSpacesRuleAssertThat(code)
            .hasLintViolation(2, 10, "Unnecessary long whitespace")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a whitespace element containing multiple tabs then replace it with a single space`() {
        val code =
            """
            fun main() {
                x(1,$TAB${TAB}3)
                val fooBar = "Foo$TAB${TAB}Bar"
            }
            """.trimIndent()
        val formattedCode =
            """
            fun main() {
                x(1,${SPACE}3)
                val fooBar = "Foo$TAB${TAB}Bar"
            }
            """.trimIndent()
        noMultipleSpacesRuleAssertThat(code)
            .hasLintViolation(2, 10, "Unnecessary long whitespace")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given KDoc with multiple consecutive spaces in kdoc then do not return lint errors`() {
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
        noMultipleSpacesRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `test multiple spaces in the beginning of the file`() {
        val code = "  package my.company.android"
        // The formatted code still has an unwanted space at the beginning. However, from the perspective of this rule,
        // it is correct.
        val formattedCode = " package my.company.android"
        noMultipleSpacesRuleAssertThat(code)
            .hasLintViolation(1, 2, "Unnecessary long whitespace")
            .isFormattedAs(formattedCode)
    }
}
