package com.pinterest.ktlint.cli.reporter.format

import com.pinterest.ktlint.cli.reporter.core.api.ReporterProviderV2
import java.io.PrintStream

public class FormatReporterProvider : ReporterProviderV2<FormatReporter> {
    override val id: String = "format"

    override fun get(
        out: PrintStream,
        opt: Map<String, String>,
    ): FormatReporter =
        FormatReporter(
            out,
            format =
                opt
                    .getOrElse("format") { throw IllegalArgumentException("Format is not specified in config options") }
                    .toBooleanStrict(),
            shouldColorOutput = opt["color"]?.emptyOrTrue() ?: false,
            outputColor = getColor(opt["color_name"]),
        )

    private fun String.emptyOrTrue() = this == "" || this == "true"

    private fun getColor(color: String?): Color =
        Color.entries.firstOrNull {
            it.name == color
        } ?: throw IllegalArgumentException("Invalid color parameter.")
}
