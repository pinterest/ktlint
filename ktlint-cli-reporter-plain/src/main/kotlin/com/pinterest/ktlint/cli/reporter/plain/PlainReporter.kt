package com.pinterest.ktlint.cli.reporter.plain

import com.pinterest.ktlint.cli.reporter.core.api.KtlintCliError
import com.pinterest.ktlint.cli.reporter.core.api.KtlintCliError.Status.FORMAT_IS_AUTOCORRECTED
import com.pinterest.ktlint.cli.reporter.core.api.KtlintCliError.Status.KOTLIN_PARSE_EXCEPTION
import com.pinterest.ktlint.cli.reporter.core.api.KtlintCliError.Status.KTLINT_RULE_ENGINE_EXCEPTION
import com.pinterest.ktlint.cli.reporter.core.api.ReporterV2
import java.io.File
import java.io.PrintStream
import java.util.concurrent.ConcurrentHashMap

/**
 * Reports [KtlintCliError]s which have not been autocorrected and pints a summary (count per rule) of the violations found.
 */
public class PlainReporter(
    private val out: PrintStream,
    private val groupByFile: Boolean = false,
    private val shouldColorOutput: Boolean = false,
    private val outputColor: Color = Color.DARK_GRAY,
    private val pad: Boolean = false,
) : ReporterV2 {
    private val acc = ConcurrentHashMap<String, MutableList<KtlintCliError>>()
    private val ruleViolationCount = ConcurrentHashMap<String, Long>()

    override fun onLintError(
        file: String,
        ktlintCliError: KtlintCliError,
    ) {
        if (ktlintCliError.status != FORMAT_IS_AUTOCORRECTED) {
            if (groupByFile) {
                acc.getOrPut(file) { ArrayList() }.add(ktlintCliError)
            } else {
                val column =
                    if (pad) {
                        String.format("%-4s", ktlintCliError.col)
                    } else {
                        ktlintCliError.col
                    }
                out.println(
                    "${colorFileName(file)}${":".colored()}${ktlintCliError.line}${":$column:".colored()} " +
                        "${ktlintCliError.detail} ${"(${ktlintCliError.ruleId})".colored()}",
                )
            }
            ruleViolationCount
                .merge(ktlintCliError.causedBy(), 1) { previousValue, _ ->
                    previousValue + 1
                }
        }
    }

    override fun after(file: String) {
        if (groupByFile) {
            val errList = acc[file] ?: return
            out.println(colorFileName(file))
            for (err in errList) {
                with(err) {
                    val column =
                        if (pad) {
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

    private fun KtlintCliError.causedBy() =
        when (status) {
            KOTLIN_PARSE_EXCEPTION -> KOTLIN_PARSE_EXCEPTION_MESSAGE
            KTLINT_RULE_ENGINE_EXCEPTION -> KTLINT_RULE_ENGINE_EXCEPTION_MESSAGE
            else -> ruleId.ifEmpty { UNKNOWN_CAUSE_MESSAGE }
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

        const val KOTLIN_PARSE_EXCEPTION_MESSAGE = "Not a valid Kotlin file"
        const val KTLINT_RULE_ENGINE_EXCEPTION_MESSAGE = "An internal error occurred in the Ktlint Rule Engine"
        const val UNKNOWN_CAUSE_MESSAGE = "Unknown"
    }
}

internal fun String.color(foreground: Color): String = "\u001B[${foreground.code}m$this\u001B[0m"
