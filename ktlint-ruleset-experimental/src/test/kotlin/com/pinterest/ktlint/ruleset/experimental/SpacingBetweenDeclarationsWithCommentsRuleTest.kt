package com.pinterest.ktlint.ruleset.experimental

import com.pinterest.ktlint.core.LintError
import com.pinterest.ktlint.test.format
import com.pinterest.ktlint.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

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
        val code =
            """
            fun a()
            /**
             * foo
             */
            fun b()
            """.trimIndent()
        assertThat(
            SpacingBetweenDeclarationsWithCommentsRule().lint(code)
        ).containsExactly(
            LintError(2, 1, "spacing-between-declarations-with-comments", "Declarations and declarations with comments should have an empty space between.")
        )
    }

    @Test
    fun `missing space before declaration with eol comment should cause error`() {
        val code =
            """
            fun a()
            // foo
            fun b()
            """.trimIndent()
        assertThat(
            SpacingBetweenDeclarationsWithCommentsRule().lint(code)
        ).containsExactly(
            LintError(2, 1, "spacing-between-declarations-with-comments", "Declarations and declarations with comments should have an empty space between.")
        )
    }

    @Test
    fun `autoformat should work correctly`() {
        val code =
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
        val formattedCode =
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
        assertThat(
            SpacingBetweenDeclarationsWithCommentsRule().format(code)
        ).isEqualTo(formattedCode)
    }

    @Test
    fun `not a declaration comment`() {
        val code =
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
        assertThat(
            SpacingBetweenDeclarationsWithCommentsRule().lint(code)
        ).isEmpty()
    }

    @Test
    fun `not a comment between declarations`() {
        val code =
            """
            class C {
                fun test() {
                    println()
                    // comment1
                    fun one() = 1
                }
            }
            """.trimIndent()
        assertThat(
            SpacingBetweenDeclarationsWithCommentsRule().lint(code)
        ).isEmpty()
    }

    @Test
    fun `declarations in class`() {
        val code =
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
        val formattedCode =
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
        assertThat(
            SpacingBetweenDeclarationsWithCommentsRule().format(code)
        ).isEqualTo(formattedCode)
    }

    // https://github.com/pinterest/ktlint/issues/1053
    @Test
    fun `declaration has tail comments`() {
        val code =
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
        assertThat(
            SpacingBetweenDeclarationsWithCommentsRule().lint(code)
        ).isEmpty()
    }
}
