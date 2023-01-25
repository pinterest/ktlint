package com.pinterest.ktlint.cli.reporter.plainsummary

import com.pinterest.ktlint.cli.reporter.core.api.ReporterProvider
import java.io.PrintStream

public class PlainSummaryReporterProvider : ReporterProvider<PlainSummaryReporter> {
    override val id: String = "plain-summary"

    override fun get(
        out: PrintStream,
        opt: Map<String, String>,
    ): PlainSummaryReporter = PlainSummaryReporter(out)
}
