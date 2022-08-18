package com.pinterest.ktlint.reporter.plain

import com.pinterest.ktlint.core.ReporterProvider
import com.pinterest.ktlint.reporter.plain.internal.Color
import java.io.PrintStream

public class PlainReporterProvider : ReporterProvider<PlainReporter> {

    override val id: String = "plain"

    override fun get(out: PrintStream, opt: Map<String, String>): PlainReporter =
        PlainReporter(
            out,
            verbose = opt["verbose"]?.emptyOrTrue() ?: false,
            groupByFile = opt["group_by_file"]?.emptyOrTrue() ?: false,
            shouldColorOutput = opt["color"]?.emptyOrTrue() ?: false,
            outputColor = getColor(opt["color_name"]),
            pad = opt["pad"]?.emptyOrTrue() ?: false,
        )

    private fun String.emptyOrTrue() = this == "" || this == "true"

    private fun getColor(colorInput: String?): Color {
        return Color.values().firstOrNull { it.name == colorInput } ?: throw IllegalArgumentException("Invalid color parameter.")
    }
}
