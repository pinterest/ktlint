package com.pinterest.ktlint.test

import com.pinterest.ktlint.core.KtLint
import com.pinterest.ktlint.core.LintError
import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.RuleSet
import com.pinterest.ktlint.core.VisitorProvider
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

/**
 * Runs lint for a single rule on a piece of code.
 */
// TODO: Remove once the deprecated function "Rule.lint(String, Map<String, String>, boolean)" is removed. This function
//  is added to prevent deprecation warning on call where no userData parameter is specified.
public fun Rule.lint(
    text: String,
    script: Boolean = false
): List<LintError> = lint(null, text, emptyMap(), script)

/**
 * Runs lint for a single rule on a piece of code.
 */
@Suppress("DeprecatedCallableAddReplaceWith")
@Deprecated(
    message = "Marked for removal in Ktlint 0.46. If parameter 'userData' contains EditorConfig properties, then " +
        "specify these properties via parameter 'EditorConfigOverride.'",
    level = DeprecationLevel.WARNING
)
public fun Rule.lint(
    text: String,
    // Default value is removed so that the deprecation warning will only be shown when the userData parameter is
    // actually specified.
    userData: Map<String, String>,
    script: Boolean = false
): List<LintError> = lint(null, text, userData, script)

/**
 * Runs lint for a list of rules on a piece of code. Rules should be specified in exact order as they will be executed
 * by the production code. Its primary usage is testing rules which are closely related like wrapping and indent rules.
 */
// TODO: Remove once the deprecated function "List<Rule>.lint(String, Map<String, String>, boolean)" is removed. This
//  function is added to prevent deprecation warning on call where no userData parameter is specified.
public fun List<Rule>.lint(
    text: String,
    script: Boolean = false
): List<LintError> = lint(null, text, emptyMap(), script)

/**
 * Runs lint for a list of rules on a piece of code. Rules should be specified in exact order as they will be executed
 * by the production code. Its primary usage is testing rules which are closely related like wrapping and indent rules.
 */
@Suppress("DeprecatedCallableAddReplaceWith")
@Deprecated(
    message = "Marked for removal in Ktlint 0.46. If parameter 'userData' contains EditorConfig properties, then " +
        "specify these properties via parameter 'EditorConfigOverride.'",
    level = DeprecationLevel.WARNING
)
public fun List<Rule>.lint(
    text: String,
    // Default value is removed so that the deprecation warning will only be shown when the userData parameter is
    // actually specified.
    userData: Map<String, String>,
    script: Boolean = false
): List<LintError> = lint(null, text, userData, script)

/**
 * Runs lint for a single rule on a piece of code.
 */
// TODO: Remove once the deprecated function "Rule.lint(String, String, Map<String, String>, boolean)" is removed. This
//  function is added to prevent deprecation warning on call where no userData parameter is specified.
public fun Rule.lint(
    lintedFilePath: String?,
    text: String,
    script: Boolean = false
): List<LintError> =
    listOf(this).lint(lintedFilePath, text, emptyMap(), script)

/**
 * Runs lint for a single rule on a piece of code.
 */
@Suppress("DeprecatedCallableAddReplaceWith")
@Deprecated(
    message = "Marked for removal in Ktlint 0.46. If parameter 'userData' contains EditorConfig properties, then " +
        "specify these properties via parameter 'EditorConfigOverride.'",
    level = DeprecationLevel.WARNING
)
public fun Rule.lint(
    lintedFilePath: String?,
    text: String,
    // Default value is removed so that the deprecation warning will only be shown when the userData parameter is
    // actually specified.
    userData: Map<String, String>,
    script: Boolean = false
): List<LintError> =
    listOf(this).lint(lintedFilePath, text, userData, script)

/**
 * Runs lint for a list of rules on a piece of code. Rules should be specified in exact order as they will be executed
 * by the production code. Its primary usage is testing rules which are closely related like wrapping and indent rules.
 */
