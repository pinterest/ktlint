package com.pinterest.ktlint.reporter.baseline

import com.pinterest.ktlint.core.LintError
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.nio.file.Paths
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class BaselineReporterTest {

    @Test
    fun testReportGeneration() {
        val basePath = Paths.get("").toAbsolutePath()
        val out = ByteArrayOutputStream()
        val reporter = BaselineReporter(PrintStream(out, true))
        reporter.onLintError(
            "$basePath/one-fixed-and-one-not.kt",
            LintError(1, 1, "rule-1", "<\"&'>"),
            false,
        )
        reporter.onLintError(
            "$basePath/one-fixed-and-one-not.kt",
            LintError(2, 1, "rule-2", "And if you see my friend"),
            true,
        )

        reporter.onLintError(
            "$basePath/two-not-fixed.kt",
            LintError(1, 10, "rule-1", "I thought I would again"),
            false,
        )
        reporter.onLintError(
            "$basePath/two-not-fixed.kt",
            LintError(2, 20, "rule-2", "A single thin straight line"),
            false,
        )

        reporter.onLintError(
            "$basePath/all-corrected.kt",
            LintError(1, 1, "rule-1", "I thought we had more time"),
            true,
        )
        reporter.afterAll()
        assertThat(String(out.toByteArray())).isEqualTo(
            """
            <?xml version="1.0" encoding="utf-8"?>
            <baseline version="1.0">
                <file name="one-fixed-and-one-not.kt">
                    <error line="1" column="1" source="rule-1" />
                </file>
                <file name="two-not-fixed.kt">
                    <error line="1" column="10" source="rule-1" />
                    <error line="2" column="20" source="rule-2" />
                </file>
            </baseline>

            """.trimIndent().replace("\n", System.lineSeparator()),
        )
    }
}
