package com.pinterest.ktlint.reporter.sarif

import com.pinterest.ktlint.core.LintError
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class SarifReporterTest {
    @Disabled("https://github.com/pinterest/ktlint/issues/1191")
    @Test
    fun testReportGeneration() {
        val workingDirectory = System.getProperty("user.home").sanitize()
        val out = ByteArrayOutputStream()
        val reporter = SarifReporter(PrintStream(out))
        reporter.beforeAll()
        reporter.onLintError(
            "$workingDirectory/one-fixed-and-one-not.kt",
            LintError(1, 1, "rule-1", "<\"&'>"),
            false,
        )
        reporter.onLintError(
            "$workingDirectory/one-fixed-and-one-not.kt",
            LintError(2, 1, "rule-2", "And if you see my friend"),
            true,
        )
        reporter.onLintError(
            "$workingDirectory/two-not-fixed.kt",
            LintError(1, 10, "rule-1", "I thought I would again"),
            false,
        )
        reporter.onLintError(
            "$workingDirectory/two-not-fixed.kt",
            LintError(2, 20, "rule-2", "A single thin straight line"),
            false,
        )
        reporter.onLintError(
            "$workingDirectory/all-corrected.kt",
            LintError(1, 1, "rule-1", "I thought we had more time"),
            true,
        )
        reporter.afterAll()

        val actual = String(out.toByteArray()).replace("\\s".toRegex(), "")
        val expected = File(javaClass.getResource("/relative-path.sarif").path)
            .readText()
            .replace("{WORKINDG_DIR}", workingDirectory)
            .replace("\\s".toRegex(), "")
        assertThat(actual).isEqualTo(expected)
    }
}
