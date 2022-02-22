package com.pinterest.ktlint.reporter.json

import com.pinterest.ktlint.core.ReporterProvider
import java.io.PrintStream

public class JsonReporterProvider : ReporterProvider<JsonReporter> {
    override val id: String = "json"
    override fun get(out: PrintStream, opt: Map<String, String>): JsonReporter = JsonReporter(out)
}
