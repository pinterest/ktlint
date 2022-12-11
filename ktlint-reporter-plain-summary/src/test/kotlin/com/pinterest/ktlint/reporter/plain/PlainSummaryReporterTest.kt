package com.pinterest.ktlint.reporter.plain

import com.pinterest.ktlint.core.LintError
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class PlainSummaryReporterTest {
    @Test
    fun `Run report and print errors which are autocorrect and those that can not be autocorrected and print summaries`() {
        val out = ByteArrayOutputStream()
        PlainSummaryReporter(
            PrintStream(out, true),
        ).apply {
            onLintError(
                "file-1.kt",
                LintError(
                    1,
                    1,
                    "rule-1",
                    "description-error-at-position-1:1 (cannot be auto-corrected)",
                ),
                false,
            )
            onLintError(
                "file-1.kt",
                LintError(
                    2,
                    1,
                    "rule-2",
                    "description-error-at-position-2:1",
                ),
                true,
            )

            onLintError(
                "file-2.kt",
                LintError(
                    1,
                    10,
                    "rule-1",
                    "description-error-at-position-1:10 (cannot be auto-corrected)",
                ),
                false,
            )
            onLintError(
                "file-2.kt",
                LintError(
                    2,
                    20,
                    "rule-2",
                    "description-error-at-position-2:20 (cannot be auto-corrected)",
                ),
                false,
            )

            onLintError(
                "file-3.kt",
                LintError(
                    1,
                    1,
                    "rule-1",
                    "description-error-at-position-1:1",
                ),
                true,
            )

            after("file-1.kt")
            after("file-2.kt")
            after("file-3.kt")

            afterAll()
        }

        assertThat(String(out.toByteArray())).isEqualTo(
            """
            Count (descending) of autocorrected errors by rule:
              rule-1: 1
              rule-2: 1

            Count (descending) of errors not autocorrected by rule:
              rule-1: 2
              rule-2: 1

            """.trimIndent().replace("\n", System.lineSeparator()),
        )
    }

    @Test
    fun `Report other violations`() {
        val out = ByteArrayOutputStream()
        val reporter = PlainSummaryReporter(PrintStream(out, true))
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
            |Count (descending) of errors not autocorrected by rule:
            |  Not a valid Kotlin file: 2
            |  Unknown: 1
            |
            """.trimMargin().replace("\n", System.lineSeparator()),
        )
    }
}
