package com.pinterest.ktlint.reporter.json

import com.pinterest.ktlint.core.Reporter
import com.pinterest.ktlint.core.ReporterProvider
import java.io.PrintStream

class JsonReporterProvider : ReporterProvider {
    override val id: String = "json"
    override fun get(out: PrintStream, opt: Map<String, String>): Reporter = JsonReporter(out)
}
