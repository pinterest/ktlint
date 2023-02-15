package com.pinterest.ktlint.cli.reporter.plainsummary

import com.pinterest.ktlint.cli.reporter.core.api.KtlintCliError
import com.pinterest.ktlint.cli.reporter.core.api.KtlintCliError.Status.KOTLIN_PARSE_EXCEPTION
import com.pinterest.ktlint.cli.reporter.core.api.KtlintCliError.Status.KTLINT_RULE_ENGINE_EXCEPTION
import com.pinterest.ktlint.cli.reporter.core.api.ReporterV2
import java.io.PrintStream
import java.util.concurrent.ConcurrentHashMap

/**
 * Reports a summary (count per rule) of the [KtlintCliError]s which have been autocorrected and not been autocorrected.
 */
public class PlainSummaryReporter(
    private val out: PrintStream,
) : ReporterV2 {
    private val ruleViolationCountAutocorrected = ConcurrentHashMap<String, Long>()
    private val ruleViolationCountNoAutocorrection = ConcurrentHashMap<String, Long>()

    override fun onLintError(
        file: String,
        ktlintCliError: KtlintCliError,
    ) {
        if (ktlintCliError.status == KtlintCliError.Status.FORMAT_IS_AUTOCORRECTED) {
            ruleViolationCountAutocorrected
                .merge(ktlintCliError.ruleId, 1) { previousValue, _ ->
                    previousValue + 1
                }
        } else {
            ruleViolationCountNoAutocorrection
                .merge(ktlintCliError.causedBy(), 1) { previousValue, _ ->
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
