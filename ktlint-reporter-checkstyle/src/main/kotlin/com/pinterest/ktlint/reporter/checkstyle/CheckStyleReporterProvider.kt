package com.pinterest.ktlint.reporter.checkstyle

import com.pinterest.ktlint.core.ReporterProvider
import java.io.PrintStream

public class CheckStyleReporterProvider : ReporterProvider<CheckStyleReporter> {
    override val id: String = "checkstyle"
    override fun get(out: PrintStream, opt: Map<String, String>): CheckStyleReporter = CheckStyleReporter(out)
}
