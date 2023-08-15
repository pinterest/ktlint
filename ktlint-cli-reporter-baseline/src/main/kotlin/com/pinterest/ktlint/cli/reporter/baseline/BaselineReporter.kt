package com.pinterest.ktlint.cli.reporter.baseline

import com.pinterest.ktlint.cli.reporter.core.api.KtlintCliError
import com.pinterest.ktlint.cli.reporter.core.api.KtlintCliError.Status.FORMAT_IS_AUTOCORRECTED
import com.pinterest.ktlint.cli.reporter.core.api.ReporterV2
import java.io.File
import java.io.PrintStream
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.ConcurrentHashMap
import kotlin.io.path.pathString
import kotlin.io.path.relativeToOrSelf

public class BaselineReporter(
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
        out.println("""<?xml version="1.0" encoding="utf-8"?>""")
        out.println("""<baseline version="1.0">""")
        for ((file, errList) in acc.entries.sortedBy { it.key }) {
            // Store error in baseline always a relative path. This allows a baseline file to be stored inside a repository and after
            // checking out this repository on a different path, the baseline will still be respected.
            val relativeFile = Paths.get(file).relativeLocation()
            out.println("""    <file name="${relativeFile.escapeXMLAttrValue()}">""")
            for (err in errList) {
                with(err) {
                    out.println("""        <error line="$line" column="$col" source="$ruleId" />""")
                }
            }
            out.println("""    </file>""")
        }
        out.println("""</baseline>""")
    }

    private fun String.escapeXMLAttrValue() =
        this
            .replace("&", "&amp;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")

    private fun Path.relativeLocation() =
        relativeToOrSelf(ROOT_DIR_PATH)
            .pathString
            .replace(File.separatorChar, '/')

    private companion object {
        val ROOT_DIR_PATH: Path = Paths.get("").toAbsolutePath()
    }
}
