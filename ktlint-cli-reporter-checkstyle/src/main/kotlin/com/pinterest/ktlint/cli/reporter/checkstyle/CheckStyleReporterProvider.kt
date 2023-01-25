package com.pinterest.ktlint.cli.reporter.checkstyle

import com.pinterest.ktlint.cli.reporter.core.api.ReporterProvider
import java.io.PrintStream

public class CheckStyleReporterProvider : ReporterProvider<CheckStyleReporter> {
    override val id: String = "checkstyle"
    override fun get(
        out: PrintStream,
        opt: Map<String, String>,
    ): CheckStyleReporter = CheckStyleReporter(out)
}
