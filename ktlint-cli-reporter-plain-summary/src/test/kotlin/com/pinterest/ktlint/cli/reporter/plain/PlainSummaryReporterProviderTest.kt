package com.pinterest.ktlint.cli.reporter.plain

import com.pinterest.ktlint.cli.reporter.plainsummary.PlainSummaryReporterProvider
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.io.PrintStream
import java.lang.System.out

class PlainSummaryReporterProviderTest {
    @Test
    fun `Get a plain summary reporter then create it without exception`() {
        val plainSummaryReporter =
            PlainSummaryReporterProvider().get(
                out = PrintStream(out, true),
                opt = emptyMap(),
            )

        assertThat(plainSummaryReporter).isNotNull
    }
}
