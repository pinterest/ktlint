package com.github.shyiko.ktlint.reporter.json

import com.github.shyiko.ktlint.core.LintError
import org.assertj.core.api.Assertions
import org.testng.annotations.Test
import java.io.ByteArrayOutputStream
import java.io.PrintStream

class JsonReporterTest {

    @Test
    fun testReportGeneration() {
        val out = ByteArrayOutputStream()
        val reporter = JsonReporter(PrintStream(out, true))
        reporter.onLintError("/one-fixed-and-one-not.kt", LintError(1, 1, "rule-1",
            "<\"&'>"), false)
        reporter.onLintError("/one-fixed-and-one-not.kt", LintError(2, 1, "rule-2",
            "And if you see my friend"), true)

        reporter.onLintError("/two-not-fixed.kt", LintError(1, 10, "rule-1",
            "I thought I would again"), false)
        reporter.onLintError("/two-not-fixed.kt", LintError(2, 20, "rule-2",
            "A single thin straight line"), false)

        reporter.onLintError("/all-corrected.kt", LintError(1, 1, "rule-1",
            "I thought we had more time"), true)
        reporter.afterAll()
        Assertions.assertThat(String(out.toByteArray())).isEqualTo(
            """
[
	{
		"file": "/one-fixed-and-one-not.kt",
		"errors": [
			{
				"line": 1,
				"column": 1,
				"message": "<\"&'>",
				"rule": "rule-1"
			}
		]
	},
	{
		"file": "/two-not-fixed.kt",
		"errors": [
			{
				"line": 1,
				"column": 10,
				"message": "I thought I would again",
				"rule": "rule-1"
			},
			{
				"line": 2,
				"column": 20,
				"message": "A single thin straight line",
				"rule": "rule-2"
			}
		]
	}
]
""".trimStart().replace("\n", System.lineSeparator())
        )
    }
}
