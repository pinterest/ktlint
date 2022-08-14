package com.pinterest.ktlint.reporter.baseline

import com.pinterest.ktlint.core.LintError
import com.pinterest.ktlint.core.Reporter
import java.io.File
import java.io.PrintStream
import java.nio.file.Paths
import java.util.ArrayList
import java.util.concurrent.ConcurrentHashMap

public class BaselineReporter(private val out: PrintStream) : Reporter {

    private val acc = ConcurrentHashMap<String, MutableList<LintError>>()

    override fun onLintError(file: String, err: LintError, corrected: Boolean) {
        if (!corrected) {
            acc.getOrPut(file) { ArrayList<LintError>() }.add(err)
        }
    }

    override fun afterAll() {
        out.println("""<?xml version="1.0" encoding="utf-8"?>""")
        out.println("""<baseline version="1.0">""")
        for ((file, errList) in acc.entries.sortedBy { it.key }) {
            val fileName = try {
                val rootPath = Paths.get("").toAbsolutePath()
                val filePath = Paths.get(file)
                rootPath.relativize(filePath).toString().replace(File.separatorChar, '/')
            } catch (e: IllegalArgumentException) {
                file
            }
            out.println("""    <file name="${fileName.escapeXMLAttrValue()}">""")
            for ((line, col, ruleId, _) in errList) {
                out.println(
                    """        <error line="$line" column="$col" source="$ruleId" />"""
                )
            }
            out.println("""    </file>""")
        }
        out.println("""</baseline>""")
    }

    private fun String.escapeXMLAttrValue() =
        this.replace("&", "&amp;").replace("\"", "&quot;").replace("'", "&apos;")
            .replace("<", "&lt;").replace(">", "&gt;")
}
