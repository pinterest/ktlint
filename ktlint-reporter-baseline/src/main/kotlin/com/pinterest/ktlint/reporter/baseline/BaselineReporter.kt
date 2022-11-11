package com.pinterest.ktlint.reporter.baseline

import com.pinterest.ktlint.core.LintError
import com.pinterest.ktlint.core.Reporter
import com.pinterest.ktlint.core.api.relativeRoute
import java.io.PrintStream
import java.nio.file.Paths
import java.util.concurrent.ConcurrentHashMap

public class BaselineReporter(private val out: PrintStream) : Reporter {
    private val acc = ConcurrentHashMap<String, MutableList<LintError>>()

    override fun onLintError(file: String, err: LintError, corrected: Boolean) {
        if (!corrected) {
            acc.getOrPut(file) { ArrayList() }.add(err)
        }
    }

    override fun afterAll() {
        out.println("""<?xml version="1.0" encoding="utf-8"?>""")
        out.println("""<baseline version="1.0">""")
        for ((file, errList) in acc.entries.sortedBy { it.key }) {
            val fileName = Paths.get(file).relativeRoute
            out.println("""    <file name="${fileName.escapeXMLAttrValue()}">""")
            for ((line, col, ruleId, _) in errList) {
                out.println("""        <error line="$line" column="$col" source="$ruleId" />""")
            }
            out.println("""    </file>""")
        }
        out.println("""</baseline>""")
    }

    private fun String.escapeXMLAttrValue() =
        this.replace("&", "&amp;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
}
