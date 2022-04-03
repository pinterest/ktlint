package com.pinterest.ktlint.test

import com.pinterest.ktlint.core.LintError
import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.api.FeatureInAlphaState
import com.pinterest.ktlint.core.api.UsesEditorConfigProperties
import org.assertj.core.api.AbstractAssert
import org.assertj.core.api.Assertions.assertThat

/**
 * AssertJ style assertion for verifying KtLint rules. This class is intended to be used as follows:
 *
 * ```
 * // Create an assertThat assertion for a specific rule or set of rules. If possible define it at the class level
 * val someRuleAssertThat = SomeRule().assertThat()
 *
 * @Test
 * fun `Some test`() {
 *     val code = .... // Original code to be formatted
 *     val formattedCode = .... // The code as it should be formatted
 *     someRuleAssertThat(code)
 *         .hasLintErrors(
 *             LintError(1, 1, "some-rule-id", "some-error-description")
 *         ).isFormattedAs(formattedCode)
 * }
 * ```
 */
@OptIn(FeatureInAlphaState::class)
@Suppress("MemberVisibilityCanBePrivate")
public class KtLintAssertThat(
    private val rules: List<Rule>,
    private val code: String
) : AbstractAssert<KtLintAssertThat, String>(code, KtLintAssertThat::class.java) {
    private var asKotlinScript = false
    private var editorConfigOverride = EditorConfigOverride.emptyEditorConfigOverride

    /**
     * Set the [EditorConfigOverride] properties to be used by the rule. The properties can only be set once. This
     * function can be chained.
     */
    public fun withEditorConfigOverride(
        vararg properties: Pair<UsesEditorConfigProperties.EditorConfigProperty<*>, *>
    ): KtLintAssertThat {
        check(editorConfigOverride == EditorConfigOverride.emptyEditorConfigOverride) {
            "The EditorConfigOverride Properties may not be altered once they have been set."
        }

        editorConfigOverride = EditorConfigOverride.from(*properties)

        return this
    }

    /**
     * Handle the code as if it was specified as a Kotlin script ("kts" file). This function can be chained.
     */
    public fun asKotlinScript(asKotlinScript: Boolean = true): KtLintAssertThat {
        this.asKotlinScript = asKotlinScript
        return this
    }

    /**
     * Asserts that the code does not contain any lint errors. This is a sugar coated version of
     * [hasNoLintErrorsBeforeFormatting]. This function can not be chained.
     *
     * Note: When linting succeeds without errors, formatting is also checked.
     */
    public fun hasNoLintErrors() {
        hasNoLintErrorsBeforeFormatting()

        // Also format the code to be absolutely sure that codes does not get changed
        val actualFormattedCode = rules.format(code, editorConfigOverride)

        assertThat(actualFormattedCode)
            .describedAs("Code is changed by format while no lint errors were found")
            .isEqualTo(code)
    }

    /**
     * Asserts that the code does not contain any lint errors before the code is actually formatted.
     */
    public fun hasNoLintErrorsBeforeFormatting() {
        val actualLintErrors = rules.lint(code, editorConfigOverride)

        assertThat(actualLintErrors).isEmpty()
    }

    /**
     * Asserts that the code does contain given lint errors. This is a sugar coated version of
     * [hasNoLintErrorsBeforeFormatting]. This method can be chained with the [isFormattedAs] and/or
     * [hasLintErrorsAfterFormatting].
     */
    public fun hasLintErrors(vararg expectedLintErrors: LintError): KtLintAssertThat =
        hasLintErrorsBeforeFormatting(*expectedLintErrors)

    /**
     * Asserts that the code does not contain any lint errors before the code is actually formatted. This method can be
     * chained with the [isFormattedAs] and/or [hasLintErrorsAfterFormatting].
     */
    public fun hasLintErrorsBeforeFormatting(vararg expectedLintErrors: LintError): KtLintAssertThat {
        check(expectedLintErrors.isNotEmpty())

        val actualLintErrors = rules.lint(code, editorConfigOverride)

        assertThat(actualLintErrors)
            .describedAs("Lint errors before formatting")
            .containsExactlyInAnyOrder(*expectedLintErrors)

        return this
    }

    /**
     * Asserts that the code is formatted as given. This method can be chained with the [hasLintErrorsAfterFormatting].
     */
    public fun isFormattedAs(formattedCode: String): KtLintAssertThat {
        check(formattedCode != code) {
            "Use '.hasNoLintErrors()' instead of '.isFormattedAs(<original code>)'"
        }

        val actualFormattedCode = rules.format(code, editorConfigOverride)

        assertThat(actualFormattedCode)
            .describedAs("Code is formatted as")
            .isEqualTo(formattedCode)

        return this
    }

    /**
     * Asserts that the code does not contain any lint errors after the code has been formatted.
     */
    public fun hasNoLintErrorsAfterFormatting() {
        val formattedCode = rules.format(code, editorConfigOverride)

        val actualLintErrors = rules.lint(formattedCode, editorConfigOverride)

        assertThat(actualLintErrors).isEmpty()
    }

    /**
     * Asserts that the code does contain the given lint errors after the code has been formatted.
     */
    public fun hasLintErrorsAfterFormatting(vararg expectedLintErrors: LintError) {
        check(expectedLintErrors.isNotEmpty())

        val formattedCode = rules.format(code, editorConfigOverride)

        val actualLintErrors = rules.lint(formattedCode, editorConfigOverride)

        assertThat(actualLintErrors)
            .describedAs("Lint errors after formatting")
            .containsExactlyInAnyOrder(*expectedLintErrors)
    }

    public companion object {
        /**
         * Creates an assertThat assertion function for a given rule. This assertion function has extensions
         * specifically for testing KtLint rules.
         */
        public fun Rule.assertThat(): (String) -> KtLintAssertThat = listOf(this).assertThat()

        /**
         * Creates an assertThat assertion function for a list of given rules. This assertion function has extensions
         * specifically for testing KtLint rules.
         */
        public fun List<Rule>.assertThat(): (String) -> KtLintAssertThat =
            { code -> KtLintAssertThat(this, code) }
    }
}
