package com.pinterest.ktlint.test

import com.pinterest.ktlint.logger.api.initKtLintKLogger
import com.pinterest.ktlint.logger.api.setDefaultLoggerModifier
import com.pinterest.ktlint.rule.engine.api.Code
import com.pinterest.ktlint.rule.engine.api.EditorConfigOverride
import com.pinterest.ktlint.rule.engine.api.EditorConfigOverride.Companion.plus
import com.pinterest.ktlint.rule.engine.api.KtLintRuleEngine
import com.pinterest.ktlint.rule.engine.api.LintError
import com.pinterest.ktlint.rule.engine.core.api.RuleProvider
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.EXPERIMENTAL_RULES_EXECUTION_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.RuleExecution
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.createRuleSetExecutionEditorConfigProperty
import mu.KotlinLogging

// TODO: Remove entire file in KtLint 0.50. Code has already been duplicated and refactored into KtLintAssertThat.

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

// Via command line parameter "--trace" the end user of ktlint can change the logging behavior. As unit tests are not
// invoked via the main ktlint runtime, this command line parameter can not be used to change the logging behavior while
// running the unit tests. Instead, the environment variable below can be set by ktlint developers to change the logging
// behavior.
// Keep value in sync with value in 'logback-test.xml' source in module 'ktlint-test-logging'
private const val KTLINT_UNIT_TEST_TRACE = "KTLINT_UNIT_TEST_TRACE"

/**
 * Execute [KtLintRuleEngine.lint] on given code snippet. To test a kotlin script file, provide a filepath ending with
 * ".kts". For each invocation of this method, a fresh instance of the [KtLintRuleEngine] is instantiated for the given
 * set of rules.
 * This method is intended to be executed in a unit test environment only. If the project that is containing the unit
 * test contains an '.editorconfig' file, then it will be ignored entirely. Provide '.editorconfig' properties that have
 * to be applied on the code snippet via [editorConfigOverride].
 */
@Deprecated("Marked for removal in Ktlint 0.50. Raise an issue if you actually are using this function.")
public fun Set<RuleProvider>.lint(
    text: String,
    filePath: String? = null,
    editorConfigOverride: EditorConfigOverride = EditorConfigOverride.EMPTY_EDITOR_CONFIG_OVERRIDE,
): List<LintError> {
    val lintErrors = ArrayList<LintError>()
    val code =
        if (filePath != null) {
            Code.fromFile(KTLINT_TEST_FILE_SYSTEM.resolve(filePath).toFile())
        } else {
            Code.fromSnippet(text)
        }
    KtLintRuleEngine(
        ruleProviders = this,
        editorConfigOverride =
            editorConfigOverride
                .enableExperimentalRules()
                .extendWithRuleSetRuleExecutionsFor(this),
        fileSystem = KTLINT_TEST_FILE_SYSTEM.fileSystem,
    ).lint(code) { lintError -> lintErrors.add(lintError) }
    return lintErrors
}

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

/**
 * Execute [KtLintRuleEngine.format] on given code snippet. To test a kotlin script file, provide a filepath ending with
 * ".kts". For each invocation of this method, a fresh instance of the [KtLintRuleEngine] is instantiated for the given
 * set of rules.
 * This method is intended to be executed in a unit test environment only. If the project that is containing the unit
 * test contains an '.editorconfig' file, then it will be ignored entirely. Provide '.editorconfig' properties that have
 * to be applied on the code snippet via [editorConfigOverride].
 */
@Deprecated("Marked for removal in Ktlint 0.50. Raise an issue if you actually are using this function.")
public fun Set<RuleProvider>.format(
    text: String,
    filePath: String?,
    editorConfigOverride: EditorConfigOverride = EditorConfigOverride.EMPTY_EDITOR_CONFIG_OVERRIDE,
): Pair<String, List<LintError>> {
    val lintErrors = ArrayList<LintError>()
    val code =
        if (filePath != null) {
            Code.fromFile(KTLINT_TEST_FILE_SYSTEM.resolve(filePath).toFile())
        } else {
            Code.fromSnippet(text)
        }
    val formattedCode =
        KtLintRuleEngine(
            ruleProviders = this,
            editorConfigOverride =
                editorConfigOverride
                    .enableExperimentalRules()
                    .extendWithRuleSetRuleExecutionsFor(this),
            fileSystem = KTLINT_TEST_FILE_SYSTEM.fileSystem,
        ).format(code) { lintError, _ -> lintErrors.add(lintError) }
    return Pair(formattedCode, lintErrors)
}

// The execution of the unit tests may not be affected by the ".editorconfig" configuration of the Ktlint project itself. So each unit test
// is to be run on a KtlintTestFileSystem not having any ".editorconfig" files. This variable should not be exposed, as unit tests should
// not be allowed to write any content on the file system.
// As this FileSystem is not modified it is not considered to be a problem that the file system is not closed after each unit test.
private val KTLINT_TEST_FILE_SYSTEM = KtlintTestFileSystem()