@Suppress("DeprecatedCallableAddReplaceWith")
@Deprecated(
    message = "Marked for removal in Ktlint 0.46. If parameter 'userData' contains EditorConfig properties, then " +
        "specify these properties via parameter 'EditorConfigOverride.'",
    level = DeprecationLevel.WARNING
)
public fun List<Rule>.lint(
    lintedFilePath: String?,
    text: String,
    userData: Map<String, String> = emptyMap(),
    script: Boolean = false
): List<LintError> {
    val res = ArrayList<LintError>()
    KtLint.lint(
        // TODO: Replace with ExperimentalParams once it is no longer annotated with FeatureInAlphaState or
        //  FeatureInBetaState as this would require all unit tests to be marked with this annotation as well.
        KtLint.Params(
            fileName = lintedFilePath,
            text = text,
            ruleSets = this.toRuleSets(),
            userData = userData,
            script = script,
            cb = { e, _ -> res.add(e) }
        )
    )
    return res
}

// TODO: Remove method once the default value for parameter editorConfigOverride is set in function
//  List<Rule>.lint(String?, String, EditorConfigOverride, Map<String, String>, Boolean).
@FeatureInAlphaState
public fun Rule.lint(
    code: String,
    editorConfigOverride: EditorConfigOverride
): List<LintError> = listOf(this).lint(null, code, editorConfigOverride, emptyMap(), false)

// TODO: Remove method once the default value for parameter editorConfigOverride is set in function
//  List<Rule>.lint(String?, String, EditorConfigOverride, Map<String, String>, Boolean).
@FeatureInAlphaState
public fun List<Rule>.lint(
    code: String,
    editorConfigOverride: EditorConfigOverride
): List<LintError> = lint(null, code, editorConfigOverride, emptyMap(), false)

@FeatureInAlphaState
public fun List<Rule>.lint(
    lintedFilePath: String? = null,
    text: String,
    // TODO: Set default value EditorConfigOverride.emptyEditorConfigOverride once the EditorConfigOverride is no longer
    //  annotated with FeatureInAlphaState or FeatureInBetaState and function
    //  List<Rule>.lint(String?, String, Map<String, String>, Boolean> is removed.
    editorConfigOverride: EditorConfigOverride,
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
        experimentalParams,
        VisitorProvider(
            ruleSets = experimentalParams.ruleSets,
            debug = experimentalParams.debug
        )
    )
    return res
}

/**
 * Runs format for a single rule on a piece of code.
 */
// TODO: Remove once the deprecated function "Rule.format(String, (e: LintError, corrected: Boolean) -> Unit, boolean)"
//  is removed. This function is added to prevent deprecation warning on call where no userData parameter is specified.
public fun Rule.format(
    text: String,
    cb: (e: LintError, corrected: Boolean) -> Unit = { _, _ -> },
    script: Boolean = false
): String = format(null, text, emptyMap(), cb, script)

/**
 * Runs format for a single rule on a piece of code.
 */
@Suppress("DeprecatedCallableAddReplaceWith")
@Deprecated(
    message = "Marked for removal in Ktlint 0.46. If parameter 'userData' contains EditorConfig properties, then " +
        "specify these properties via parameter 'EditorConfigOverride.'",
    level = DeprecationLevel.WARNING
)
public fun Rule.format(
    text: String,
    // Default value is removed so that the deprecation warning will only be shown when the userData parameter is
    // actually specified.
    userData: Map<String, String>,
    cb: (e: LintError, corrected: Boolean) -> Unit = { _, _ -> },
    script: Boolean = false
): String = format(null, text, userData, cb, script)

/**
 * Runs format for a list of rules on a piece of code. Rules should be specified in exact order as they will be executed
 * by the production code.  Its primary usage is testing rules which are closely related like wrapping and indent rules.
 */
// TODO: Remove once the deprecated function "Rule.format(String, Map<String, String>, (e: LintError, corrected: Boolean) -> Unit, boolean)"
//  is removed. This function is added to prevent deprecation warning on call where no userData parameter is specified.
public fun List<Rule>.format(
    text: String,
    cb: (e: LintError, corrected: Boolean) -> Unit = { _, _ -> },
    script: Boolean = false
): String = format(null, text, emptyMap(), cb, script)

