package com.pinterest.ktlint.reporter.format

import com.pinterest.ktlint.core.ReporterProvider
import com.pinterest.ktlint.reporter.format.internal.Color
import java.io.PrintStream

public class FormatReporterProvider : ReporterProvider<FormatReporter> {
    override val id: String = "format"

    override fun get(out: PrintStream, opt: Map<String, String>): FormatReporter =
        FormatReporter(
            out,
            format = opt
                .getOrElse("format") { throw IllegalArgumentException("Format is not specified in config options") }
                .toBooleanStrict(),
            shouldColorOutput = opt["color"]?.emptyOrTrue() ?: false,
            outputColor = getColor(opt["color_name"])
        )

    private fun String.emptyOrTrue() = this == "" || this == "true"

    private fun getColor(color: String?): Color {
        return Color.values().firstOrNull { it.name == color } ?: throw IllegalArgumentException("Invalid color parameter.")
    }
}
