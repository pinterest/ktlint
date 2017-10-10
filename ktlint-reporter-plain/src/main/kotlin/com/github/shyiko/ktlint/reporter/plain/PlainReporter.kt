package com.github.shyiko.ktlint.reporter.plain

import com.github.shyiko.ktlint.core.LintError
import com.github.shyiko.ktlint.core.Reporter
import java.io.PrintStream
import java.util.ArrayList
import java.util.concurrent.ConcurrentHashMap

class PlainReporter(val out: PrintStream, val verbose: Boolean = false, val groupByFile: Boolean = false) : Reporter {

    private val acc = ConcurrentHashMap<String, MutableList<LintError>>()

    override fun onLintError(file: String, err: LintError, corrected: Boolean) {
        if (!corrected) {
            if (groupByFile) {
                acc.getOrPut(file) { ArrayList<LintError>() }.add(err)
            } else {
                out.println("$file:${err.line}:${err.col}: " +
                    "${err.detail}${if (verbose) " (${err.ruleId})" else ""}")
            }
        }
    }

    override fun after(file: String) {
        if (groupByFile) {
            val errList = acc[file] ?: return
            out.println(file)
            for ((line, col, ruleId, detail) in errList) {
                out.println("  $line:$col " +
                    "$detail${if (verbose) " ($ruleId)" else ""}")
            }
        }
    }
}
