package io.github.ktlint.core.cli.reporter.baseline

import io.github.ktlint.core.cli.reporter.core.api.ReporterProviderV2
import java.io.PrintStream

public class BaselineReporterProvider : ReporterProviderV2<BaselineReporter> {
    override val id: String = "baseline"

    override fun get(
        out: PrintStream,
        opt: Map<String, String>,
    ): BaselineReporter = BaselineReporter(out)
}
