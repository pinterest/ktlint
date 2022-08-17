package com.pinterest.ktlint.reporter.checkstyle

import com.pinterest.ktlint.core.LintError
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class CheckStyleReporterTest {

    @Test
    fun testReportGeneration() {
        val out = ByteArrayOutputStream()
        val reporter = CheckStyleReporter(PrintStream(out, true))
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
        reporter.afterAll()
        assertThat(String(out.toByteArray())).isEqualTo(
            """
            <?xml version="1.0" encoding="utf-8"?>
            <checkstyle version="8.0">
                <file name="/one-fixed-and-one-not.kt">
                    <error line="1" column="1" severity="error" message="&lt;&quot;&amp;&apos;&gt;" source="rule-1" />
                </file>
                <file name="/two-not-fixed.kt">
                    <error line="1" column="10" severity="error" message="I thought I would again" source="rule-1" />
                    <error line="2" column="20" severity="error" message="A single thin straight line" source="rule-2" />
                </file>
            </checkstyle>

            """.trimIndent()
                .replace("\n", System.lineSeparator()),
        )
    }
}
