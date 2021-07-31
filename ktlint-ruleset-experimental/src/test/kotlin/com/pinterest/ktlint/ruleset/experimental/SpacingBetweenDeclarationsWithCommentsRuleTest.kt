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

    @Test
    fun `not a declaration comment`() {
        assertThat(
            SpacingBetweenDeclarationsWithCommentsRule().lint(
                """
                fun function() {
                    with(binding) {
                        loginButton.setOnClickListener {
                //            comment
                            showScreen()
                        }
                    }
                }
                """.trimIndent()
            )
        ).isEmpty()
    }

    @Test
    fun `not a comment between declarations`() {
        assertThat(
            SpacingBetweenDeclarationsWithCommentsRule().lint(
                """
                class C {
                    fun test() {
                        println()
                        // comment1
                        fun one() = 1
                    }
                }
                """.trimIndent()
            )
        ).isEmpty()
    }

    @Test
    fun `declarations in class`() {
        val actual = SpacingBetweenDeclarationsWithCommentsRule().format(
            """
            class C {
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
            }
            """.trimIndent()
        )
        assertThat(actual).isEqualTo(
            """
            class C {
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
            }
            """.trimIndent()
        )
    }

    // https://github.com/pinterest/ktlint/issues/1053
    @Test
    fun `declaration has tail comments`() {
        assertThat(
            SpacingBetweenDeclarationsWithCommentsRule().lint(
                """
                class SampleClass {

                    private val public = "ok" // ok
                    private val private = "not_ok" // false positive
                    private val halfPublicHalfPrivate = "not_ok" // false positive

                    /**
                     * Doc 1
                     */
                    fun one() = 1

                    /**
                     * Doc 2
                     */
                    fun two() = 2
                    fun three() {
                        val public = "ok" // ok
                        val private = "not_ok" // false positive
                    }
                }

                enum class SampleEnum {
                    One, // ok
                    Two, // false positive
                    Three, // false positive
                }
                """.trimIndent()
            )
        ).isEmpty()
    }
}
