package com.pinterest.ktlint.cli.reporter.sarif

import com.pinterest.ktlint.cli.reporter.core.api.KtlintCliError
import com.pinterest.ktlint.cli.reporter.core.api.ReporterV2
import com.pinterest.ktlint.cli.reporter.core.api.ktlintVersion
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
import kotlin.io.path.Path
import kotlin.io.path.pathString
import kotlin.io.path.relativeToOrSelf

private const val SRCROOT = "%SRCROOT%"

internal fun String.sanitize(): String =
    this
        .replace(File.separatorChar, '/')
        .let {
            if (it.endsWith('/')) {
                it
            } else {
                "$it/"
            }
        }

public class SarifReporter(
    private val out: PrintStream,
) : ReporterV2 {
    private val results: MutableList<Result> = mutableListOf()
    private var workingDirectory: File? = null

    override fun beforeAll() {
        workingDirectory = System.getProperty("user.home")?.let(::File)
    }

    override fun onLintError(
        file: String,
        ktlintCliError: KtlintCliError,
    ) {
        results.add(
            Result(
                ruleID = ktlintCliError.ruleId,
                level = Level.Error,
                locations =
                    listOf(
                        Location(
                            physicalLocation =
                                PhysicalLocation(
                                    region =
                                        Region(
                                            startLine = ktlintCliError.line.toLong(),
                                            startColumn = ktlintCliError.col.toLong(),
                                        ),
                                    artifactLocation =
                                        workingDirectory?.let { workingDirectory ->
                                            ArtifactLocation(
                                                uri = Path(file).relativeToOrSelf(workingDirectory.toPath()).pathString,
                                                uriBaseID = SRCROOT,
                                            )
                                        } ?: ArtifactLocation(
                                            uri = file,
                                        ),
                                ),
                        ),
                    ),
                message = Message(text = ktlintCliError.detail),
            ),
        )
    }

    override fun afterAll() {
        val version = ktlintVersion(SarifReporter::class.java)
        val sarifSchema210 =
            SarifSchema210(
                schema = "https://raw.githubusercontent.com/oasis-tcs/sarif-spec/master/Schemata/sarif-schema-2.1.0.json",
                version = Version.The210,
                runs =
                    listOf(
                        Run(
                            tool =
                                Tool(
                                    driver =
                                        ToolComponent(
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
                            originalURIBaseIDS =
                                workingDirectory?.let {
                                    mapOf(SRCROOT to ArtifactLocation(uri = "file://${it.path.sanitize()}"))
                                },
                            results = results,
                        ),
                    ),
            )
        out.println(SarifSerializer.toJson(sarifSchema210))
    }
}
