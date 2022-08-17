package com.pinterest.ktlint.reporter.sarif

import com.pinterest.ktlint.core.LintError
import com.pinterest.ktlint.core.Reporter
import com.pinterest.ktlint.core.ktlintVersion
import io.github.detekt.sarif4k.ArtifactLocation
import io.github.detekt.sarif4k.Level
import io.github.detekt.sarif4k.Location
import io.github.detekt.sarif4k.Message
import io.github.detekt.sarif4k.PhysicalLocation
import io.github.detekt.sarif4k.Region
import io.github.detekt.sarif4k.Result
import io.github.detekt.sarif4k.Run
import io.github.detekt.sarif4k.SarifSchema210
import io.github.detekt.sarif4k.SarifSerializer
import io.github.detekt.sarif4k.Tool
import io.github.detekt.sarif4k.ToolComponent
import io.github.detekt.sarif4k.Version
import java.io.File
import java.io.PrintStream

private const val SRCROOT = "%SRCROOT%"
internal fun String.sanitize(): String =
    this.replace(File.separatorChar, '/')
        .let {
            if (it.endsWith('/')) it else "$it/"
        }

public class SarifReporter(private val out: PrintStream) : Reporter {

    private val results: MutableList<Result> = mutableListOf()
    private var workingDirectory: File? = null

    override fun beforeAll() {
        workingDirectory = System.getProperty("user.home")?.let(::File)
    }

    override fun onLintError(file: String, err: LintError, corrected: Boolean) {
        results.add(
            Result(
                ruleID = err.ruleId,
                level = Level.Error,
                locations = listOf(
                    Location(
                        physicalLocation = PhysicalLocation(
                            region = Region(
                                startLine = err.line.toLong(),
                                startColumn = err.col.toLong(),
                            ),
                            artifactLocation = workingDirectory?.let { workingDirectory ->
                                ArtifactLocation(
                                    uri = File(file).relativeTo(workingDirectory).path,
                                    uriBaseID = SRCROOT,
                                )
                            } ?: ArtifactLocation(
                                uri = file,
                            ),
                        ),
                    ),
                ),
                message = Message(text = err.detail),
            ),
        )
    }

    override fun afterAll() {
        val version = ktlintVersion(SarifReporter::class.java)
        val sarifSchema210 = SarifSchema210(
            schema = "https://raw.githubusercontent.com/oasis-tcs/sarif-spec/master/Schemata/sarif-schema-2.1.0.json",
            version = Version.The210,
            runs = listOf(
                Run(
                    tool = Tool(
                        driver = ToolComponent(
                            downloadURI = "https://github.com/pinterest/ktlint/releases/tag/$version",
                            fullName = "ktlint",
                            informationURI = "https://github.com/pinterest/ktlint/",
                            language = "en",
                            name = "ktlint",
                            rules = listOf(),
                            organization = "pinterest",
                            semanticVersion = version,
                            version = version,
                        ),
                    ),
                    originalURIBaseIDS = workingDirectory?.let {
                        mapOf(SRCROOT to ArtifactLocation(uri = "file://${it.path.sanitize()}"))
                    },
                    results = results,
                ),
            ),
        )
        out.println(SarifSerializer.toJson(sarifSchema210))
    }
}