@Suppress("DeprecatedCallableAddReplaceWith")
@Deprecated(
    message = "Marked for removal in Ktlint 0.46. If parameter 'userData' contains EditorConfig properties, then " +
        "specify these properties via parameter 'EditorConfigOverride.'",
    level = DeprecationLevel.WARNING
)
public fun List<Rule>.format(
    text: String,
    // Default value is removed so that the deprecation warning will only be shown when the userData parameter is
    // actually specified.
    userData: Map<String, String>,
    cb: (e: LintError, corrected: Boolean) -> Unit = { _, _ -> },
    script: Boolean = false
): String = format(null, text, userData, cb, script)

/**
 * Runs format for a single rule on a piece of code.
 */
// TODO: Remove once the deprecated function "Rule.format(String?, String, (e: LintError, corrected: Boolean) -> Unit, boolean)"
//  is removed. This function is added to prevent deprecation warning on call where no userData parameter is specified.
public fun Rule.format(
    lintedFilePath: String?,
    text: String,
    cb: (e: LintError, corrected: Boolean) -> Unit = { _, _ -> },
    script: Boolean = false
): String = listOf(this).format(lintedFilePath, text, emptyMap(), cb, script)

@Suppress("DeprecatedCallableAddReplaceWith")
@Deprecated(
    message = "Marked for removal in Ktlint 0.46. If parameter 'userData' contains EditorConfig properties, then " +
        "specify these properties via parameter 'EditorConfigOverride.'",
    level = DeprecationLevel.WARNING
)
public fun Rule.format(
    lintedFilePath: String?,
    text: String,
    // Default value is removed so that the deprecation warning will only be shown when the userData parameter is
    // actually specified.
    userData: Map<String, String>,
    cb: (e: LintError, corrected: Boolean) -> Unit = { _, _ -> },
    script: Boolean = false
): String = listOf(this).format(lintedFilePath, text, userData, cb, script)

/**
 * Runs format for a list of rules on a piece of code. Rules should be specified in exact order as they will be executed
 * by the production code.  Its primary usage is testing rules which are closely related like wrapping and indent rules.
 */
@Suppress("DeprecatedCallableAddReplaceWith")
@Deprecated(
    message = "Marked for removal in Ktlint 0.46. If parameter 'userData' contains EditorConfig properties, then " +
        "specify these properties via parameter 'EditorConfigOverride.'",
    level = DeprecationLevel.WARNING
)
public fun List<Rule>.format(
    lintedFilePath: String?,
    text: String,
    userData: Map<String, String> = emptyMap(),
    cb: (e: LintError, corrected: Boolean) -> Unit = { _, _ -> },
    script: Boolean = false
): String {
    return KtLint.format(
        // TODO: Replace with ExperimentalParams once it is no longer annotated with FeatureInAlphaState or
        //  FeatureInBetaState as this would require all unit tests to be marked with this annotation as well.
        KtLint.Params(
            fileName = lintedFilePath,
            text = text,
            ruleSets = this.toRuleSets(),
            userData = userData,
            script = script,
            cb = cb
        )
    )
}

// TODO: Remove method once the default value for parameter editorConfigOverride is set in function
//  List<Rule>.format(String?, String, EditorConfigOverride, Map<String, String>, Boolean).
@FeatureInAlphaState
public fun Rule.format(
    text: String,
    editorConfigOverride: EditorConfigOverride
): String = listOf(this).format(text = text, editorConfigOverride = editorConfigOverride)

// TODO: Remove method once the default value for parameter editorConfigOverride is set in function
//  List<Rule>.format(String?, String, EditorConfigOverride, Map<String, String>, Boolean).
@FeatureInAlphaState
public fun List<Rule>.format(
    text: String,
    editorConfigOverride: EditorConfigOverride,
    cb: (e: LintError, corrected: Boolean) -> Unit = { _, _ -> }
): String = format(lintedFilePath = null, text = text, editorConfigOverride = editorConfigOverride, cb = cb)

@FeatureInAlphaState
public fun List<Rule>.format(
    lintedFilePath: String?,
    text: String,
    // TODO: Set default value EditorConfigOverride.emptyEditorConfigOverride once the EditorConfigOverride is no longer
    //  annotated with FeatureInAlphaState or FeatureInBetaState and function
    //  List<Rule>.lint(String?, String, Map<String, String>, Boolean> is removed.
    editorConfigOverride: EditorConfigOverride,
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
    return KtLint.format(
        experimentalParams,
        VisitorProvider(
            ruleSets = experimentalParams.ruleSets,
            debug = experimentalParams.debug
        )
    )
}

