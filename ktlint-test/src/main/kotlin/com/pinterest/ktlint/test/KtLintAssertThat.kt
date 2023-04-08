package com.pinterest.ktlint.test

import com.pinterest.ktlint.logger.api.initKtLintKLogger
import com.pinterest.ktlint.logger.api.setDefaultLoggerModifier
import com.pinterest.ktlint.rule.engine.api.Code
import com.pinterest.ktlint.rule.engine.api.EditorConfigOverride
import com.pinterest.ktlint.rule.engine.api.EditorConfigOverride.Companion.EMPTY_EDITOR_CONFIG_OVERRIDE
import com.pinterest.ktlint.rule.engine.api.EditorConfigOverride.Companion.plus
import com.pinterest.ktlint.rule.engine.api.KtLintRuleEngine
import com.pinterest.ktlint.rule.engine.api.LintError
import com.pinterest.ktlint.rule.engine.core.api.Rule
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.RuleProvider
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.EXPERIMENTAL_RULES_EXECUTION_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.EditorConfigProperty
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.MAX_LINE_LENGTH_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.RuleExecution
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.createRuleSetExecutionEditorConfigProperty
import com.pinterest.ktlint.test.KtLintAssertThat.Companion.EOL_CHAR
import com.pinterest.ktlint.test.KtLintAssertThat.Companion.MAX_LINE_LENGTH_MARKER
import mu.KotlinLogging
import org.assertj.core.api.AbstractAssert
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.assertAll
import kotlin.io.path.pathString

// When system and/or environment variables below are enabled, the unit tests provide additional logging information.
private const val KTLINT_UNIT_TEST_TRACE = "KTLINT_UNIT_TEST_TRACE" // Keep value in sync with value in 'logback-test.xml'

@Suppress("unused")
private val LOGGER =
    KotlinLogging
        .logger {}
        .setDefaultLoggerModifier { logger ->
            if (!logger.isTraceEnabled || !logger.isDebugEnabled) {
                logger.info {
                    """
                    Additional information can be printed during running of unit tests, by setting environment variable below:
                        $KTLINT_UNIT_TEST_TRACE=[on|off]
                    """.trimIndent()
                }
            }
        }.initKtLintKLogger()

