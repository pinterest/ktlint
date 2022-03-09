package com.pinterest.ktlint.reporter.sarif

import com.pinterest.ktlint.core.ReporterProvider
import java.io.PrintStream

public class SarifReporterProvider : ReporterProvider<SarifReporter> {

    override val id: String = "sarif"

    override fun get(out: PrintStream, opt: Map<String, String>): SarifReporter = SarifReporter(out)
}
