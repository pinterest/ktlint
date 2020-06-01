package com.pinterest.ktlint.ruleset.experimental

import com.pinterest.ktlint.core.LintError
import com.pinterest.ktlint.test.format
import com.pinterest.ktlint.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.Assert.assertEquals
import org.junit.Test

class SpacingBetweenDeclarationsWithCommentsRuleTest {
    @Test
    fun `comment at top of file should do nothing`() {
        assertThat(
            SpacingBetweenDeclarationsWithCommentsRule().lint(
                """
                /**
                 * foo
                 */
                fun a()
                """.trimIndent()
            )
        ).isEmpty()
    }

    @Test
    fun `multiple comments should do nothing`() {
        assertThat(
            SpacingBetweenDeclarationsWithCommentsRule().lint(
                """
                /**
                 * boo
                 */
                /**
                 * foo
                 */
                fun a()
                """.trimIndent()
            )
        ).isEmpty()

        assertThat(
            SpacingBetweenDeclarationsWithCommentsRule().lint(
                """
                // bar
                /**
                 * foo
                 */
                fun a()
                """.trimIndent()
            )
        ).isEmpty()

        // This matches the Kotlin plugin behavior although I sort of think it violates the spirit of the rule.
        assertThat(
            SpacingBetweenDeclarationsWithCommentsRule().lint(
                """
                fun blah()
                // foo
                /**
                 * Doc 1
                 */
                const val bar = 1
                """.trimIndent()
            )
        ).isEmpty()
    }

    @Test
    fun `missing space before declaration with kdoc should cause error`() {
        assertThat(
            SpacingBetweenDeclarationsWithCommentsRule().lint(
                """
                fun a()
                /**
                 * foo
                 */
                fun b()
                """.trimIndent()
            )
        ).isEqualTo(
            listOf(
                LintError(
                    2,
                    1,
                    "spacing-between-declarations-with-comments",
                    "Declarations and declarations with comments should have an empty space between."
                )
            )
        )
    }

    @Test
    fun `missing space before declaration with eol comment should cause error`() {
        assertThat(
            SpacingBetweenDeclarationsWithCommentsRule().lint(
                """
                fun a()
                // foo
                fun b()
                """.trimIndent()
            )
        ).isEqualTo(
            listOf(
                LintError(
                    2,
                    1,
                    "spacing-between-declarations-with-comments",
                    "Declarations and declarations with comments should have an empty space between."
                )
            )
        )
    }

    @Test
    fun `autoformat should work correctly`() {
        assertEquals(
            """
            /**
             * Doc 1
             */
            fun one() = 1
            
            /**
             * Doc 2
             */
            fun two() = 2
            fun three() = 42
            
            // comment
            fun four() = 44
            """.trimIndent(),
            SpacingBetweenDeclarationsWithCommentsRule().format(
                """
                /**
                 * Doc 1
                 */
                fun one() = 1
                /**
                 * Doc 2
                 */
                fun two() = 2
                fun three() = 42
                // comment
                fun four() = 44
                """.trimIndent()
            )
        )
    }
}
