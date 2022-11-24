package com.pinterest.ktlint.internal

import com.pinterest.ktlint.core.KtLintRuleEngine
import com.pinterest.ktlint.core.RuleProvider
import com.pinterest.ktlint.core.api.KtLintParseException
import com.pinterest.ktlint.core.initKtLintKLogger
import com.pinterest.ruleset.test.DumpASTRule
import java.io.File
import java.nio.file.FileSystems
import java.nio.file.Paths
import java.util.Locale
import mu.KotlinLogging
import picocli.CommandLine

private val LOGGER = KotlinLogging.logger {}.initKtLintKLogger()

@CommandLine.Command(
    description = [
        "Print AST (useful when writing/debugging rules)",
        "Usage of \"--print-ast\" command line option is deprecated!",
    ],
    aliases = ["--print-ast"],
    mixinStandardHelpOptions = true,
    versionProvider = KtlintVersionProvider::class,
)
internal class PrintASTSubCommand : Runnable {
    @CommandLine.ParentCommand
    private lateinit var ktlintCommand: KtlintCommandLine

    @CommandLine.Spec
    private lateinit var commandSpec: CommandLine.Model.CommandSpec

    @CommandLine.Parameters(
        description = ["include all files under this .gitignore-like patterns"],
    )
    private var patterns = ArrayList<String>()

    @CommandLine.Option(
        names = ["--stdin"],
        description = ["Read file content from stdin"],
    )
    private var stdin: Boolean = false

    override fun run() {
        commandSpec.commandLine().printCommandLineHelpOrVersionUsage()

        if (stdin) {
            printAST(fileContent = String(System.`in`.readBytes()))
        } else {
            FileSystems.getDefault()
                .fileSequence(patterns)
                .map { it.toFile() }
                .forEach {
                    printAST(
                        fileName = it.path,
                        fileContent = it.readText(),
                    )
                }
        }
    }

    private fun printAST(
        fileContent: String,
        fileName: String? = null,
    ) {
        if (fileName != null) {
            LOGGER.debug { "Analyzing ${File(fileName).location(ktlintCommand.relative)}" }
        }

        try {
            KtLintRuleEngine(
                ruleProviders = setOf(
                    RuleProvider { DumpASTRule(System.out, ktlintCommand.color) },
                ),
            ).lint(
                code = fileContent,
                filePath = fileName?.let { Paths.get(it) },
            )
        } catch (e: Exception) {
            if (e is KtLintParseException) {
                throw KtLintParseException(
                    e.line,
                    e.col,
                    "Not a valid Kotlin file (${e.message?.lowercase(Locale.getDefault())})",
                )
            }
            throw e
        }
    }

    companion object {
        internal const val COMMAND_NAME = "printAST"
    }
}