public fun Rule.diffFileLint(
    path: String
): String = listOf(this).diffFileLint(path, emptyMap())

public fun List<Rule>.diffFileLint(
    path: String
): String = diffFileLint(path, emptyMap())

@Suppress("DeprecatedCallableAddReplaceWith")
@Deprecated(
    message = "Marked for removal in Ktlint 0.46. If parameter 'userData' contains EditorConfig properties, then " +
        "specify these properties via parameter 'EditorConfigOverride.'",
    level = DeprecationLevel.WARNING
)
public fun List<Rule>.diffFileLint(
    path: String,
    userData: Map<String, String> = emptyMap()
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
            line.trimMargin("// ").split(':', limit = 4).let { expectation ->
                if (this.size > 1 && expectation.size != 4) {
                    throw RuntimeException("$path expectation must be a quartet <line>:<column>:<rule>:<message> because diffFileLint is running on multiple rules")
                    // " (<message> is not allowed to contain \":\")")
                } else if (expectation.size < 3 || expectation.size > 4) {
                    throw RuntimeException("$path expectation must be a triple <line>:<column>:<message> or quartet <line>:<column>:<rule>:<message>")
                    // " (<message> is not allowed to contain \":\")")
                }
                val message = expectation.last()
                val detail = message.removeSuffix(" (cannot be auto-corrected)")
                val ruleId = if (expectation.size == 4) {
                    expectation[2]
                } else {
                    this.first().id
                }
                LintError(expectation[0].toInt(), expectation[1].toInt(), ruleId, detail, message == detail)
            }
        }
    }
    val actual = lint(input, userData, script = true)
    val str = { err: LintError ->
        val correctionStatus = if (!err.canBeAutoCorrected) " (cannot be auto-corrected)" else ""
        "${err.line}:${err.col}:${err.detail}${err.ruleId}$correctionStatus"
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

public fun Rule.diffFileFormat(
    srcPath: String,
    expectedPath: String
): String = listOf(this).diffFileFormat(srcPath, expectedPath, emptyMap())

public fun List<Rule>.diffFileFormat(
    srcPath: String,
    expectedPath: String
): String = diffFileFormat(srcPath, expectedPath, emptyMap())

@Suppress("DeprecatedCallableAddReplaceWith")
@Deprecated(
    message = "Marked for removal in Ktlint 0.46. If parameter 'userData' contains EditorConfig properties, then " +
        "specify these properties via parameter 'EditorConfigOverride.'",
    level = DeprecationLevel.WARNING
)
public fun List<Rule>.diffFileFormat(
    srcPath: String,
    expectedPath: String,
    userData: Map<String, String> = emptyMap()
): String {
    val actual = format(getResourceAsText(srcPath), userData, script = true).split('\n')
    val expected = getResourceAsText(expectedPath).split('\n')
    val diff =
        generateUnifiedDiff(expectedPath, "output", expected, diff(expected, actual), expected.size + actual.size)
            .joinToString("\n")
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

/**
 * Alternative to [diffFileFormat]. Depending on your personal favor it might be more insightful whenever a test is
 * failing. Currently it is offered as utility so it can be used during development.
 *
 * To be used as:
 *
 *     @Test
 *     fun testFormatRawStringTrimIndent() {
 *         IndentationRule().assertThatFileFormat(
 *             "spec/indent/format-raw-string-trim-indent.kt.spec",
 *             "spec/indent/format-raw-string-trim-indent-expected.kt.spec"
 *         )
 *     }
 */
@Suppress("DeprecatedCallableAddReplaceWith")
@Deprecated(
    message = "Marked for removal in Ktlint 0.46. If parameter 'userData' contains EditorConfig properties, then " +
        "specify these properties via parameter 'EditorConfigOverride.'",
    level = DeprecationLevel.WARNING
)
public fun Rule.assertThatFileFormat(
    srcPath: String,
    expectedPath: String,
    userData: Map<String, String> = emptyMap()
) {
    val actual = format(getResourceAsText(srcPath), userData, script = true).split('\n')
    val expected = getResourceAsText(expectedPath).split('\n')
    assertThat(actual).isEqualTo(expected)
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
