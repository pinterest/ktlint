package com.pinterest.ktlint.reporter.plain

import com.pinterest.ktlint.core.ReporterProvider
import java.io.PrintStream

public class PlainSummaryReporterProvider : ReporterProvider<PlainSummaryReporter> {
    override val id: String = "plain-summary"

    override fun get(out: PrintStream, opt: Map<String, String>): PlainSummaryReporter =
        PlainSummaryReporter(out)
}
