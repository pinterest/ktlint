package com.pinterest.ktlint.cli.reporter.plain

import com.pinterest.ktlint.cli.reporter.core.api.ReporterProviderV2
import java.io.PrintStream

public class PlainReporterProvider : ReporterProviderV2<PlainReporter> {
    override val id: String = "plain"

    override fun get(
        out: PrintStream,
        opt: Map<String, String>,
    ): PlainReporter =
        PlainReporter(
            out,
            groupByFile = opt["group_by_file"]?.emptyOrTrue() ?: false,
            shouldColorOutput = opt["color"]?.emptyOrTrue() ?: false,
            outputColor = getColor(opt["color_name"]),
            pad = opt["pad"]?.emptyOrTrue() ?: false,
        )

    private fun String.emptyOrTrue() = this == "" || this == "true"

    private fun getColor(colorInput: String?): Color =
        Color.entries.firstOrNull {
            it.name == colorInput
        } ?: throw IllegalArgumentException("Invalid color parameter.")
}
