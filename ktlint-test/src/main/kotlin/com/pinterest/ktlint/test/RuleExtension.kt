package com.pinterest.ktlint.test

import com.pinterest.ktlint.logger.api.initKtLintKLogger
import com.pinterest.ktlint.logger.api.setDefaultLoggerModifier
import com.pinterest.ktlint.rule.engine.api.EditorConfigOverride
import com.pinterest.ktlint.rule.engine.api.EditorConfigOverride.Companion.plus
import com.pinterest.ktlint.rule.engine.api.KtLint
import com.pinterest.ktlint.rule.engine.api.KtLintRuleEngine
import com.pinterest.ktlint.rule.engine.api.LintError
import com.pinterest.ktlint.rule.engine.core.api.RuleProvider
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.EXPERIMENTAL_RULES_EXECUTION_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.RuleExecution
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.createRuleSetExecutionEditorConfigProperty
import com.pinterest.ruleset.testtooling.DumpASTRule
import mu.KotlinLogging
import org.jetbrains.kotlin.utils.addToStdlib.ifTrue
import java.nio.file.Paths

private val LOGGER =
    KotlinLogging
        .logger {}
        .setDefaultLoggerModifier { logger ->
            if (!logger.isTraceEnabled || !logger.isDebugEnabled) {
                logger.info {
                    """
                    Additional information can be printed during running of unit tests, by setting one or more of environment variables below:
                        $KTLINT_UNIT_TEST_TRACE=[on|off]
                        $KTLINT_UNIT_TEST_DUMP_AST=[on|off]
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

// Via command line parameter "--print-ast" the end user of ktlint can change the logging behavior. As unit tests are
// not invoked via the main ktlint runtime, this command line parameter can not be used to change the logging behavior
// while running the unit tests. Instead, the environment variable below can be used by ktlint developers to change the
// logging behavior.
private const val KTLINT_UNIT_TEST_DUMP_AST = "KTLINT_UNIT_TEST_DUMP_AST"
private const val KTLINT_UNIT_TEST_ON_PROPERTY = "ON"

private fun Set<RuleProvider>.toRuleProviders(): Set<RuleProvider> {
    val dumpAstRuleProvider = System
        .getenv(KTLINT_UNIT_TEST_DUMP_AST)
        .orEmpty()
        .equals(KTLINT_UNIT_TEST_ON_PROPERTY, ignoreCase = true)
        .ifTrue {
            LOGGER.info { "Dump AST of code before processing as System environment variable $KTLINT_UNIT_TEST_DUMP_AST is set to 'on'" }
            RuleProvider {
                DumpASTRule(
                    // Write to STDOUT. The focus in a failed unit test should first go to the error in the rule that is
                    // to be tested and not to the AST,
                    out = System.out,
                )
            }
        }
    return this.plus(setOfNotNull(dumpAstRuleProvider))
}

@Deprecated(
    message = "Marked for removal in KtLint 0.49",
    replaceWith = ReplaceWith("lint(filePath,text,editorConfigOverride)"),
)
public fun Set<RuleProvider>.lint(
    lintedFilePath: String? = null,
    text: String,
    editorConfigOverride: EditorConfigOverride = EditorConfigOverride.EMPTY_EDITOR_CONFIG_OVERRIDE,
    userData: Map<String, String> = emptyMap(),
    script: Boolean = false,
): List<LintError> {
    val lintErrors = ArrayList<LintError>()
    val ruleProviders = toRuleProviders()
    val experimentalParams = KtLint.ExperimentalParams(
        fileName = lintedFilePath,
        text = text,
        ruleProviders = this.toRuleProviders(),
        editorConfigOverride = editorConfigOverride
            .enableExperimentalRules()
            .extendWithRuleSetRuleExecutionsFor(ruleProviders),
        userData = userData,
        script = script,
        cb = { lintError, _ -> lintErrors.add(lintError) },
        debug = true,
    )
    KtLint.lint(experimentalParams)
    return lintErrors
}

/**
 * Execute [KtLintRuleEngine.lint] on given code snippet. To test a kotlin script file, provide a filepath ending with
 * ".kts". For each invocation of this method, a fresh instance of the [KtLintRuleEngine] is instantiated for the given
 * set of rules.
 * This method is intended to be executed in a unit test environment only. If the project that is containing the unit
 * test contains an '.editorconfig' file, then it will be ignored entirely. Provide '.editorconfig' properties that have
 * to be applied on the code snippet via [editorConfigOverride].
 */
public fun Set<RuleProvider>.lint(
    text: String,
    filePath: String? = null,
    editorConfigOverride: EditorConfigOverride = EditorConfigOverride.EMPTY_EDITOR_CONFIG_OVERRIDE,
): List<LintError> {
    val lintErrors = ArrayList<LintError>()
    val ruleProviders = toRuleProviders()
    KtLintRuleEngine(
        ruleProviders = ruleProviders,
        editorConfigOverride = editorConfigOverride
            .enableExperimentalRules()
            .extendWithRuleSetRuleExecutionsFor(ruleProviders),
        // The unit test itself has to obey with the ktlint configuration in the '.editorconfig' file. The code snippets
        // inside the unit test may not be affected by the '.editorconfig' configuration of the ktlint project itself.
        ignoreEditorConfigOnFileSystem = true,
    ).lint(
        code = text,
        filePath = filePath?.let { Paths.get(filePath) },
    ) { lintError -> lintErrors.add(lintError) }
    return lintErrors
}

/**
 * Enables the rule sets for the given set of [ruleProviders] unless the rule execution of that rule set was already
 * provided.
 */
private fun EditorConfigOverride.extendWithRuleSetRuleExecutionsFor(ruleProviders: Set<RuleProvider>): EditorConfigOverride {
    val ruleSetRuleExecutions = ruleProviders
        .asSequence()
        .map { it.createNewRuleInstance().ruleId.ruleSetId.createRuleSetExecutionEditorConfigProperty() }
        .distinct()
        .filter { editorConfigProperty -> this.properties[editorConfigProperty] == null }
        .map { it to RuleExecution.enabled }
        .toList()
        .toTypedArray()
    return this.plus(*ruleSetRuleExecutions)
}

private fun EditorConfigOverride.enableExperimentalRules(): EditorConfigOverride =
    plus(EXPERIMENTAL_RULES_EXECUTION_PROPERTY to RuleExecution.enabled)

@Deprecated(
    message = "Marked for removal in KtLint 0.49",
    replaceWith = ReplaceWith("format(filePath,text,editorConfigOverride)"),
)
public fun Set<RuleProvider>.format(
    lintedFilePath: String?,
    text: String,
    editorConfigOverride: EditorConfigOverride = EditorConfigOverride.EMPTY_EDITOR_CONFIG_OVERRIDE,
    userData: Map<String, String> = emptyMap(),
    cb: (e: LintError, corrected: Boolean) -> Unit = { _, _ -> },
    script: Boolean = false,
): Pair<String, List<LintError>> {
    val lintErrors = ArrayList<LintError>()
    val ruleProviders = toRuleProviders()
    val experimentalParams = KtLint.ExperimentalParams(
        fileName = lintedFilePath,
        text = text,
        ruleProviders = ruleProviders,
        editorConfigOverride = editorConfigOverride
            .enableExperimentalRules()
            .extendWithRuleSetRuleExecutionsFor(ruleProviders),
        userData = userData,
        script = script,
        cb = { lintError, _ -> lintErrors.add(lintError) },
        debug = true,
    )
    val formattedCode = KtLint.format(experimentalParams)
    return Pair(formattedCode, lintErrors)
}

/**
 * Execute [KtLintRuleEngine.format] on given code snippet. To test a kotlin script file, provide a filepath ending with
 * ".kts". For each invocation of this method, a fresh instance of the [KtLintRuleEngine] is instantiated for the given
 * set of rules.
 * This method is intended to be executed in a unit test environment only. If the project that is containing the unit
 * test contains an '.editorconfig' file, then it will be ignored entirely. Provide '.editorconfig' properties that have
 * to be applied on the code snippet via [editorConfigOverride].
 */
public fun Set<RuleProvider>.format(
    text: String,
    filePath: String?,
    editorConfigOverride: EditorConfigOverride = EditorConfigOverride.EMPTY_EDITOR_CONFIG_OVERRIDE,
): Pair<String, List<LintError>> {
    val lintErrors = ArrayList<LintError>()
    val ruleProviders = toRuleProviders()
    val formattedCode =
        KtLintRuleEngine(
            ruleProviders = ruleProviders,
            editorConfigOverride = editorConfigOverride
                .enableExperimentalRules()
                .extendWithRuleSetRuleExecutionsFor(ruleProviders),
            // The unit test itself has to obey with the ktlint configuration in the '.editorconfig' file. The code snippets
            // inside the unit test may not be affected by the '.editorconfig' configuration of the ktlint project itself.
            ignoreEditorConfigOnFileSystem = true,
        ).format(
            code = text,
            filePath = filePath?.let { Paths.get(filePath) },
        ) { lintError, _ -> lintErrors.add(lintError) }
    return Pair(formattedCode, lintErrors)
}
