package com.pinterest.ktlint.cli.reporter.baseline

import com.pinterest.ktlint.cli.reporter.core.api.ReporterProvider
import java.io.PrintStream

public class BaselineReporterProvider : ReporterProvider<BaselineReporter> {
    override val id: String = "baseline"
    override fun get(
        out: PrintStream,
        opt: Map<String, String>,
    ): BaselineReporter = BaselineReporter(out)
}
