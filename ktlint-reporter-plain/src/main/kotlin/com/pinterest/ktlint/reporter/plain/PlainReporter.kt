package com.pinterest.ktlint.reporter.plain

import com.pinterest.ktlint.core.LintError
import com.pinterest.ktlint.core.Reporter
import com.pinterest.ktlint.reporter.plain.internal.Color
import com.pinterest.ktlint.reporter.plain.internal.color
import java.io.File
import java.io.PrintStream
import java.util.ArrayList
import java.util.concurrent.ConcurrentHashMap

class PlainReporter(
    val out: PrintStream,
    val verbose: Boolean = false,
    val groupByFile: Boolean = false,
    val shouldColorOutput: Boolean = false,
    val outputColor: Color = Color.DARK_GRAY,
    val pad: Boolean = false
) : Reporter {

    private val acc = ConcurrentHashMap<String, MutableList<LintError>>()

    override fun onLintError(file: String, err: LintError, corrected: Boolean) {
        if (!corrected) {
            if (groupByFile) {
                acc.getOrPut(file) { ArrayList<LintError>() }.add(err)
            } else {
                out.println(
                    "${colorFileName(file)}${":".colored()}${err.line}${
                    ":${"${err.col}:".let { if (pad) String.format("%-4s", it) else it}}".colored()
                    } ${err.detail}${if (verbose) " (${err.ruleId})".colored() else ""}"
                )
            }
        }
    }

    override fun after(file: String) {
        if (groupByFile) {
            val errList = acc[file] ?: return
            out.println(colorFileName(file))
            for ((line, col, ruleId, detail) in errList) {
                out.println(
                    "  $line${
                    ":${if (pad) String.format("%-3s", col) else "$col"}".colored()
                    } $detail${if (verbose) " ($ruleId)".colored() else ""}"
                )
            }
        }
    }

    private fun colorFileName(fileName: String): String {
        val name = fileName.substringAfterLast(File.separator)
        return fileName.substring(0, fileName.length - name.length).colored() + name
    }

    private fun String.colored() =
        if (shouldColorOutput) this.color(outputColor) else this
}
