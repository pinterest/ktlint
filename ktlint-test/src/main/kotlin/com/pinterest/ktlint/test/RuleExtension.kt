package com.pinterest.ktlint.test

import com.pinterest.ktlint.core.KtLint
import com.pinterest.ktlint.core.LintError
import com.pinterest.ktlint.core.RuleProvider
import com.pinterest.ktlint.core.api.EditorConfigOverride
import com.pinterest.ktlint.core.initKtLintKLogger
import com.pinterest.ktlint.core.setDefaultLoggerModifier
import com.pinterest.ruleset.test.DumpASTRule
import mu.KotlinLogging
import org.jetbrains.kotlin.utils.addToStdlib.ifTrue

private val logger =
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
            logger.info { "Dump AST of code before processing as System environment variable $KTLINT_UNIT_TEST_DUMP_AST is set to 'on'" }
            RuleProvider {
                DumpASTRule(
                    // Write to STDOUT. The focus in a failed unit test should first go to the error in the rule that is
                    // to be tested and not to the AST,
                    out = System.out
                )
            }
        }
    return this.plus(setOfNotNull(dumpAstRuleProvider))
}

public fun Set<RuleProvider>.lint(
    lintedFilePath: String? = null,
    text: String,
    editorConfigOverride: EditorConfigOverride = EditorConfigOverride.emptyEditorConfigOverride,
    userData: Map<String, String> = emptyMap(),
    script: Boolean = false
): List<LintError> {
    val res = ArrayList<LintError>()
    val experimentalParams = KtLint.ExperimentalParams(
        fileName = lintedFilePath,
        text = text,
        ruleProviders = this.toRuleProviders(),
        editorConfigOverride = editorConfigOverride,
        userData = userData,
        script = script,
        cb = { e, _ -> res.add(e) }
    )
    KtLint.lint(
        experimentalParams
    )
    return res
}

public fun Set<RuleProvider>.format(
    lintedFilePath: String?,
    text: String,
    editorConfigOverride: EditorConfigOverride = EditorConfigOverride.emptyEditorConfigOverride,
    userData: Map<String, String> = emptyMap(),
    cb: (e: LintError, corrected: Boolean) -> Unit = { _, _ -> },
    script: Boolean = false
): String {
    val experimentalParams = KtLint.ExperimentalParams(
        fileName = lintedFilePath,
        text = text,
        ruleProviders = this.toRuleProviders(),
        editorConfigOverride = editorConfigOverride,
        userData = userData,
        script = script,
        cb = cb
    )
    return KtLint.format(experimentalParams)
}
