package io.github.ktlint.core.ruleset.standard.rules

import io.github.ktlint.core.test.KtLintAssertThat.Companion.assertThatRule
import io.github.ktlint.core.test.KtlintDocumentationTest
import io.github.ktlint.core.test.LintViolation
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
            val foo = bar { return@bar "value" }
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
                return@bar /* comment-2 */ "value" // comment-3
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

    @KtlintDocumentationTest
    fun `Given some nested lambda arguments then only remove return when it refers to the inner most lambda`() {
        val code =
            $$"""
            val foo =
                "Foo"
                    .let outer@{ foo ->
                        if (foo != "Foo") {
                            return@outer "$foo is not expected input"
                        }
                        foo
                            .let { return@let "$it was a" }
                            .let secondLet@{
                                if (foo != "Foo") {
                                    return@secondLet "$foo is not expected input"
                                }
                                return@secondLet "$it success"
                            }.let { return@outer "$it (map after let)" }
                        // This is unreachable code due to "return@outer" in let above.
                        return@outer "$foo was a failure"
                    }.let { return@let "$it (let after also)" }
            """.trimIndent()
        val formattedCode =
            $$"""
            val foo =
                "Foo"
                    .let outer@{ foo ->
                        if (foo != "Foo") {
                            return@outer "$foo is not expected input"
                        }
                        foo
                            .let { "$it was a" }
                            .let secondLet@{
                                if (foo != "Foo") {
                                    return@secondLet "$foo is not expected input"
                                }
                                "$it success"
                            }.let { return@outer "$it (map after let)" }
                        // This is unreachable code due to "return@outer" in let above.
                        "$foo was a failure"
                    }.let { "$it (let after also)" }
            """.trimIndent()
        lambdaReturnRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(8, 24, "Unnecessary return"),
                LintViolation(13, 21, "Unnecessary return"),
                LintViolation(16, 13, "Unnecessary return"),
                LintViolation(17, 17, "Unnecessary return"),
            ).isFormattedAs(formattedCode)
    }
}
