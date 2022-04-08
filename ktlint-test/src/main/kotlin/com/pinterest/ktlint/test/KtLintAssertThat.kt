package com.pinterest.ktlint.test

import com.pinterest.ktlint.core.LintError
import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.api.DefaultEditorConfigProperties.maxLineLengthProperty
import com.pinterest.ktlint.core.api.EditorConfigOverride
import com.pinterest.ktlint.core.api.FeatureInAlphaState
import com.pinterest.ktlint.core.api.UsesEditorConfigProperties
import com.pinterest.ktlint.test.KtLintAssertThat.Companion.EOL_CHAR
import com.pinterest.ktlint.test.KtLintAssertThat.Companion.MAX_LINE_LENGTH_MARKER
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
    private var filePath: String? = null
    private var editorConfigProperties = emptySet<Pair<UsesEditorConfigProperties.EditorConfigProperty<*>, *>>()
    private var editorConfigOverride = EditorConfigOverride.emptyEditorConfigOverride
        get() =
            if (editorConfigProperties.isEmpty()) {
                EditorConfigOverride.emptyEditorConfigOverride
            } else {
                EditorConfigOverride.from(*editorConfigProperties.toTypedArray())
            }

    /**
     * Set the [EditorConfigOverride] properties to be used by the rule. This function can be called multiple times.
     * Properties which have been set before, are silently overwritten with the new vale. This function can be chained.
     */
    public fun withEditorConfigOverride(
        vararg properties: Pair<UsesEditorConfigProperties.EditorConfigProperty<*>, *>
    ): KtLintAssertThat {
        editorConfigProperties = editorConfigProperties + properties.toSet()

        return this
    }

    /**
     * Set the [EditorConfigOverride] "max_line_length"property based on the EOL Marker which is places at the first
     * line of the code sample. If the property has been set before via [withEditorConfigOverride] then that value is
     * silently overwritten. This function can be chained.
     *
     * Example of usage:
     * ```
     *  val code =
     *      """
     *      // $MAX_LINE_LENGTH_MARKER                   $EOL_CHAR
     *      val fooooooooooooooo = "fooooooooooooooooooooo"
     *      """.trimIndent()
     *  maxLineLengthRuleAssertThat(code)
     *      .setMaxLineLength()
     *      .hasLintErrors(
     *          LintError(2, 1, "max-line-length", "Exceeded max line length (46)")
     *      )
     * ```
     */
    @Throws(MissingEolMarker::class)
    public fun setMaxLineLength(): KtLintAssertThat {
        code
            .split("\n")
            .firstOrNull { it.contains(MAX_LINE_LENGTH_MARKER) && it.endsWith(EOL_CHAR) }
            ?.indexOf(EOL_CHAR)
            ?.let { index ->
                editorConfigProperties =
                    editorConfigProperties + setOf(maxLineLengthProperty to (index + 1).toString())
            } ?: throw MissingEolMarker()

        return this
    }

    /**
     * Handle the code as if it was specified in file on the given path. This function can be chained.
     */
    public fun asFileWithPath(filePath: String): KtLintAssertThat {
        this.filePath = filePath
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
        val actualFormattedCode = rules.format(
            lintedFilePath = filePath,
            text = code,
            editorConfigOverride = editorConfigOverride
        )

        assertThat(actualFormattedCode)
            .describedAs("Code is changed by format while no lint errors were found")
            .isEqualTo(code)
    }

    /**
     * Asserts that the code does not contain any lint errors before the code is actually formatted.
     */
    public fun hasNoLintErrorsBeforeFormatting() {
        val actualLintErrors = rules.lint(
            lintedFilePath = filePath,
            text = code,
            editorConfigOverride = editorConfigOverride
        )

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

        val actualLintErrors = rules.lint(
            lintedFilePath = filePath,
            text = code,
            editorConfigOverride = editorConfigOverride
        )

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

        /**
         * See [setMaxLineLength] for intended usage.
         */
        public const val MAX_LINE_LENGTH_MARKER: String = "Max line length marker:" // Keep length of constant name same as length of value

        /**
         * See [setMaxLineLength] for intended usage.
         */
        public const val EOL_CHAR: Char = '#'
    }
}

internal class MissingEolMarker : RuntimeException(
    """
    The first line of the provide code sample should contain text '$MAX_LINE_LENGTH_MARKER' which is provided by the
    constant '${::MAX_LINE_LENGTH_MARKER.name}' and ends with the EOL_CHAR'$EOL_CHAR' provided by the constant
    '${::EOL_CHAR.name}' which indicates the last position that is allowed.
    """.trimIndent()
)
