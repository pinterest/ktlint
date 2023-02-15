package com.pinterest.ktlint.cli.reporter.plainsummary

import com.pinterest.ktlint.cli.reporter.core.api.ReporterProviderV2
import java.io.PrintStream

public class PlainSummaryReporterProvider : ReporterProviderV2<PlainSummaryReporter> {
    override val id: String = "plain-summary"

    override fun get(
        out: PrintStream,
        opt: Map<String, String>,
    ): PlainSummaryReporter = PlainSummaryReporter(out)
}
