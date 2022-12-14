package com.pinterest.ktlint.reporter.plain

import com.pinterest.ktlint.core.LintError
import com.pinterest.ktlint.core.Reporter
import java.io.File
import java.io.PrintStream
import java.util.concurrent.ConcurrentHashMap

/**
 * Reports [LintError]s which have not been autocorrected
 */
public class PlainReporter(
    private val out: PrintStream,
    private val groupByFile: Boolean = false,
    private val shouldColorOutput: Boolean = false,
    private val outputColor: Color = Color.DARK_GRAY,
    private val pad: Boolean = false,
) : Reporter {

    private val acc = ConcurrentHashMap<String, MutableList<LintError>>()
    private val ruleViolationCount = ConcurrentHashMap<String, Long>()

    override fun onLintError(file: String, err: LintError, corrected: Boolean) {
        if (!corrected) {
            if (groupByFile) {
                acc.getOrPut(file) { ArrayList() }.add(err)
            } else {
                val column =
                    if (pad) {
                        String.format("%-4s", err.col)
                    } else {
                        err.col
                    }
                out.println(
                    "${colorFileName(file)}${":".colored()}${err.line}${":$column:".colored()} ${err.detail} ${"(${err.ruleId})".colored()}",
                )
            }
            ruleViolationCount
                .merge(err.causedBy(), 1) { previousValue, _ ->
                    previousValue + 1
                }
        }
    }

    override fun after(file: String) {
        if (groupByFile) {
            val errList = acc[file] ?: return
            out.println(colorFileName(file))
            for ((line, col, ruleId, detail) in errList) {
                val column = if (pad) {
                    String.format("%-3s", col)
                } else {
                    col
                }
                out.println(
                    "  $line${":$column".colored()} $detail ${"($ruleId)".colored()}",
                )
            }
        }
    }

    override fun afterAll() {
        if (ruleViolationCount.isNotEmpty()) {
            out.println("")
            ruleViolationCount.printSummary("Summary error count (descending) by rule:")
        }
    }

    private fun ConcurrentHashMap<String, Long>.printSummary(header: String) {
        out.println(header)
        toList()
            .sortedWith(COUNT_DESC_AND_RULE_ID_ASC_COMPARATOR)
            .map { out.println("  ${it.first}: ${it.second}") }
    }

    private fun colorFileName(fileName: String): String {
        val name = fileName.substringAfterLast(File.separator)
        return fileName.substring(0, fileName.length - name.length).colored() + name
    }

    private fun String.colored() =
        if (shouldColorOutput) {
            this.color(outputColor)
        } else {
            this
        }

    private fun LintError.causedBy() =
        when {
            ruleId.isNotEmpty() -> ruleId
            detail.startsWith(NOT_A_VALID_KOTLIN_FILE) -> NOT_A_VALID_KOTLIN_FILE
            else -> "Unknown"
        }

    private companion object {
        val COUNT_DESC_AND_RULE_ID_ASC_COMPARATOR =
            kotlin
                .Comparator<Pair<String, Long>> { left, right ->
                    compareValuesBy(left, right) { it.second }
                }.reversed()
                .thenComparator { left, right ->
                    compareValuesBy(left, right) { it.first }
                }

        const val NOT_A_VALID_KOTLIN_FILE = "Not a valid Kotlin file"
    }
}

internal fun String.color(foreground: Color): String =
    "\u001B[${foreground.code}m$this\u001B[0m"
