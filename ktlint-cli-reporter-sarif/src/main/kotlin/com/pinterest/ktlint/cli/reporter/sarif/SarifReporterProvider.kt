package com.pinterest.ktlint.cli.reporter.sarif

import com.pinterest.ktlint.cli.reporter.core.api.ReporterProviderV2
import java.io.PrintStream

public class SarifReporterProvider : ReporterProviderV2<SarifReporter> {
    override val id: String = "sarif"

    override fun get(
        out: PrintStream,
        opt: Map<String, String>,
    ): SarifReporter = SarifReporter(out)
}
