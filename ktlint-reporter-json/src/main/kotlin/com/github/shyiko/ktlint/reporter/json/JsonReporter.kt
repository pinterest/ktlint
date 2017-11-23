package com.github.shyiko.ktlint.reporter.json

import com.github.shyiko.ktlint.core.LintError
import com.github.shyiko.ktlint.core.Reporter
import java.io.PrintStream
import java.util.ArrayList
import java.util.concurrent.ConcurrentHashMap

class JsonReporter(val out: PrintStream) : Reporter {

    private val acc = ConcurrentHashMap<String, MutableList<LintError>>()

    override fun onLintError(file: String, err: LintError, corrected: Boolean) {
        if (!corrected) {
            acc.getOrPut(file) { ArrayList<LintError>() }.add(err)
        }
    }

    override fun afterAll() {
        out.println("[")
        val indexLast = acc.size - 1
        for ((index, entry) in acc.entries.sortedBy { it.key }.withIndex()) {
            val (file, errList) = entry
            out.println("""	{""")
            out.println("""		"file": "${file.escapeJsonValue()}",""")
            out.println("""		"errors": [""")
            val errIndexLast = errList.size - 1
            for ((errIndex, err) in errList.withIndex()) {
                val (line, col, ruleId, detail) = err
                out.println("""			{""")
                out.println("""				"line": $line,""")
                out.println("""				"column": $col,""")
                out.println("""				"message": "${detail.escapeJsonValue()}",""")
                out.println("""				"rule": "$ruleId"""")
                out.println("""			}${if (errIndex != errIndexLast) "," else ""}""")
            }
            out.println("""		]""")
            out.println("""	}${if (index != indexLast) "," else ""}""")
        }
        out.println("]")
    }

    private fun String.escapeJsonValue() = this.replace("\"", "\\\"")
}
