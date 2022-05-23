package com.pinterest.ktlint.test

import com.pinterest.ktlint.core.KtLint
import com.pinterest.ktlint.core.LintError
import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.RuleSet
import com.pinterest.ktlint.core.api.EditorConfigOverride
import com.pinterest.ktlint.core.api.FeatureInAlphaState
import com.pinterest.ktlint.core.initKtLintKLogger
import com.pinterest.ktlint.core.setDefaultLoggerModifier
import com.pinterest.ruleset.test.DumpASTRule
import mu.KotlinLogging
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.util.diff.DiffUtils.diff
import org.assertj.core.util.diff.DiffUtils.generateUnifiedDiff
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

private fun List<Rule>.toRuleSets(): List<RuleSet> {
    val dumpAstRuleSet = System
        .getenv(KTLINT_UNIT_TEST_DUMP_AST)
        .orEmpty()
        .equals(KTLINT_UNIT_TEST_ON_PROPERTY, ignoreCase = true)
        .ifTrue {
            logger.info { "Dump AST of code before processing as System environment variable $KTLINT_UNIT_TEST_DUMP_AST is set to 'on'" }
            RuleSet(
                "debug",
                DumpASTRule(
                    // Write to STDOUT. The focus in a failed unit test should first go to the error in the rule that is
                    // to be tested and not to the AST,
                    out = System.out
                )
            )
        }
    return this
        .groupBy { it.id.substringBefore(":", "standard") }
        .map { (ruleSetId, rules) -> RuleSet(ruleSetId, *rules.toTypedArray()) }
        .plus(listOfNotNull(dumpAstRuleSet))
}

@FeatureInAlphaState
public fun List<Rule>.lint(
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
        ruleSets = toRuleSets(),
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

public fun List<Rule>.format(
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
        ruleSets = this.toRuleSets(),
        editorConfigOverride = editorConfigOverride,
        userData = userData,
        script = script,
        cb = cb
    )
    return KtLint.format(experimentalParams)
}

@FeatureInAlphaState
public fun Rule.diffFileLint(
    path: String,
    // TODO: Set default value once method is no longer annotated with FeatureInAlphaState and function
    //  diffFileLint(String, Map<String, String>) is removed.
    editorConfigOverride: EditorConfigOverride
): String {
    val resourceText = getResourceAsText(path).replace("\r\n", "\n")
    val dividerIndex = resourceText.lastIndexOf("\n// expect\n")
    if (dividerIndex == -1) {
        throw RuntimeException("$path must contain '// expect' line")
    }
    val input = resourceText.substring(0, dividerIndex)
    val expected = resourceText.substring(dividerIndex + 1).split('\n').mapNotNull { line ->
        if (line.isBlank() || line == "// expect") {
            null
        } else {
            line.trimMargin("// ").split(':', limit = 3).let { expectation ->
                if (expectation.size != 3) {
                    throw RuntimeException("$path expectation must be a triple <line>:<column>:<message>")
                    // " (<message> is not allowed to contain \":\")")
                }
                val message = expectation[2]
                val detail = message.removeSuffix(" (cannot be auto-corrected)")
                LintError(expectation[0].toInt(), expectation[1].toInt(), id, detail, message == detail)
            }
        }
    }
    val actual = listOf(this).lint(
        text = input,
        editorConfigOverride = editorConfigOverride,
        script = true
    )
    val str = { err: LintError ->
        val ruleId = if (err.ruleId != id) " (${err.ruleId})" else ""
        val correctionStatus = if (!err.canBeAutoCorrected) " (cannot be auto-corrected)" else ""
        "${err.line}:${err.col}:${err.detail}$ruleId$correctionStatus"
    }
    val diff =
        generateUnifiedDiff(
            "expected",
            "actual",
            expected.map(str),
            diff(expected.map(str), actual.map(str)),
            expected.size + actual.size
        ).joinToString("\n")
    return diff.ifEmpty { "" }
}

@FeatureInAlphaState
public fun Rule.diffFileFormat(
    srcPath: String,
    expectedPath: String,
    editorConfigOverride: EditorConfigOverride = EditorConfigOverride.emptyEditorConfigOverride
): String = listOf(this).diffFileFormat(srcPath, expectedPath, editorConfigOverride)

@FeatureInAlphaState
public fun List<Rule>.diffFileFormat(
    srcPath: String,
    expectedPath: String,
    editorConfigOverride: EditorConfigOverride = EditorConfigOverride.emptyEditorConfigOverride
): String {
    val actual = format(
        lintedFilePath = null,
        text = getResourceAsText(srcPath),
        editorConfigOverride = editorConfigOverride,
        script = true
    ).split('\n')
    val expected = getResourceAsText(expectedPath).split('\n')
    val diff =
        generateUnifiedDiff(expectedPath, "output", expected, diff(expected, actual), expected.size + actual.size)
            .joinToString("\n")
    return diff.ifEmpty { "" }
}

@FeatureInAlphaState
public fun Rule.assertThatFileFormat(
    srcPath: String,
    expectedPath: String,
    // TODO: Set default value once method is no longer annotated with FeatureInAlphaState and function
    //  diffFileLint(String, Map<String, String>) is removed.
    editorConfigOverride: EditorConfigOverride,
    userData: Map<String, String> = emptyMap()
) {
    val actual = listOf(this).format(null, getResourceAsText(srcPath), editorConfigOverride, userData, script = true).split('\n')
    val expected = getResourceAsText(expectedPath).split('\n')
    assertThat(actual).isEqualTo(expected)
}

private fun getResourceAsText(path: String) =
    (ClassLoader.getSystemClassLoader().getResourceAsStream(path) ?: throw RuntimeException("$path not found"))
        .bufferedReader()
        .readText()
