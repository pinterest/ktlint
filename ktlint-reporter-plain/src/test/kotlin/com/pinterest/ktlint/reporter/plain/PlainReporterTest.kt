package com.pinterest.ktlint.reporter.plain

import com.pinterest.ktlint.core.LintError
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class PlainReporterTest {
    @Test
    fun `Report normal rule violations`() {
        val out = ByteArrayOutputStream()
        val reporter = PlainReporter(PrintStream(out, true))
        reporter.onLintError(
            "file-1.kt",
            LintError(
                1,
                1,
                "rule-1",
                "description-error-at-position-1:1",
            ),
            false,
        )
        reporter.onLintError(
            "file-1.kt",
            LintError(
                2,
                1,
                "rule-2",
                "description-error-at-position-2:1",
            ),
            true,
        )

        reporter.onLintError(
            "file-2.kt",
            LintError(
                1,
                10,
                "rule-1",
                "description-error-at-position-1:10",
            ),
            false,
        )
        reporter.onLintError(
            "file-2.kt",
            LintError(
                2,
                20,
                "rule-2",
                "description-error-at-position-2:20",
            ),
            false,
        )

        reporter.onLintError(
            "file-3.kt",
            LintError(
                1,
                1,
                "rule-1",
                "description-error-at-position-1:1",
            ),
            true,
        )

        reporter.afterAll()

        assertThat(String(out.toByteArray())).isEqualTo(
            """
            |file-1.kt:1:1: description-error-at-position-1:1 (rule-1)
            |file-2.kt:1:10: description-error-at-position-1:10 (rule-1)
            |file-2.kt:2:20: description-error-at-position-2:20 (rule-2)
            |
            |Summary error count (descending) by rule:
            |  rule-1: 2
            |  rule-2: 1
            |
            """.trimMargin().replace("\n", System.lineSeparator()),
        )
    }

    @Test
    fun `Report other violations`() {
        val out = ByteArrayOutputStream()
        val reporter = PlainReporter(PrintStream(out, true))
        reporter.onLintError(
            "file-1.kt",
            LintError(
                18,
                51,
                "",
                "Not a valid Kotlin file (18:51 unexpected tokens (use ';' to separate expressions on the same line)) (cannot be auto-corrected) ()",
            ),
            false,
        )
        reporter.onLintError(
            "file-2.kt",
            LintError(
                18,
                51,
                "",
                "Not a valid Kotlin file (18:51 unexpected tokens (use ';' to separate expressions on the same line)) (cannot be auto-corrected) ()",
            ),
            false,
        )
        reporter.onLintError(
            "file-3.kt",
            LintError(
                18,
                51,
                "",
                "Something else",
            ),
            false,
        )
        reporter.afterAll()

        assertThat(String(out.toByteArray())).isEqualTo(
            """
            |file-1.kt:18:51: Not a valid Kotlin file (18:51 unexpected tokens (use ';' to separate expressions on the same line)) (cannot be auto-corrected) () ()
            |file-2.kt:18:51: Not a valid Kotlin file (18:51 unexpected tokens (use ';' to separate expressions on the same line)) (cannot be auto-corrected) () ()
            |file-3.kt:18:51: Something else ()
            |
            |Summary error count (descending) by rule:
            |  Not a valid Kotlin file: 2
            |  Unknown: 1
            |
            """.trimMargin().replace("\n", System.lineSeparator()),
        )
    }

    @Test
    fun testColoredOutput() {
        val out = ByteArrayOutputStream()
        val outputColor = Color.DARK_GRAY
        val reporter = PlainReporter(
            PrintStream(out, true),
            shouldColorOutput = true,
            outputColor = outputColor,
        )
        reporter.onLintError(
            File.separator + "one-fixed-and-one-not.kt",
            LintError(
                1,
                1,
                "rule-1",
                "<\"&'>",
            ),
            false,
        )
        val outputString = String(out.toByteArray())

        assertThat(outputString).isEqualTo(
            // We don't expect class name, or first line to be colored
            File.separator.color(outputColor) +
                "one-fixed-and-one-not.kt" +
                ":".color(outputColor) +
                "1" +
                ":1:".color(outputColor) +
                " <\"&'> " +
                "(rule-1)".color(outputColor) +
                System.lineSeparator(),
        )
    }

    @Test
    fun testReportGenerationGroupedByFile() {
        val out = ByteArrayOutputStream()
        val reporter = PlainReporter(PrintStream(out, true), groupByFile = true)
        reporter.onLintError(
            "/one-fixed-and-one-not.kt",
            LintError(
                1,
                1,
                "rule-1",
                "<\"&'>",
            ),
            false,
        )
        reporter.onLintError(
            "/one-fixed-and-one-not.kt",
            LintError(
                2,
                1,
                "rule-2",
                "And if you see my friend",
            ),
            true,
        )

        reporter.onLintError(
            "/two-not-fixed.kt",
            LintError(
                1,
                10,
                "rule-1",
                "I thought I would again",
            ),
            false,
        )
        reporter.onLintError(
            "/two-not-fixed.kt",
            LintError(
                2,
                20,
                "rule-2",
                "A single thin straight line",
            ),
            false,
        )

        reporter.onLintError(
            "/all-corrected.kt",
            LintError(
                1,
                1,
                "rule-1",
                "I thought we had more time",
            ),
            true,
        )
        reporter.after("/one-fixed-and-one-not.kt")
        reporter.after("/two-not-fixed.kt")
        reporter.after("/all-corrected.kt")
        assertThat(String(out.toByteArray())).isEqualTo(
            """
            /one-fixed-and-one-not.kt
              1:1 <"&'> (rule-1)
            /two-not-fixed.kt
              1:10 I thought I would again (rule-1)
              2:20 A single thin straight line (rule-2)

            """.trimIndent().replace("\n", System.lineSeparator()),
        )
    }
}
