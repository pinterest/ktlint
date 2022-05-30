package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThat
import org.junit.jupiter.api.Test

class SpacingBetweenDeclarationsWithCommentsRuleTest {
    private val spacingBetweenDeclarationsWithCommentsRuleAssertThat = SpacingBetweenDeclarationsWithCommentsRule().assertThat()

    @Test
    fun `Given an EOL comment at top of file should do nothing`() {
        val code =
            """
            // foo
            fun foo()
            """.trimIndent()
        spacingBetweenDeclarationsWithCommentsRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Given a block comment at top of file should do nothing`() {
        val code =
            """
            /*
             * foo
             */
            fun foo()
            """.trimIndent()
        spacingBetweenDeclarationsWithCommentsRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Given a KDoc at top of file should do nothing`() {
        val code =
            """
            /**
             * foo
             */
            fun foo()
            """.trimIndent()
        spacingBetweenDeclarationsWithCommentsRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Given multiple EOL comments should do nothing`() {
        val code =
            """
            // boo
            // foo
            fun foo()
            """.trimIndent()
        spacingBetweenDeclarationsWithCommentsRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Issue 946 - Given EOL comments not above a declaration should do nothing`() {
        val code =
            """
            fun foo() {
                // some comment 1
                bar()
                /*
                 * some comment 2
                 */
                bar()
            }
            """.trimIndent()
        spacingBetweenDeclarationsWithCommentsRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Issue 946 - Given EOL comment above declarations then precede the comment above the second and later declarations with a blank line`() {
        val code =
            """
            class C {
                fun test() {
                    // comment 1
                    fun bar1() = 1
                    /*
                     * comment 2
                     */
                    fun bar2() = 2
                }
            }
            """.trimIndent()
        val formattedCode =
            """
            class C {
                fun test() {
                    // comment 1
                    fun bar1() = 1

                    /*
                     * comment 2
                     */
                    fun bar2() = 2
                }
            }
            """.trimIndent()
        spacingBetweenDeclarationsWithCommentsRuleAssertThat(code)
            .hasLintViolation(5, 9, "Declarations and declarations with comments should have an empty space between.")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Issue 1053 - Given declarations with tail comments should not force blank lines when next line contains a declaration`() {
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
        spacingBetweenDeclarationsWithCommentsRuleAssertThat(code).hasNoLintViolations()
    }
}
