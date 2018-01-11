package com.github.shyiko.ktlint.reporter.plain

import com.github.shyiko.ktlint.core.Reporter
import com.github.shyiko.ktlint.core.ReporterProvider
import java.io.PrintStream

class PlainReporterProvider : ReporterProvider {

    override val id: String = "plain"

    override fun get(out: PrintStream, opt: Map<String, String>): Reporter =
        PlainReporter(
            out,
            verbose = opt["verbose"]?.emptyOrTrue() ?: false,
            groupByFile = opt["group_by_file"]?.emptyOrTrue() ?: false,
            color = opt["color"]?.emptyOrTrue() ?: false,
            pad = opt["pad"]?.emptyOrTrue() ?: false
        )

    private fun String.emptyOrTrue() = this == "" || this == "true"
}
