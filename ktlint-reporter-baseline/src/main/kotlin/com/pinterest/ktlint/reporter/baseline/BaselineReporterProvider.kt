package com.pinterest.ktlint.reporter.baseline

import com.pinterest.ktlint.core.ReporterProvider
import java.io.PrintStream

public class BaselineReporterProvider : ReporterProvider<BaselineReporter> {
    override val id: String = "baseline"
    override fun get(out: PrintStream, opt: Map<String, String>): BaselineReporter = BaselineReporter(out)
}
