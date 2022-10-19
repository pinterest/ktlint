package com.pinterest.ktlint.reporter.plain

import com.pinterest.ktlint.core.LintError
import com.pinterest.ktlint.core.Reporter
import com.pinterest.ktlint.reporter.plain.internal.Color
import com.pinterest.ktlint.reporter.plain.internal.color
import java.io.File
import java.io.PrintStream
import java.util.ArrayList
import java.util.concurrent.ConcurrentHashMap

public class PlainReporter(
    private val out: PrintStream,
    private val verbose: Boolean = false,
    private val groupByFile: Boolean = false,
    private val shouldColorOutput: Boolean = false,
    private val outputColor: Color = Color.DARK_GRAY,
    private val pad: Boolean = false,
) : Reporter {

    private val acc = ConcurrentHashMap<String, MutableList<LintError>>()

    override fun onLintError(file: String, err: LintError, corrected: Boolean) {
        if (!corrected) {
            if (groupByFile) {
                acc.getOrPut(file) { ArrayList<LintError>() }.add(err)
            } else {
                val column =
                    if (pad) {
                        String.format("%-4s", err.col)
                    } else {
                        err.col
                    }
                out.println(
                    "${colorFileName(file)}${":".colored()}${err.line}${":$column:".colored()} ${err.detail} ${"(${err.ruleId})".colored()}",
                )
            }
        }
    }

    override fun after(file: String) {
        if (groupByFile) {
            val errList = acc[file] ?: return
            out.println(colorFileName(file))
            for ((line, col, ruleId, detail) in errList) {
                val column = if (pad) {
                    String.format("%-3s", col)
                } else {
                    col
                }
                val ruleName = if (verbose) {
                    " ($ruleId)".colored()
                } else {
                    ""
                }
                out.println(
                    "  $line${":$column".colored()} $detail$ruleName",
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
