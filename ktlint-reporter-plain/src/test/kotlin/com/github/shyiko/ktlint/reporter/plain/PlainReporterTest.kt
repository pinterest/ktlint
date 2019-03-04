package com.github.shyiko.ktlint.reporter.plain

import com.github.shyiko.ktlint.core.LintError
import org.assertj.core.api.Assertions.assertThat
import org.testng.annotations.Test
import java.io.ByteArrayOutputStream
import java.io.PrintStream

class PlainReporterTest {

    @Test
    fun testReportGeneration() {
        val out = ByteArrayOutputStream()
        val reporter = PlainReporter(PrintStream(out, true))
        reporter.onLintError(
            "/one-fixed-and-one-not.kt",
            LintError(
                1, 1, "rule-1",
                "<\"&'>"
            ),
            false
        )
        reporter.onLintError(
            "/one-fixed-and-one-not.kt",
            LintError(
                2, 1, "rule-2",
                "And if you see my friend"
            ),
            true
        )

        reporter.onLintError(
            "/two-not-fixed.kt",
            LintError(
                1, 10, "rule-1",
                "I thought I would again"
            ),
            false
        )
        reporter.onLintError(
            "/two-not-fixed.kt",
            LintError(
                2, 20, "rule-2",
                "A single thin straight line"
            ),
            false
        )

        reporter.onLintError(
            "/all-corrected.kt",
            LintError(
                1, 1, "rule-1",
                "I thought we had more time"
            ),
            true
        )
        assertThat(String(out.toByteArray())).isEqualTo(
"""
/one-fixed-and-one-not.kt:1:1: <"&'>
/two-not-fixed.kt:1:10: I thought I would again
/two-not-fixed.kt:2:20: A single thin straight line
""".trimStart().replace("\n", System.lineSeparator())
        )
    }

    @Test
    fun testReportGenerationGroupedByFile() {
        val out = ByteArrayOutputStream()
        val reporter = PlainReporter(PrintStream(out, true), groupByFile = true)
        reporter.onLintError(
            "/one-fixed-and-one-not.kt",
            LintError(
                1, 1, "rule-1",
                "<\"&'>"
            ),
            false
        )
        reporter.onLintError(
            "/one-fixed-and-one-not.kt",
            LintError(
                2, 1, "rule-2",
                "And if you see my friend"
            ),
            true
        )

        reporter.onLintError(
            "/two-not-fixed.kt",
            LintError(
                1, 10, "rule-1",
                "I thought I would again"
            ),
            false
        )
        reporter.onLintError(
            "/two-not-fixed.kt",
            LintError(
                2, 20, "rule-2",
                "A single thin straight line"
            ),
            false
        )

        reporter.onLintError(
            "/all-corrected.kt",
            LintError(
                1, 1, "rule-1",
                "I thought we had more time"
            ),
            true
        )
        reporter.after("/one-fixed-and-one-not.kt")
        reporter.after("/two-not-fixed.kt")
        reporter.after("/all-corrected.kt")
        assertThat(String(out.toByteArray())).isEqualTo(
"""
/one-fixed-and-one-not.kt
  1:1 <"&'>
/two-not-fixed.kt
  1:10 I thought I would again
  2:20 A single thin straight line
""".trimStart().replace("\n", System.lineSeparator())
        )
    }
}
