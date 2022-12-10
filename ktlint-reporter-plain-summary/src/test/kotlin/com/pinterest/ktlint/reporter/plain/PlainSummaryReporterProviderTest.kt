package com.pinterest.ktlint.reporter.plain

import java.io.PrintStream
import java.lang.System.out
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class PlainSummaryReporterProviderTest {
    @Test
    fun `Get a plain summary reporter then create it without exception`() {
        val plainSummaryReporter = PlainSummaryReporterProvider().get(
            out = PrintStream(out, true),
            opt = emptyMap(),
        )

        assertThat(plainSummaryReporter).isNotNull
    }
}
