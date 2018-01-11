package com.github.shyiko.ktlint.reporter.plain

import com.andreapivetta.kolor.Color
import com.andreapivetta.kolor.Kolor
import com.github.shyiko.ktlint.core.LintError
import com.github.shyiko.ktlint.core.Reporter
import java.io.File
import java.io.PrintStream
import java.util.ArrayList
import java.util.concurrent.ConcurrentHashMap

class PlainReporter(
    val out: PrintStream,
    val verbose: Boolean = false,
    val groupByFile: Boolean = false,
    val color: Boolean = false,
    val pad: Boolean = false
) : Reporter {

    private val acc = ConcurrentHashMap<String, MutableList<LintError>>()

    override fun onLintError(file: String, err: LintError, corrected: Boolean) {
        if (!corrected) {
            if (groupByFile) {
                acc.getOrPut(file) { ArrayList<LintError>() }.add(err)
            } else {
                out.println("${colorFileName(file)}${":".gray()}${err.line}${
                    ":${"${err.col}:".let { if (pad) String.format("%-4s", it) else it}}".gray()
                } ${err.detail}${if (verbose) " (${err.ruleId})".gray() else ""}")
            }
        }
    }

    override fun after(file: String) {
        if (groupByFile) {
            val errList = acc[file] ?: return
            out.println(colorFileName(file))
            for ((line, col, ruleId, detail) in errList) {
                out.println("  $line${
                    ":${if (pad) String.format("%-3s", col) else "$col"}".gray()
                } $detail${if (verbose) " ($ruleId)".gray() else ""}")
            }
        }
    }

    private fun colorFileName(fileName: String): String {
        val name = fileName.substringAfterLast(File.separator)
        return fileName.substring(0, fileName.length - name.length).gray() + name
    }

    private fun String.gray() =
        if (color) Kolor.foreground(this, Color.DARK_GRAY) else this
}
