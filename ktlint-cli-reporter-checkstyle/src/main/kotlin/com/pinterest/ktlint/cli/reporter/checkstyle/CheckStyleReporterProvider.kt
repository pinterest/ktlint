package com.pinterest.ktlint.cli.reporter.checkstyle

import com.pinterest.ktlint.cli.reporter.core.api.ReporterProviderV2
import java.io.PrintStream

public class CheckStyleReporterProvider : ReporterProviderV2<CheckStyleReporter> {
    override val id: String = "checkstyle"

    override fun get(
        out: PrintStream,
        opt: Map<String, String>,
    ): CheckStyleReporter = CheckStyleReporter(out)
}
