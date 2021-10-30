package com.pinterest.ktlint.reporter.json

import com.pinterest.ktlint.core.LintError
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class JsonReporterTest {

    @Test
    fun testReportGeneration() {
        val out = ByteArrayOutputStream()
        val reporter = JsonReporter(PrintStream(out, true))
        reporter.onLintError(
            "/one-fixed-and-one-not.kt",
            LintError(
                1,
                1,
                "rule-1",
                "<\"&'>"
            ),
            false
        )
        reporter.onLintError(
            "/one-fixed-and-one-not.kt",
            LintError(
                2,
                1,
                "rule-2",
                "And if you see my friend"
            ),
            true
        )

        reporter.onLintError(
            "/two-not-fixed.kt",
            LintError(
                1,
                10,
                "rule-1",
                "I thought I would again"
            ),
            false
        )
        reporter.onLintError(
            "/two-not-fixed.kt",
            LintError(
                2,
                20,
                "rule-2",
                "A single thin straight line"
            ),
            false
        )

        reporter.onLintError(
            "/all-corrected.kt",
            LintError(
                1,
                1,
                "rule-1",
                "I thought we had more time"
            ),
            true
        )
        reporter.afterAll()
        assertThat(String(out.toByteArray())).isEqualTo(
            """
            [
            {TAB}{
            {TAB}{TAB}"file": "/one-fixed-and-one-not.kt",
            {TAB}{TAB}"errors": [
            {TAB}{TAB}{TAB}{
            {TAB}{TAB}{TAB}{TAB}"line": 1,
            {TAB}{TAB}{TAB}{TAB}"column": 1,
            {TAB}{TAB}{TAB}{TAB}"message": "<\"&'>",
            {TAB}{TAB}{TAB}{TAB}"rule": "rule-1"
            {TAB}{TAB}{TAB}}
            {TAB}{TAB}]
            {TAB}},
            {TAB}{
            {TAB}{TAB}"file": "/two-not-fixed.kt",
            {TAB}{TAB}"errors": [
            {TAB}{TAB}{TAB}{
            {TAB}{TAB}{TAB}{TAB}"line": 1,
            {TAB}{TAB}{TAB}{TAB}"column": 10,
            {TAB}{TAB}{TAB}{TAB}"message": "I thought I would again",
            {TAB}{TAB}{TAB}{TAB}"rule": "rule-1"
            {TAB}{TAB}{TAB}},
            {TAB}{TAB}{TAB}{
            {TAB}{TAB}{TAB}{TAB}"line": 2,
            {TAB}{TAB}{TAB}{TAB}"column": 20,
            {TAB}{TAB}{TAB}{TAB}"message": "A single thin straight line",
            {TAB}{TAB}{TAB}{TAB}"rule": "rule-2"
            {TAB}{TAB}{TAB}}
            {TAB}{TAB}]
            {TAB}}
            ]

            """.trimIndent()
                .replace("{TAB}", "\t")
                .replace("\n", System.lineSeparator())
        )
    }

    @Test
    fun testProperEscaping() {
        val out = ByteArrayOutputStream()
        val reporter = JsonReporter(PrintStream(out, true))
        reporter.onLintError("src\\main\\all\\corrected.kt", LintError(4, 7, "rule-7", "\\n\n\r\t\""), false)
        reporter.afterAll()
        assertThat(String(out.toByteArray())).isEqualTo(
            """
            [
            {TAB}{
            {TAB}{TAB}"file": "src\\main\\all\\corrected.kt",
            {TAB}{TAB}"errors": [
            {TAB}{TAB}{TAB}{
            {TAB}{TAB}{TAB}{TAB}"line": 4,
            {TAB}{TAB}{TAB}{TAB}"column": 7,
            {TAB}{TAB}{TAB}{TAB}"message": "\\n\n\r\t\"",
            {TAB}{TAB}{TAB}{TAB}"rule": "rule-7"
            {TAB}{TAB}{TAB}}
            {TAB}{TAB}]
            {TAB}}
            ]

            """.trimIndent()
                .replace("{TAB}", "\t")
                .replace("\n", System.lineSeparator())
        )
    }
}
