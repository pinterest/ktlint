package com.pinterest.ktlint.cli.reporter.json

import com.pinterest.ktlint.cli.reporter.core.api.ReporterProviderV2
import java.io.PrintStream

public class JsonReporterProvider : ReporterProviderV2<JsonReporter> {
    override val id: String = "json"

    override fun get(
        out: PrintStream,
        opt: Map<String, String>,
    ): JsonReporter = JsonReporter(out)
}
