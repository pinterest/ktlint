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
            acc.getOrPut(file, { ArrayList<LintError>() }).add(err)
        }
    }

    override fun afterAll() {
        out.println("[")
        for ((i, entry) in acc.entries.sortedBy { it.key }.withIndex()) {
            val (file, errList) = entry
            out.println(
                """
                |	{
                |		"file": "${file.escapeJsonValue()}",
                |		"errors": [
                """.trimMargin()
            )
            out.println(
                errList.map { (line, col, ruleId, detail) ->
                    """
                    |			{
                    |				"line": $line,
                    |				"column": $col,
                    |				"message": "${detail.escapeJsonValue()}",
                    |				"rule": "$ruleId"
                    |			}
                    """.trimMargin()
                }.joinToString(",\n")
            )
            out.println(
                """
                |		]
                |	}${if (i < acc.size - 1) "," else ""}
                """.trimMargin())
        }
        out.println("]")
    }

    private fun String.escapeJsonValue() = this.replace("\"", "\\\"")

}
