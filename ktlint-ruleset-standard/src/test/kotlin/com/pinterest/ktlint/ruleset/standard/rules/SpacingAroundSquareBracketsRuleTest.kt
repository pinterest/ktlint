package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRule
import com.pinterest.ktlint.test.LintViolation
import org.junit.jupiter.api.Test

class SpacingAroundSquareBracketsRuleTest {
    private val spacingAroundSquareBracketsRuleAssertThat = assertThatRule { SpacingAroundSquareBracketsRule() }

    @Test
    fun `Given array access expression`() {
        val code =
            """
            val foo = bar[1]
            """.trimIndent()
        spacingAroundSquareBracketsRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Given array access expression with unexpected spacing before LBRACKET`() {
        val code =
            """
            val foo = bar [1]
            """.trimIndent()
        val formattedCode =
            """
            val foo = bar[1]
            """.trimIndent()
        spacingAroundSquareBracketsRuleAssertThat(code)
            .hasLintViolation(1, 14, "Unexpected spacing before '['")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a KDoc with white space before LBRACKET then do not emit`() {
        val code =
            """
            /**
             * @See [Foo] for more information.
             */
            fun foo() {}
            """.trimIndent()
        spacingAroundSquareBracketsRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Given array access expression with unexpected spacing after LBRACKET`() {
        val code =
            """
            val foo = bar[ 1]
            """.trimIndent()
        val formattedCode =
            """
            val foo = bar[1]
            """.trimIndent()
        spacingAroundSquareBracketsRuleAssertThat(code)
            .hasLintViolation(1, 15, "Unexpected spacing after '['")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given array access expression with unexpected spacing around LBRACKET`() {
        val code =
            """
            val foo = bar [ 1]
            """.trimIndent()
        val formattedCode =
            """
            val foo = bar[1]
            """.trimIndent()
        spacingAroundSquareBracketsRuleAssertThat(code)
            .hasLintViolation(1, 15, "Unexpected spacing around '['")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given array access expression with unexpected spacing before RBRACKET`() {
        val code =
            """
            val foo = bar[1 ]
            """.trimIndent()
        val formattedCode =
            """
            val foo = bar[1]
            """.trimIndent()
        spacingAroundSquareBracketsRuleAssertThat(code)
            .hasLintViolation(1, 16, "Unexpected spacing before ']'")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a multiline array access expression then do not emit`() {
        val code =
            """
            val foo = bar[
                1,
                baz
            ]
            """.trimIndent()
        spacingAroundSquareBracketsRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Given array access expression with whitespace after RBRACKET then do not emit`() {
        val code =
            """
            val foo1 = bar[1]
            val foo2 = bar[1] + bar[2]
            val foo3 = bar[1].count()
            """.trimIndent()
        spacingAroundSquareBracketsRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Given an annotation with a collection literal expression with whitespaces before around LBRACKET and RBRACKET then do not emit`() {
        val code =
            """
            @Foo(
                fooBar = ["foo", "bar"],
                fooBaz = [
                    "foo",
                    "baz",
                ],
            )
            fun foo() {}
            """.trimIndent()
        spacingAroundSquareBracketsRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Issue 2675 - operator get with lambda`() {
        val code =
            """
            val foo = bar[ { 123 } ]
            """.trimIndent()

        val formattedCode =
            """
            val foo = bar[{ 123 }]
            """.trimIndent()

        spacingAroundSquareBracketsRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(line = 1, col = 15, detail = "Unexpected spacing after '['", canBeAutoCorrected = true),
                LintViolation(line = 1, col = 23, detail = "Unexpected spacing before ']'", canBeAutoCorrected = true),
            ).isFormattedAs(formattedCode)
    }
}
