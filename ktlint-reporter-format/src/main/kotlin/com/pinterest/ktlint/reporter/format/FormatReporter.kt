package com.pinterest.ktlint.reporter.format

import com.pinterest.ktlint.core.LintError
import com.pinterest.ktlint.core.Reporter
import com.pinterest.ktlint.reporter.format.internal.Color
import com.pinterest.ktlint.reporter.format.internal.color
import java.io.File
import java.io.PrintStream
import java.util.concurrent.ConcurrentHashMap

public class FormatReporter(
    private val out: PrintStream,
    private val format: Boolean,
    private val shouldColorOutput: Boolean = false,
    private val outputColor: Color = Color.DARK_GRAY
) : Reporter {
    private val countAutoCorrectPossibleOrDone = ConcurrentHashMap<String, Int>()
    private val countCanNotBeAutoCorrected = ConcurrentHashMap<String, Int>()

    override fun onLintError(file: String, err: LintError, corrected: Boolean) {
        // Do not report the individual errors, but count only
        if (err.canBeAutoCorrected || corrected) {
            countAutoCorrectPossibleOrDone.putIfAbsent(file, 0)
            countAutoCorrectPossibleOrDone.replace(file, countAutoCorrectPossibleOrDone.getOrDefault(file, 0) + 1)
        } else {
            countCanNotBeAutoCorrected.putIfAbsent(file, 0)
            countCanNotBeAutoCorrected.replace(file, countCanNotBeAutoCorrected.getOrDefault(file, 0) + 1)
        }
    }

    override fun after(file: String) {
        val canNotBeAutocorrected = countCanNotBeAutoCorrected.getOrDefault(file, 0)
        val result = when {
            canNotBeAutocorrected == 1 ->
                if (format) {
                    "Format not completed (1 violation needs manual fixing)"
                } else {
                    "Format required (1 violation needs manual fixing)"
                }
            canNotBeAutocorrected > 1 ->
                if (format) {
                    "Format not completed ($canNotBeAutocorrected violations need manual fixing)"
                } else {
                    "Format required ($canNotBeAutocorrected violations need manual fixing)"
                }
            countAutoCorrectPossibleOrDone.getOrDefault(file, 0) > 0 ->
                if (format) {
                    "Format completed (all violations have been fixed)"
                } else {
                    "Format required (all violations can be autocorrected)"
                }
            else ->
                "Format not needed (no violations found)"
        }
        out.println(
            "${colorFileName(file)}${":".colored()} $result"
        )
    }

    private fun colorFileName(fileName: String): String {
        val name = fileName.substringAfterLast(File.separator)
        return fileName.substring(0, fileName.length - name.length).colored() + name
    }

    private fun String.colored() =
        if (shouldColorOutput) this.color(outputColor) else this
}
