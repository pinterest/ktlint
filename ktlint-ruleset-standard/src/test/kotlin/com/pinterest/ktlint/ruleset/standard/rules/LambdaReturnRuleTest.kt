package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRule
import org.junit.jupiter.api.Test

class LambdaReturnRuleTest {
    private val lambdaReturnRuleAssertThat = assertThatRule { LambdaReturnRule() }

    @Test
    fun `Given a lambda without return statement then do not emit`() {
        val code =
            """
            val foo = bar { "value" }
            """.trimIndent()
        lambdaReturnRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Given a lambda with single statement return statement then do not emit`() {
        val code =
            """
            val foo = bar { return@foo "value" }
            """.trimIndent()
        val formattedCode =
            """
            val foo = bar { "value" }
            """.trimIndent()
        lambdaReturnRuleAssertThat(code)
            .hasLintViolation(1, 17, "Unnecessary return")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a lambda with multiple statements and an early return then then do not emit on the early return`() {
        val code =
            """
            val foo = bar {
                if (baz()) return@bar "value"

                "value"
            }
            """.trimIndent()
        lambdaReturnRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Given a lambda with multiple statement and an early return then then do not emit`() {
        val code =
            """
            val foo = bar {
                if (baz()) return@bar "value"

                return@bar "value"
            }
            """.trimIndent()
        val formattedCode =
            """
            val foo = bar {
                if (baz()) return@bar "value"

                "value"
            }
            """.trimIndent()
        lambdaReturnRuleAssertThat(code)
            .hasLintViolation(4, 5, "Unnecessary return")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a lambda with an unnecessary return statement and some comments then remove the return but keep the comments`() {
        val code =
            """
            val foo = bar {
                // comment-1
                return@foo /* comment-2 */ "value" // comment-3
                /* comment-4 */
            }
            """.trimIndent()
        val formattedCode =
            """
            val foo = bar {
                // comment-1
                /* comment-2 */ "value" // comment-3
                /* comment-4 */
            }
            """.trimIndent()
        lambdaReturnRuleAssertThat(code)
            .hasLintViolation(3, 5, "Unnecessary return")
            .isFormattedAs(formattedCode)
    }
}
