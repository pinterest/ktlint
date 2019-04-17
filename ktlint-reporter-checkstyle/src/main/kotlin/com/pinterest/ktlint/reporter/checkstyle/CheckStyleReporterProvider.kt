package com.pinterest.ktlint.reporter.checkstyle

import com.pinterest.ktlint.core.Reporter
import com.pinterest.ktlint.core.ReporterProvider
import java.io.PrintStream

class CheckStyleReporterProvider : ReporterProvider {
    override val id: String = "checkstyle"
    override fun get(out: PrintStream, opt: Map<String, String>): Reporter = CheckStyleReporter(out)
}
