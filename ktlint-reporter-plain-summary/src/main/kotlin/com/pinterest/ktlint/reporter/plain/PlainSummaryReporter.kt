package com.pinterest.ktlint.reporter.plain

import com.pinterest.ktlint.core.LintError
import com.pinterest.ktlint.core.Reporter
import java.io.PrintStream
import java.util.concurrent.ConcurrentHashMap

/**
 * Reports a summary, e.g. a count of [LintError]s per rule
 */
public class PlainSummaryReporter(
    private val out: PrintStream,
) : Reporter {
    private val ruleViolationCountAutocorrected = ConcurrentHashMap<String, Long>()
    private val ruleViolationCountNoAutocorrection = ConcurrentHashMap<String, Long>()

    override fun onLintError(file: String, err: LintError, corrected: Boolean) {
        if (corrected) {
            ruleViolationCountAutocorrected
                .merge(err.ruleId, 1) { previousValue, _ ->
                    previousValue + 1
                }
        } else {
            ruleViolationCountNoAutocorrection
                .merge(err.causedBy(), 1) { previousValue, _ ->
                    previousValue + 1
                }
        }
    }

    override fun afterAll() {
        if (ruleViolationCountAutocorrected.isNotEmpty()) {
            ruleViolationCountAutocorrected.printSummary("Count (descending) of autocorrected errors by rule:")
        }
        if (ruleViolationCountNoAutocorrection.isNotEmpty()) {
            if (ruleViolationCountAutocorrected.isNotEmpty()) {
                out.println("")
            }
            ruleViolationCountNoAutocorrection.printSummary("Count (descending) of errors not autocorrected by rule:")
        }
    }

    private fun ConcurrentHashMap<String, Long>.printSummary(header: String) {
        out.println(header)
        toList()
            .sortedWith(COUNT_DESC_AND_RULE_ID_ASC_COMPARATOR)
            .map { out.println("  ${it.first}: ${it.second}") }
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
