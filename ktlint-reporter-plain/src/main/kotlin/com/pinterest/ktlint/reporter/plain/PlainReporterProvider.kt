package com.pinterest.ktlint.reporter.plain

import com.pinterest.ktlint.core.Reporter
import com.pinterest.ktlint.core.ReporterProvider
import com.pinterest.ktlint.reporter.plain.internal.Color
import java.io.PrintStream

class PlainReporterProvider : ReporterProvider {

    override val id: String = "plain"

    override fun get(out: PrintStream, opt: Map<String, String>): Reporter =
        PlainReporter(
            out,
            verbose = opt["verbose"]?.emptyOrTrue() ?: false,
            groupByFile = opt["group_by_file"]?.emptyOrTrue() ?: false,
            shouldColorOutput = opt["color"]?.emptyOrTrue() ?: false,
            outputColor = opt["outputColor"]?.toColor() ?: Color.DARK_GRAY,
            pad = opt["pad"]?.emptyOrTrue() ?: false
        )

    private fun String.emptyOrTrue() = this == "" || this == "true"

    private fun String.toColor() = Color.values().firstOrNull { it.name == this } ?: Color.DARK_GRAY
}
