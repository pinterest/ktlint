package com.pinterest.ktlint.test

import com.pinterest.ktlint.core.KTLINT_UNIT_TEST_DUMP_AST
import com.pinterest.ktlint.core.KTLINT_UNIT_TEST_ON_PROPERTY
import com.pinterest.ktlint.core.KtLint
import com.pinterest.ktlint.core.LintError
import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.RuleSet
import com.pinterest.ktlint.core.initKtLintKLogger
import com.pinterest.ruleset.test.DumpASTRule
import mu.KotlinLogging
import org.assertj.core.api.Assertions
import org.assertj.core.util.diff.DiffUtils.diff
import org.assertj.core.util.diff.DiffUtils.generateUnifiedDiff
import org.jetbrains.kotlin.utils.addToStdlib.ifTrue

private val logger = KotlinLogging.logger {}.initKtLintKLogger()

private fun Rule.toRuleSets(): List<RuleSet> {
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
    return listOfNotNull(dumpAstRuleSet, RuleSet("standard", this))
}

public fun Rule.lint(
    text: String,
    userData: Map<String, String> = emptyMap(),
    script: Boolean = false
): List<LintError> = lint(null, text, userData, script)

public fun Rule.lint(
    lintedFilePath: String?,
    text: String,
    userData: Map<String, String> = emptyMap(),
    script: Boolean = false
): List<LintError> {
    val res = ArrayList<LintError>()
    KtLint.lint(
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

public fun Rule.format(
    text: String,
    userData: Map<String, String> = emptyMap(),
    cb: (e: LintError, corrected: Boolean) -> Unit = { _, _ -> },
    script: Boolean = false
): String = format(null, text, userData, cb, script)

public fun Rule.format(
    lintedFilePath: String?,
    text: String,
    userData: Map<String, String> = emptyMap(),
    cb: (e: LintError, corrected: Boolean) -> Unit = { _, _ -> },
    script: Boolean = false
): String = KtLint.format(
    KtLint.Params(
        fileName = lintedFilePath,
        text = text,
        ruleSets = this.toRuleSets(),
        userData = userData,
        script = script,
        cb = cb
    )
)

public fun Rule.diffFileLint(
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
    val actual = lint(input, userData, script = true)
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
public fun Rule.assertThatFileFormat(
    srcPath: String,
    expectedPath: String,
    userData: Map<String, String> = emptyMap()
) {
    val actual = format(getResourceAsText(srcPath), userData, script = true).split('\n')
    val expected = getResourceAsText(expectedPath).split('\n')
    Assertions.assertThat(actual).isEqualTo(expected)
}

private fun getResourceAsText(path: String) =
    (ClassLoader.getSystemClassLoader().getResourceAsStream(path) ?: throw RuntimeException("$path not found"))
        .bufferedReader()
        .readText()
