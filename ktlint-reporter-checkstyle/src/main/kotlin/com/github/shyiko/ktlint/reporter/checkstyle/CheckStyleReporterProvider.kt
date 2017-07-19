package com.github.shyiko.ktlint.reporter.checkstyle

import com.github.shyiko.ktlint.core.Reporter
import com.github.shyiko.ktlint.core.ReporterProvider
import java.io.PrintStream

class CheckStyleReporterProvider : ReporterProvider {
    override val id: String = "checkstyle"
    override fun get(out: PrintStream, opt: Map<String, String>): Reporter = CheckStyleReporter(out)
}
