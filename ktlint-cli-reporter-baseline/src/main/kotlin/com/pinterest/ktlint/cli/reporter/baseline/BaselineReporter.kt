package com.pinterest.ktlint.cli.reporter.baseline

import com.pinterest.ktlint.cli.reporter.core.api.KtlintCliError
import com.pinterest.ktlint.cli.reporter.core.api.KtlintCliError.Status.FORMAT_IS_AUTOCORRECTED
import com.pinterest.ktlint.cli.reporter.core.api.ReporterV2
import java.io.PrintStream
import java.util.concurrent.ConcurrentHashMap

public class BaselineReporter(private val out: PrintStream) : ReporterV2 {
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
        out.println("""<?xml version="1.0" encoding="utf-8"?>""")
        out.println("""<baseline version="1.0">""")
        for ((file, errList) in acc.entries.sortedBy { it.key }) {
            out.println("""    <file name="${file.escapeXMLAttrValue()}">""")
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
