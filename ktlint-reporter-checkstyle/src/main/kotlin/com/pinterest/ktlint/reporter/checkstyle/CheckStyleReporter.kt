package com.pinterest.ktlint.reporter.checkstyle

import com.pinterest.ktlint.core.LintError
import com.pinterest.ktlint.core.Reporter
import java.io.PrintStream
import java.util.ArrayList
import java.util.concurrent.ConcurrentHashMap

public class CheckStyleReporter(private val out: PrintStream) : Reporter {

    private val acc = ConcurrentHashMap<String, MutableList<LintError>>()

    override fun onLintError(file: String, err: LintError, corrected: Boolean) {
        if (!corrected) {
            acc.getOrPut(file) { ArrayList<LintError>() }.add(err)
        }
    }

    override fun afterAll() {
        out.println("""<?xml version="1.0" encoding="utf-8"?>""")
        out.println("""<checkstyle version="8.0">""")
        for ((file, errList) in acc.entries.sortedBy { it.key }) {
            out.println("""    <file name="${file.escapeXMLAttrValue()}">""")
            for ((line, col, ruleId, detail) in errList) {
                out.println(
                    """        <error line="$line" column="$col" severity="error" message="${
                    detail.escapeXMLAttrValue()
                    }" source="$ruleId" />""",
                )
            }
            out.println("""    </file>""")
        }
        out.println("""</checkstyle>""")
    }

    private fun String.escapeXMLAttrValue() =
        this.replace("&", "&amp;").replace("\"", "&quot;").replace("'", "&apos;")
            .replace("<", "&lt;").replace(">", "&gt;")
}
