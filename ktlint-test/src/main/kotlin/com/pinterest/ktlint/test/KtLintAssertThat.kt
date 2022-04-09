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
 *         .hasLintViolations(
 *             LintViolation(1, 1, "some-error-description")
 *         ).isFormattedAs(formattedCode)
 * }
 * ```
 */
@OptIn(FeatureInAlphaState::class)
@Suppress("MemberVisibilityCanBePrivate")
public class KtLintAssertThat(
    private val rules: List<Rule>,
    private val code: String
) {
    private var filePath: String? = null
    private var kotlinScript = false
    private var editorConfigProperties = emptySet<Pair<UsesEditorConfigProperties.EditorConfigProperty<*>, *>>()

    /**
     * Set the [EditorConfigOverride] properties to be used by the rule. This function can be called multiple times.
     * Properties which have been set before, are silently overwritten with the new vale.
     */
    public fun withEditorConfigOverride(
        vararg properties: Pair<UsesEditorConfigProperties.EditorConfigProperty<*>, *>
    ): KtLintAssertThat {
        editorConfigProperties = editorConfigProperties + properties.toSet()

        return this
    }

    /**
     * Set the [EditorConfigOverride] "max_line_length" property based on the EOL Marker which is places at the first
     * line of the code sample. If the property has been set before via [withEditorConfigOverride] then that value is
     * silently overwritten.
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
     *      .hasLintViolations(
     *          LintViolation(2, 1, "Exceeded max line length (46)")
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
     * Handle the code as if it was specified in file on the given path.
     */
    public fun asFileWithPath(filePath: String): KtLintAssertThat {
        this.filePath = filePath
        return this
    }

    /**
     * Handle the code as Kotlin script (kts).
     */
    public fun asKotlinScript(): KtLintAssertThat {
        this.kotlinScript = true
        return this
    }

    /**
     * Asserts that the code does not contain any [LintViolation]s. This is a sugar coated version of
     * [hasNoLintViolationsBeforeFormatting].
     *
     * Note: When linting succeeds without errors, formatting is also checked.
     */
    public fun hasNoLintViolations(): Unit = ktLintAssertThatAssertable().hasNoLintViolations()

    /**
     * Asserts that the code does not contain any [LintViolation]s before the code is actually formatted.
     */
    public fun hasNoLintViolationsBeforeFormatting(): Unit = ktLintAssertThatAssertable().hasNoLintViolationsBeforeFormatting()

    /**
     * Asserts that the code does contain given [LintViolation]s. This is a sugar coated version of
     * [hasNoLintViolationsBeforeFormatting].
     */
    public fun hasLintViolations(vararg expectedErrors: LintViolation): KtLintAssertThatAssertable =
        ktLintAssertThatAssertable().hasLintViolationsBeforeFormatting(*expectedErrors)

    /**
     * Asserts that the code does not contain any [LintViolation]s before the code is actually formatted.
     */
    public fun hasLintViolationsBeforeFormatting(vararg expectedErrors: LintViolation): KtLintAssertThatAssertable =
        ktLintAssertThatAssertable().hasLintViolationsBeforeFormatting(*expectedErrors)

    /**
     * Asserts that the code is formatted as given.
     */
    public fun isFormattedAs(formattedCode: String): KtLintAssertThatAssertable =
        ktLintAssertThatAssertable().isFormattedAs(formattedCode)

    /**
     * Asserts that the code does not contain any [LintViolation]s after the code has been formatted.
     */
    public fun hasNoLintViolationsAfterFormatting(): Unit =
        ktLintAssertThatAssertable().hasNoLintViolationsAfterFormatting()

    /**
     * Asserts that the code does contain the given [LintViolation]s after the code has been formatted.
     */
    public fun hasLintViolationsAfterFormatting(vararg expectedErrors: LintViolation): Unit =
        ktLintAssertThatAssertable().hasLintViolationsAfterFormatting(*expectedErrors)

    private fun ktLintAssertThatAssertable(): KtLintAssertThatAssertable =
        if (editorConfigProperties.isEmpty()) {
            KtLintAssertThatAssertable(
                rules = rules,
                code = code,
                filePath = filePath,
                kotlinScript = kotlinScript,
                editorConfigOverride = EditorConfigOverride.emptyEditorConfigOverride
            )
        } else {
            KtLintAssertThatAssertable(
                rules = rules,
                code = code,
                filePath = filePath,
                kotlinScript = kotlinScript,
                editorConfigOverride = EditorConfigOverride.from(*editorConfigProperties.toTypedArray())
            )
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

/**
 * Immutable assertable. Once the first assertion is made on [KtLintAssertThat] it is converted to the
 * [KtLintAssertThatAssertable] which allows no further modifications of the internal state. This guarantees that all
 * assertions operate on the same state.
 */
@OptIn(FeatureInAlphaState::class)
public class KtLintAssertThatAssertable(
    private val rules: List<Rule>,
    private val code: String,
    private val filePath: String?,
    private val kotlinScript: Boolean,
    private val editorConfigOverride: EditorConfigOverride = EditorConfigOverride.emptyEditorConfigOverride
) : AbstractAssert<KtLintAssertThatAssertable, String>(code, KtLintAssertThatAssertable::class.java) {
    /**
     * Asserts that the code does not contain any [LintViolation]s. This is a sugar coated version of
     * [hasNoLintViolationsBeforeFormatting].
     *
     * Note: When linting succeeds without errors, formatting is also checked.
     */
    public fun hasNoLintViolations() {
        hasNoLintViolationsBeforeFormatting()

        // Also format the code to be absolutely sure that codes does not get changed
        val actualFormattedCode = format()

        assertThat(actualFormattedCode)
            .describedAs("Code is changed by format while no lint errors were found")
            .isEqualTo(code)
    }

    /**
     * Asserts that the code does not contain any [LintViolation]s before the code is actually formatted.
     */
    public fun hasNoLintViolationsBeforeFormatting() {
        val actualLintErrors = lint()

        assertThat(actualLintErrors).isEmpty()
    }

    /**
     * Asserts that the code does contain given [LintViolation]s. This is a sugar coated version of
     * [hasNoLintViolationsBeforeFormatting].
     */
    public fun hasLintViolations(vararg expectedErrors: LintViolation): KtLintAssertThatAssertable =
        hasLintViolationsBeforeFormatting(*expectedErrors)

    /**
     * Asserts that the code does not contain any [LintViolation]s before the code is actually formatted.
     */
    public fun hasLintViolationsBeforeFormatting(vararg expectedErrors: LintViolation): KtLintAssertThatAssertable {
        check(expectedErrors.isNotEmpty())

        val actualLintErrors = lint()

        assertThat(actualLintErrors)
            .describedAs("Lint errors before formatting")
            .containsExactlyInAnyOrder(*expectedErrors.toLintErrors())

        return this
    }

    /**
     * Asserts that the code is formatted as given.
     */
    public fun isFormattedAs(formattedCode: String): KtLintAssertThatAssertable {
        check(formattedCode != code) {
            "Use '.hasNoLintErrors()' instead of '.isFormattedAs(<original code>)'"
        }

        val actualFormattedCode = format()

        assertThat(actualFormattedCode)
            .describedAs("Code is formatted as")
            .isEqualTo(formattedCode)

        return this
    }

    /**
     * Asserts that the code does not contain any [LintViolation]s after the code has been formatted.
     */
    public fun hasNoLintViolationsAfterFormatting() {
        val actualLintErrors = lint(runFormatBeforeLint = true)

        assertThat(actualLintErrors).isEmpty()
    }

    /**
     * Asserts that the code does contain the given [LintViolation]s after the code has been formatted.
     */
    public fun hasLintViolationsAfterFormatting(vararg expectedErrors: LintViolation) {
        check(expectedErrors.isNotEmpty())

        val actualLintErrors = lint(runFormatBeforeLint = true)

        assertThat(actualLintErrors)
            .describedAs("Lint errors after formatting")
            .containsExactlyInAnyOrder(*expectedErrors.toLintErrors())
    }

    private fun Array<out LintViolation>.toLintErrors(): Array<LintError> {
        if (rules.count() == 1) {
            check(any { it.ruleId == null }) {
                "Do not specify the rule id in the LintViolation whenever exactly one rule is asserted."
            }
        } else {
            check(none { it.ruleId == null }) {
                "Whenever multiple rules are being asserted, the rule id of each LintViolation has to be specified."
            }
        }
        return map {
            LintError(
                line = it.line,
                col = it.col,
                ruleId = it.ruleId ?: rules.first().id,
                detail = it.detail,
                canBeAutoCorrected = it.canBeAutoCorrected
            )
        }.toTypedArray()
    }

    private fun lint(runFormatBeforeLint: Boolean = false) =
        rules.lint(
            lintedFilePath = filePath,
            script = kotlinScript,
            text = if (runFormatBeforeLint) {
                format()
            } else {
                code
            },
            editorConfigOverride = editorConfigOverride
        )

    private fun format() =
        rules.format(
            lintedFilePath = filePath,
            script = kotlinScript,
            text = code,
            editorConfigOverride = editorConfigOverride
        )
}

internal class MissingEolMarker : RuntimeException(
    """
    The first line of the provide code sample should contain text '$MAX_LINE_LENGTH_MARKER' which is provided by the
    constant '${::MAX_LINE_LENGTH_MARKER.name}' and ends with the EOL_CHAR'$EOL_CHAR' provided by the constant
    '${::EOL_CHAR.name}' which indicates the last position that is allowed.
    """.trimIndent()
)

/**
 * Expectation of the [LintError]. Contrary to the [LintError] the ruleId does not need to be specified in the
 * [LintViolation] whenever only one rule is to be asserted.
 */
public data class LintViolation(
    val line: Int,
    val col: Int,
    val detail: String,
    /**
     * To improve readability and maintainability of the tests, the ruleId should only be specified in case multiple
     * rules are to be asserted.
     */
    val ruleId: String? = null,
    val canBeAutoCorrected: Boolean = true
)