// The execution of the unit tests may not be affected by the ".editorconfig" configuration of the Ktlint project itself. So each unit test
// is to be run on a KtlintTestFileSystem not having any ".editorconfig" files. This variable should not be exposed, as unit tests should
// not be allowed to write any content on the file system.
// As this FileSystem is not modified it is not considered to be a problem that the file system is not closed after each unit test.
private val KTLINT_TEST_FILE_SYSTEM = KtlintTestFileSystem()

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
 *         .hasLintViolation(1, 1, "some-error-description")
 *         .isFormattedAs(formattedCode)
 * }
 * ```
 */
@Suppress("MemberVisibilityCanBePrivate")
public class KtLintAssertThat(
    /**
     * Provider of a rule which is the subject of the test, e.g. the rule for which the AssertThat is created.
     */
    private val ruleProvider: RuleProvider,
    /**
     * The code which is to be linted and formatted.
     */
    private val code: String,
    /**
     * Providers of rules which have to be executed in addition to the main rule when linting/formatting the code. Note
     * that lint errors for those rules are suppressed.
     */
    private val additionalRuleProviders: MutableSet<RuleProvider>,
    /**
     * EditorConfig properties to be applied by default when linting/formatting the code.
     */
    private val editorConfigProperties: MutableSet<Pair<EditorConfigProperty<*>, *>>,
) {
    private var filePath: String? = null
    private var kotlinScript = false

    /**
     * Set the [EditorConfigOverride] properties to be used by the rule. This function can be called multiple times.
     * Properties which have been set before, are silently overwritten with the new vale.
     */
    public fun withEditorConfigOverride(vararg properties: Pair<EditorConfigProperty<*>, *>): KtLintAssertThat {
        editorConfigProperties.addAll(properties)

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
     *      .hasLintViolation(2, 1, "Exceeded max line length (46)")
     * ```
     */
    @Throws(MissingEolMarker::class)
    public fun setMaxLineLength(): KtLintAssertThat {
        code
            .split("\n")
            .firstOrNull { it.contains(MAX_LINE_LENGTH_MARKER) && it.endsWith(EOL_CHAR) }
            ?.indexOf(EOL_CHAR)
            ?.let { index -> editorConfigProperties.add(MAX_LINE_LENGTH_PROPERTY to (index + 1).toString()) }
            ?: throw MissingEolMarker()

        return this
    }

    /**
     * Handle the code as if it was specified in file on the given path. Only to be used when the [filePath] is absolutely needed. It
     * results in creating a file cause a slight performance decrease of the test.
     */
    public fun asFileWithPath(filePath: String): KtLintAssertThat {
        this.filePath = filePath
        return this
    }

    /**
     * Handle the code as Kotlin script (kts).
     */
    public fun asKotlinScript(kotlinScript: Boolean = true): KtLintAssertThat {
        this.kotlinScript = kotlinScript
        return this
    }

    /**
     * Adds a single provider of an additional rule to be executed when linting/formatting the code. This can to be used
     * to unit test rules which are best to be tested in conjunction with other rules, for example wrapping and
     * indenting.
     *
     * Caution:
     * An additional rule provider for a rule which actually is executed before the rule under test, might result in
     * unexpected [LintViolation] in case that additional rule is modifying the AST. During the [lint] phase, all rules
     * are executed in parallel and as of that both the rule under test and the additional rule are using the exact same
     * version of the AST. When, in the [format] stage the additional rule is run first, and if it modifies the AST,
     * this could result in a [LintViolation] for the rule under test which is executed later than the additional rule.
     *
     * Prefer to use [addAdditionalRuleProviders] when adding multiple providers of rules.
     */
    public fun addAdditionalRuleProvider(provider: () -> Rule): KtLintAssertThat {
        additionalRuleProviders.add(RuleProvider(provider))

        return this
    }

    /**
     * Adds a single provider of an additional rule to be executed when linting/formatting the code. This can to be used
     * to unit test rules which are best to be tested in conjunction with other rules, for example wrapping and
     * indenting.
     *
     * Caution:
     * An additional rule provider for a rule which actually is executed before the rule under test, might result in
     * unexpected [LintViolation] in case that additional rule is modifying the AST. During the [lint] phase, all rules
     * are executed in parallel and as of that both the rule under test and the additional rule are using the exact same
     * version of the AST. When, in the [format] stage the additional rule is run first, and if it modifies the AST,
     * this could result in a [LintViolation] for the rule under test which is executed later than the additional rule.
     *
     * Prefer to use [addAdditionalRuleProvider] when only a singe provider of a rule is to be added.
     */
    public fun addAdditionalRuleProviders(vararg providers: (() -> Rule)): KtLintAssertThat {
        additionalRuleProviders.addAll(
            providers.map { RuleProvider(it) },
        )

        return this
    }

    /**
     * Asserts that the code does not contain any [LintViolation]s in the rule associated with the KtLintAssertThat.
     *
     * Note: When linting succeeds without errors, formatting is also checked.
     */
    public fun hasNoLintViolations(): Unit = ktLintAssertThatAssertable().hasNoLintViolations()

    /**
     * Asserts that the code does not contain any [LintViolation]s for a given rule id.
     */
    public fun hasNoLintViolationsForRuleId(ruleId: RuleId): KtLintAssertThatAssertable =
        ktLintAssertThatAssertable().hasNoLintViolationsForRuleId(ruleId)

    /**
     * Asserts that the code does not contain any [LintViolation]s except in the additional formatting rules.
     *
     * Note: When linting succeeds without errors, formatting is also checked.
     */
    public fun hasNoLintViolationsExceptInAdditionalRules(): KtLintAssertThatAssertable =
        ktLintAssertThatAssertable().hasNoLintViolationsExceptInAdditionalRules()

    /**
     * Asserts that the code does contain given [LintViolation] which automatically can be corrected. This is a sugar
     * coated version of [hasLintViolations] for the case that the code contains exactly one lint violation.
     */
    public fun hasLintViolation(
        line: Int,
        col: Int,
        detail: String,
    ): KtLintAssertThatAssertable = ktLintAssertThatAssertable().hasLintViolation(line, col, detail)

    /**
     * Asserts that the code does contain given [LintViolation]s which can be automatically corrected. Note that tests
     * resulting in only one (type of) [LintViolation] are usually easier to comprehend.
     */
    public fun hasLintViolations(vararg expectedErrors: LintViolation): KtLintAssertThatAssertable =
        ktLintAssertThatAssertable().hasLintViolations(*expectedErrors)

    /**
     * Asserts that the code does contain given [LintViolation] caused by an additional rule which automatically can be
     * corrected. This is a sugar coated version of [hasLintViolations] for the case that the code contains exactly one
     * lint violation.
     */
    public fun hasLintViolationForAdditionalRule(
        line: Int,
        col: Int,
        detail: String,
    ): KtLintAssertThatAssertable = ktLintAssertThatAssertable().hasLintViolationForAdditionalRule(line, col, detail)

    /**
     * Asserts that the code does contain given [LintViolation]s caused by an additional rules which can be
     * automatically corrected. Note that tests resulting in only one (type of) [LintViolation] are usually easier to
     * comprehend.
     */
    public fun hasLintViolationsForAdditionalRule(vararg expectedErrors: LintViolation): KtLintAssertThatAssertable =
        ktLintAssertThatAssertable().hasLintViolationsForAdditionalRules(*expectedErrors)

    /**
     * Asserts that the code is formatted as given.
     */
    public fun isFormattedAs(formattedCode: String): KtLintAssertThatAssertable = ktLintAssertThatAssertable().isFormattedAs(formattedCode)

    /**
     * Asserts that the code does contain the given [LintViolation] which can not be automatically corrected.
     */
    public fun hasLintViolationWithoutAutoCorrect(
        line: Int,
        col: Int,
        detail: String,
    ): Unit = ktLintAssertThatAssertable().hasLintViolationWithoutAutoCorrect(line, col, detail)

    /**
     * Asserts that the code does contain the given [LintViolation]s which can not be automatically corrected. Note that
     * tests resulting in only one [LintViolation] are usually easier to comprehend.
     */
    public fun hasLintViolationsWithoutAutoCorrect(vararg expectedErrors: LintViolation): Unit =
        ktLintAssertThatAssertable().hasLintViolationsWithoutAutocorrect(*expectedErrors)

    private fun ktLintAssertThatAssertable(): KtLintAssertThatAssertable =
        KtLintAssertThatAssertable(
            ruleProvider = ruleProvider,
            code = code(),
            editorConfigOverride = editorConfigProperties.toEditorConfigOverride(),
            additionalRuleProviders = additionalRuleProviders.toSet(),
        )

    private fun MutableSet<Pair<EditorConfigProperty<*>, *>>.toEditorConfigOverride() =
        if (isEmpty()) {
            EMPTY_EDITOR_CONFIG_OVERRIDE
        } else {
            EditorConfigOverride.from(*toTypedArray())
        }

    private fun code() =
        if (filePath != null) {
            // Create a file containing the code and the given filepath. CodeFile can not be used here as it can not read successfully from
            // the Ktlint test file system.
            val path = KTLINT_TEST_FILE_SYSTEM.resolve(filePath)
            KTLINT_TEST_FILE_SYSTEM.writeFile(path.pathString, code)
            Code.fromPath(path)
        } else {
            Code.fromSnippet(code, kotlinScript)
        }

    public companion object {
        /**
         * Creates an assertThat assertion function for the rule provided by [provider]. This assertion function has
         * extensions specifically for testing KtLint rules.
         */
        public fun assertThatRule(provider: () -> Rule): (String) -> KtLintAssertThat = RuleProvider { provider() }.assertThat()

        /**
         * Creates an assertThat assertion function for the rule provided by [provider]. This assertion function has
         * extensions specifically for testing KtLint rules. The rules provided via [additionalRuleProviders] are only
         * executed during the format phase of the test. This means that the unit test only has to check the lint
         * violations thrown by the rule for which the assertThat is created. But the code is formatted by both the rule
         * and the rules provided by [additionalRuleProviders] in the order as defined by the rule definitions.
         */

        public fun assertThatRule(
            provider: () -> Rule,
            additionalRuleProviders: Set<RuleProvider> = emptySet(),
            editorConfigProperties: Set<Pair<EditorConfigProperty<*>, *>> = emptySet(),
        ): (String) -> KtLintAssertThat = RuleProvider { provider() }.assertThat(additionalRuleProviders, editorConfigProperties)

        private fun RuleProvider.assertThat(
            additionalRuleProviders: Set<RuleProvider> = emptySet(),
            editorConfigProperties: Set<Pair<EditorConfigProperty<*>, *>> = emptySet(),
        ): (String) -> KtLintAssertThat =
            { code ->
                KtLintAssertThat(
                    ruleProvider = this,
                    code = code,
                    additionalRuleProviders = additionalRuleProviders.toMutableSet(),
                    editorConfigProperties = editorConfigProperties.toMutableSet(),
                )
            }

        /**
         * See [setMaxLineLength] for intended usage.
         */
        public const val MAX_LINE_LENGTH_MARKER: String = "Max line length marker:"

        /**
         * See [setMaxLineLength] for intended usage.
         */
        public const val EOL_CHAR: Char = '#'

        init {
            /* A unit test using the max line length marker typically looks like:
             * val code =
             *     """
             *     // $MAX_LINE_LENGTH_MARKER                   $EOL_CHAR
             *     val fooooooooooooooo = "fooooooooooooooooooooo"
             *     """.trimIndent()
             * In order for this to work properly, the length of "$MAX_LINE_LENGTH_MARKER" must be identical to the length of the real value
             * of the constant MAX_LINE_LENGTH_MARKER.
             */
            require("$".plus(::MAX_LINE_LENGTH_MARKER.name).length == MAX_LINE_LENGTH_MARKER.length) {
                "Length of the value of the string '${'$'}MAX_LINE_LENGTH_MARKER' must be identical to length of the value of the " +
                    "constant. Tests using this constant will not work as intended otherwise."
            }
        }
    }
}

