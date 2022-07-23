package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThat
import com.pinterest.ktlint.test.LintViolation
import org.junit.jupiter.api.Test

class TrailingCommaRuleTest {

    private val combinedTrailingCommaAssertThat =
        TrailingCommaOnDeclarationSiteRule()
            .assertThat(
                TrailingCommaOnCallSiteRule(),
                // Apply the IndentationRule always as additional rule, so that the formattedCode in the unit test looks
                // correct.
                IndentationRule()
            )

    // TBS?
    @Test
    fun `Given that properties to force trailing comma's on call and declaration site have been enabled`() {
        val code =
            """
            fun test(
                x: Int,
                y: Int,
                block: (Int, Int) -> Int
            ): (
                Int, Int
            ) -> Int = when (x) {
                1, 2
                -> {
                        foo,
                        bar /* The comma should be inserted before the comment */
                    ->
                    block(
                        foo * bar,
                        foo + bar
                    )
                }
                else -> { _, _ -> block(0, 0) }
            }
            """.trimIndent()
        val formattedCode =
            """
            fun test(
                x: Int,
                y: Int,
                block: (Int, Int) -> Int,
            ): (
                Int, Int,
            ) -> Int = when (x) {
                1, 2,
                -> {
                        foo,
                        bar, /* The comma should be inserted before the comment */
                    ->
                    block(
                        foo * bar,
                        foo + bar,
                    )
                }
                else -> { _, _ -> block(0, 0) }
            }
            """.trimIndent()
        combinedTrailingCommaAssertThat(code)
            .withEditorConfigOverride(TrailingCommaOnCallSiteRule.allowTrailingCommaOnCallSiteProperty to true)
            .withEditorConfigOverride(TrailingCommaOnDeclarationSiteRule.allowTrailingCommaProperty to true)
            .hasLintViolations(
                LintViolation(4, 29, "Missing trailing comma before \")\""),
                LintViolation(6, 13, "Missing trailing comma before \")\""),
                LintViolation(8, 9, "Missing trailing comma before \"->\""),
                LintViolation(11, 16, "Missing trailing comma before \"->\"")
            )
            .hasLintViolationsForAdditionalRules(
                LintViolation(15, 22, "Missing trailing comma before \")\"")
            )
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given that a trailing comma is required on call site and declaration site then still it should not be added to the setter`() {
        val code =
            """
            class Test {
                var foo = Bar()
                    set(value) {
                    }
            }
            """.trimIndent()
        combinedTrailingCommaAssertThat(code)
            .withEditorConfigOverride(TrailingCommaOnDeclarationSiteRule.allowTrailingCommaProperty to true)
            .withEditorConfigOverride(TrailingCommaOnCallSiteRule.allowTrailingCommaOnCallSiteProperty to true)
            .hasNoLintViolations()
    }
}
