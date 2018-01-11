package com.github.shyiko.ktlint.reporter.json

import com.github.shyiko.ktlint.core.Reporter
import com.github.shyiko.ktlint.core.ReporterProvider
import java.io.PrintStream

class JsonReporterProvider : ReporterProvider {
    override val id: String = "json"
    override fun get(out: PrintStream, opt: Map<String, String>): Reporter = JsonReporter(out)
}