/**
 * Immutable assertable. Once the first assertion is made on [KtLintAssertThat] it is converted to the
 * [KtLintAssertThatAssertable] which allows no further modifications of the internal state. This guarantees that all
 * assertions operate on the same state.
 */
public class KtLintAssertThatAssertable(
    /** The provider of the rule which is the subject of the test, e.g. the rule for which the AssertThat is created. */
    private val ruleProvider: RuleProvider,
    private val code: Code,
    private val editorConfigOverride: EditorConfigOverride = EMPTY_EDITOR_CONFIG_OVERRIDE,
    /**
     *  The rules which have to be executed in addition to the main rule when linting/formatting the code. Note that
     *  lint errors for those rules are suppressed.
     */
    private val additionalRuleProviders: Set<RuleProvider>,
) : AbstractAssert<KtLintAssertThatAssertable, String>(code.content, KtLintAssertThatAssertable::class.java) {
    private val ruleId = ruleProvider.createNewRuleInstance().ruleId

    /**
     * Asserts that the code does not contain any [LintViolation]s caused by the rule associated with the
     * KtLintAssertThat.
     *
     * Note: When linting succeeds without errors, formatting is also checked.
     */
    public fun hasNoLintViolations() {
        assertThat(lint().filterCurrentRuleOnly()).isEmpty()

        // Also format the code to be absolutely sure that codes does not get changed
        val (actualFormattedCode, lintErrorsWhenFormatting) = format()

        assertAll(
            {
                assertThat(lintErrorsWhenFormatting)
                    .describedAs(
                        "LintViolations found by format while no lint errors were found during Lint. " +
                            if (additionalRuleProviders.isEmpty()) {
                                "This is unexpected as no additional rule have been defined for this test."
                            } else {
                                "If this is caused by an additional rule added to the test, then remove that rule in case it " +
                                    "is always executed before the rule under test."
                            },
                    )
                    .isEmpty()
            },
            {
                assertThat(actualFormattedCode)
                    .describedAs(
                        "Code is changed by format while no lint errors were found. " +
                            if (additionalRuleProviders.isEmpty()) {
                                "This is unexpected as no additional rule have been defined for this test."
                            } else {
                                "If this is caused by an additional rule added to the test, then remove that rule in case it " +
                                    "is always executed before the rule under test."
                            },
                    )
                    .isEqualTo(code.content)
            },
        )
    }

    /**
     * Asserts that the code does not contain any [LintViolation]s for the given rule id.
     */
    public fun hasNoLintViolationsForRuleId(ruleId: RuleId): KtLintAssertThatAssertable {
        val (_, lintErrorsWhenFormatting) = format()

        assertThat(lintErrorsWhenFormatting.filter { it.ruleId == ruleId })
            .describedAs("At least 1 lint violation was found for rule id '${ruleId.value}' while none were expected")
            .isEmpty()

        return this
    }

    /**
     * Asserts that the code does not contain any [LintViolation]s except in the additional formatting rules.
     *
     * Note that this method can and should be chained with [isFormattedAs] to verify whether the code is correctly
     * formatted.
     *
     * This method can be used when the rule which is associated with the KtLintAssertThat is not violated by the sample
     * code, but the code is reformatted by the additional formatting rules. In case that rules are dependent on each
     * other, a unit test cal still verify that code is formatted correctly even when the rule under test is not
     * violated.
     */
    public fun hasNoLintViolationsExceptInAdditionalRules(): KtLintAssertThatAssertable {
        check(additionalRuleProviders.isNotEmpty()) {
            "hasNoLintViolationsExceptInAdditionalRules can only be used when additional rules have been added"
        }

        val lintErrors = lint()
        assertThat(lintErrors.filterCurrentRuleOnly()).isEmpty()
        assertThat(lintErrors.filterAdditionalRulesOnly()).isNotEmpty

        return this
    }

    /**
     * Asserts that the code does contain given [LintViolation]. This is a sugar coated version of
     * [hasLintViolation] for the case that the code contains exactly one lint violation.
     */
    public fun hasLintViolation(
        line: Int,
        col: Int,
        detail: String,
    ): KtLintAssertThatAssertable =
        hasLintViolations(
            LintViolation(
                line = line,
                col = col,
                detail = detail,
            ),
        )

    /**
     * Asserts that the code does contain given [LintViolation] caused by an additional rule. This is a sugar coated
     * version of [hasLintViolationsForAdditionalRules] for the case that the code contains exactly one lint violation.
     */
    public fun hasLintViolationForAdditionalRule(
        line: Int,
        col: Int,
        detail: String,
    ): KtLintAssertThatAssertable =
        hasLintViolationsForAdditionalRules(
            LintViolation(
                line = line,
                col = col,
                detail = detail,
            ),
        )

    /**
     * Asserts that the code does contain given [LintViolation]s which can be automatically corrected.
     */
    public fun hasLintViolations(vararg expectedErrors: LintViolation): KtLintAssertThatAssertable {
        check(expectedErrors.isNotEmpty())

        val actualLintViolationFields =
            lint()
                .filterCurrentRuleOnly()
                .toLintViolationsFields()
        assertThat(actualLintViolationFields)
            .describedAs("Lint errors which can be automatically corrected")
            .containsExactlyInAnyOrder(*expectedErrors.toLintViolationsFields(canBeAutoCorrected = true))
        return this
    }

    /**
     * Asserts that the code does contain given [LintViolation]s caused by additional rules and which can be
     * automatically corrected.
     */
    public fun hasLintViolationsForAdditionalRules(vararg expectedErrors: LintViolation): KtLintAssertThatAssertable {
        check(expectedErrors.isNotEmpty())

        val actualLintViolationFields =
            lint()
                .filterAdditionalRulesOnly()
                .toLintViolationsFields()
        assertThat(actualLintViolationFields)
            .describedAs("Lint errors which can be automatically corrected")
            .containsExactlyInAnyOrder(*expectedErrors.toLintViolationsFields(canBeAutoCorrected = true))
        return this
    }

    /**
     * Asserts that the code is formatted as given.
     */
    public fun isFormattedAs(formattedCode: String): KtLintAssertThatAssertable {
        check(formattedCode != code.content) {
            "Use '.hasNoLintViolations()' instead of '.isFormattedAs(<original code>)'"
        }

        // Also format the code to be absolutely sure that codes does not get changed
        val (actualFormattedCode, _) = format()

        assertThat(actualFormattedCode)
            .describedAs("Code is formatted as")
            .isEqualTo(formattedCode)

        return this
    }

    /**
     * Asserts that the code contains the given [LintViolation] which can not be automatically corrected.
     */
    public fun hasLintViolationWithoutAutoCorrect(
        line: Int,
        col: Int,
        detail: String,
    ): Unit =
        hasLintViolationsWithoutAutocorrect(
            LintViolation(
                line = line,
                col = col,
                detail = detail,
            ),
        )

    /**
     * Asserts that the code does contain the given [LintViolation]s and that those violations can not be automatically
     * corrected.
     */
    public fun hasLintViolationsWithoutAutocorrect(vararg expectedLintViolations: LintViolation) {
        check(expectedLintViolations.isNotEmpty())

        val actualLintViolationFields =
            lint()
                .filterCurrentRuleOnly()
                .toLintViolationsFields()

        assertThat(actualLintViolationFields)
            .describedAs("Lint errors which can not be automatically corrected")
            .containsExactlyInAnyOrder(*expectedLintViolations.toLintViolationsFields(canBeAutoCorrected = false))
    }

    private fun Array<out LintViolation>.toLintViolationsFields(canBeAutoCorrected: Boolean): Array<LintViolationFields> {
        return map {
            LintViolationFields(
                line = it.line,
                col = it.col,
                detail = it.detail,
                canBeAutoCorrected = canBeAutoCorrected,
            )
        }.toTypedArray()
    }

    private fun List<LintError>.filterAdditionalRulesOnly() = filter { it.ruleId != ruleId }

    private fun List<LintError>.filterCurrentRuleOnly() = filter { it.ruleId == ruleId }

    private fun List<LintError>.toLintViolationsFields(): Array<LintViolationFields> =
        map {
            LintViolationFields(
                line = it.line,
                col = it.col,
                detail = it.detail,
                canBeAutoCorrected = it.canBeAutoCorrected,
            )
        }.toTypedArray()

    private fun createKtLintRuleEngine(): KtLintRuleEngine {
        val ruleProviders =
            setOf(ruleProvider)
                // Also run the additional rules as the main rule might have a VisitorModifier which requires one or more of
                // the additional rules to be loaded and enabled as well.
                .plus(additionalRuleProviders)
        val editorConfigOverride =
            editorConfigOverride
                .enableExperimentalRules()
                .extendWithRuleSetRuleExecutionsFor(ruleProviders)
        return KtLintRuleEngine(
            ruleProviders = ruleProviders,
            editorConfigOverride = editorConfigOverride,
            fileSystem = KTLINT_TEST_FILE_SYSTEM.fileSystem,
        )
    }

    private fun lint(): List<LintError> {
        val lintErrors = ArrayList<LintError>()
        createKtLintRuleEngine().lint(code) { lintError -> lintErrors.add(lintError) }
        return lintErrors
    }

    private fun format(): Pair<String, List<LintError>> {
        val lintErrors = ArrayList<LintError>()
        val formattedCode = createKtLintRuleEngine().format(code) { lintError, _ -> lintErrors.add(lintError) }
        return Pair(formattedCode, lintErrors)
    }

    /* Representation of the field of the [LintError] that should be identical. Note that no comparison can be made
     * against the original [LintError] as the [canBeAutoCorrected] flag is excluded from the hashcode.
     */
    private data class LintViolationFields(
        val line: Int,
        val col: Int,
        val detail: String,
        val canBeAutoCorrected: Boolean,
    )
}

