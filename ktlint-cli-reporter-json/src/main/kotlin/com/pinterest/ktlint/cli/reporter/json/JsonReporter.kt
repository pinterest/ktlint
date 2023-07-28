package com.pinterest.ktlint.cli.reporter.json

import com.pinterest.ktlint.cli.reporter.core.api.KtlintCliError
import com.pinterest.ktlint.cli.reporter.core.api.KtlintCliError.Status.FORMAT_IS_AUTOCORRECTED
import com.pinterest.ktlint.cli.reporter.core.api.ReporterV2
import java.io.PrintStream
import java.util.ArrayList
import java.util.concurrent.ConcurrentHashMap

public class JsonReporter(
    private val out: PrintStream,
) : ReporterV2 {
    private val acc = ConcurrentHashMap<String, MutableList<KtlintCliError>>()

    override fun onLintError(
        file: String,
        ktlintCliError: KtlintCliError,
    ) {
        if (ktlintCliError.status != FORMAT_IS_AUTOCORRECTED) {
            acc.getOrPut(file) { ArrayList() }.add(ktlintCliError)
        }
    }

    override fun afterAll() {
        out.println("[")
        val indexLast = acc.size - 1
        for ((index, entry) in acc.entries.sortedBy { it.key }.withIndex()) {
            val (file, errList) = entry
            out.println("""    {""")
            out.println("""        "file": "${file.escapeJsonValue()}",""")
            out.println("""        "errors": [""")
            val errIndexLast = errList.size - 1
            for ((errIndex, err) in errList.withIndex()) {
                with(err) {
                    out.println("""            {""")
                    out.println("""                "line": $line,""")
                    out.println("""                "column": $col,""")
                    out.println("""                "message": "${detail.escapeJsonValue()}",""")
                    out.println("""                "rule": "$ruleId"""")
                    out.println("""            }${if (errIndex != errIndexLast) "," else ""}""")
                }
            }
            out.println("""        ]""")
            out.println("""    }${if (index != indexLast) "," else ""}""")
        }
        out.println("]")
    }

    private fun String.escapeJsonValue() =
        this
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\b", "\\b")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
}
