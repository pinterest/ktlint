package com.pinterest.ktlint.reporter.format

import com.pinterest.ktlint.core.LintError
import com.pinterest.ktlint.reporter.format.internal.Color
import com.pinterest.ktlint.reporter.format.internal.color
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class FormatReporterTest {
    @Test
    fun `Given some lint violations from which at least one is autocorrected and format is running`() {
        val out = ByteArrayOutputStream()
        val reporter = FormatReporter(
            out = PrintStream(out, true),
            format = true
        )

        // File 1: 1 violation not autocorrected
        reporter.onLintError(
            SOME_FILE_PATH_1,
            SOME_LINT_ERROR_UNCORRECTABLE,
            false
        )

        // File 2: 2 violations not autocorrected
        reporter.onLintError(
            SOME_FILE_PATH_2,
            SOME_LINT_ERROR_UNCORRECTABLE,
            false
        )
        reporter.onLintError(
            SOME_FILE_PATH_2,
            SOME_LINT_ERROR_UNCORRECTABLE,
            false
        )

        // File 3: At least 1 violation autocorrected
        reporter.onLintError(
            SOME_FILE_PATH_3,
            SOME_LINT_ERROR_CORRECTABLE,
            true
        )

        reporter.after(SOME_FILE_PATH_1)
        reporter.after(SOME_FILE_PATH_2)
        reporter.after(SOME_FILE_PATH_3)
        reporter.after(SOME_FILE_PATH_4)

        val outputString = String(out.toByteArray())

        assertThat(outputString).isEqualTo(
            """
            $SOME_FILE_PATH_1: Format not completed (1 violation needs manual fixing)
            $SOME_FILE_PATH_2: Format not completed (2 violations need manual fixing)
            $SOME_FILE_PATH_3: Format completed (all violations have been fixed)
            $SOME_FILE_PATH_4: Format not needed (no violations found)

            """.trimIndent().replace("\n", System.lineSeparator())
        )
    }

    @Test
    fun `Given some lint violations from which none is autocorrected and lint is running`() {
        val out = ByteArrayOutputStream()
        val reporter = FormatReporter(
            out = PrintStream(out, true),
            format = false
        )

        // File 1: At least 1 violation can be autocorrected but is not (e.g. lint is running)
        reporter.onLintError(
            SOME_FILE_PATH_1,
            SOME_LINT_ERROR_CORRECTABLE,
            false
        )

        reporter.after(SOME_FILE_PATH_1)
        reporter.after(SOME_FILE_PATH_2)

        val outputString = String(out.toByteArray())

        assertThat(outputString).isEqualTo(
            """
            $SOME_FILE_PATH_1: Format required (all violations can be autocorrected)
            $SOME_FILE_PATH_2: Format not needed (no violations found)

            """.trimIndent().replace("\n", System.lineSeparator())
        )
    }

    @Test
    fun `Given that the output has to be colored`() {
        val out = ByteArrayOutputStream()
        val outputColor = Color.DARK_GRAY
        val reporter = FormatReporter(
            PrintStream(out, true),
            format = true,
            shouldColorOutput = true,
            outputColor = outputColor
        )

        reporter.onLintError(
            File.separator.plus(SOME_FILE_NAME),
            SOME_LINT_ERROR_UNCORRECTABLE,
            false
        )
        reporter.after(
            File.separator.plus(SOME_FILE_NAME)
        )

        val outputString = String(out.toByteArray())

        assertThat(outputString).isEqualTo(
            // Filename should not be colored
            File.separator.color(outputColor) +
                SOME_FILE_NAME +
                ":".color(outputColor) +
                " " +
                "Format not completed (1 violation needs manual fixing)" +
                System.lineSeparator()
        )
    }

    companion object {
        val SOME_LINT_ERROR_CORRECTABLE = LintError(
            1,
            1,
            "some-rule",
            "This error can be autocorrected",
            true
        )
        val SOME_LINT_ERROR_UNCORRECTABLE = LintError(
            1,
            1,
            "rule-1",
            "This error can *not* be autocorrected",
            false
        )
        const val SOME_FILE_PATH_1 = "/path/to/some-file-1.kt"
        const val SOME_FILE_PATH_2 = "/path/to/some-file-2.kt"
        const val SOME_FILE_PATH_3 = "/path/to/some-file-3.kt"
        const val SOME_FILE_PATH_4 = "/path/to/some-file-4.kt"
        const val SOME_FILE_NAME = "some-file-name.kt"
    }
}