internal class MissingEolMarker : RuntimeException(
    """
    The first line of the provide code sample should contain text '$MAX_LINE_LENGTH_MARKER' which is provided by the
    constant '${::MAX_LINE_LENGTH_MARKER.name}' and ends with the EOL_CHAR'$EOL_CHAR' provided by the constant
    '${::EOL_CHAR.name}' which indicates the last position that is allowed.
    """.trimIndent(),
)

/**
 * Expectation of the [LintError]. Contrary to the [LintError] it does not contain the ruleId. The ruleId will be
 * derived from the rule for which the AssertThat was created.
 */
public data class LintViolation(
    val line: Int,
    val col: Int,
    val detail: String,
)

/**
 * Enables the rule sets for the given set of [ruleProviders] unless the rule execution of that rule set was already
 * provided.
 */
private fun EditorConfigOverride.extendWithRuleSetRuleExecutionsFor(ruleProviders: Set<RuleProvider>): EditorConfigOverride {
    val ruleSetRuleExecutions =
        ruleProviders
            .asSequence()
            .map { ruleProvider ->
                ruleProvider
                    .createNewRuleInstance()
                    .ruleId
                    .ruleSetId
                    .createRuleSetExecutionEditorConfigProperty()
            }.distinct()
            .filter { editorConfigProperty -> this.properties[editorConfigProperty] == null }
            .map { it to RuleExecution.enabled }
            .toList()
            .toTypedArray()
    return this.plus(*ruleSetRuleExecutions)
}

private fun EditorConfigOverride.enableExperimentalRules(): EditorConfigOverride =
    plus(EXPERIMENTAL_RULES_EXECUTION_PROPERTY to RuleExecution.enabled)
