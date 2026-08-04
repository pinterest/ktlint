package io.github.ktlint.core.cli.reporter.checkstyle

import io.github.ktlint.core.cli.reporter.core.api.ReporterProviderV2
import java.io.PrintStream

public class CheckStyleReporterProvider : ReporterProviderV2<CheckStyleReporter> {
    override val id: String = "checkstyle"

    override fun get(
        out: PrintStream,
        opt: Map<String, String>,
    ): CheckStyleReporter = CheckStyleReporter(out)
}
