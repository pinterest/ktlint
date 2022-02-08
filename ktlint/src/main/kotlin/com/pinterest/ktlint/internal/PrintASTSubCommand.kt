package com.pinterest.ktlint.internal

import com.pinterest.ktlint.KtlintCommandLine
import com.pinterest.ktlint.core.KtLint
import com.pinterest.ktlint.core.ParseException
import com.pinterest.ktlint.core.RuleSet
import com.pinterest.ktlint.core.VisitorProvider
import com.pinterest.ktlint.core.initKtLintKLogger
import com.pinterest.ruleset.test.DumpASTRule
import java.io.File
import java.nio.file.FileSystems
import mu.KotlinLogging
import picocli.CommandLine

private val logger = KotlinLogging.logger {}.initKtLintKLogger()

@CommandLine.Command(
    description = [
        "Print AST (useful when writing/debugging rules)",
        "Usage of \"--print-ast\" command line option is deprecated!"
    ],
    aliases = ["--print-ast"],
    mixinStandardHelpOptions = true,
    versionProvider = KtlintVersionProvider::class
)
internal class PrintASTSubCommand : Runnable {
    @CommandLine.ParentCommand
    private lateinit var ktlintCommand: KtlintCommandLine

    @CommandLine.Spec
    private lateinit var commandSpec: CommandLine.Model.CommandSpec

    @CommandLine.Parameters(
        description = ["include all files under this .gitignore-like patterns"]
    )
    private var patterns = ArrayList<String>()

    @CommandLine.Option(
        names = ["--stdin"],
        description = ["Read file content from stdin"]
    )
    private var stdin: Boolean = false

    private val astRuleSet by lazy(LazyThreadSafetyMode.NONE) {
        listOf(
            RuleSet("debug", DumpASTRule(System.out, ktlintCommand.color))
        )
    }

    override fun run() {
        commandSpec.commandLine().printHelpOrVersionUsage()

        val visitorProvider = VisitorProvider(
            ruleSets = astRuleSet,
            debug = ktlintCommand.debug
        )
        if (stdin) {
            printAST(visitorProvider, KtLint.STDIN_FILE, String(System.`in`.readBytes()))
        } else {
            FileSystems.getDefault()
                .fileSequence(patterns)
                .map { it.toFile() }
                .forEach {
                    printAST(visitorProvider, it.path, it.readText())
                }
        }
    }

    private fun printAST(
        visitorProvider: VisitorProvider,
        fileName: String,
        fileContent: String
    ) {
        logger.debug {
            "Analyzing " + if (fileName != KtLint.STDIN_FILE) {
                File(fileName).location(ktlintCommand.relative)
            } else {
                "stdin"
            }
        }

        try {
            lintFile(
                fileName = fileName,
                fileContents = fileContent,
                ruleSets = astRuleSet,
                visitorProvider = visitorProvider,
                debug = ktlintCommand.debug
            )
        } catch (e: Exception) {
            if (e is ParseException) {
                throw ParseException(e.line, e.col, "Not a valid Kotlin file (${e.message?.toLowerCase()})")
            }
            throw e
        }
    }

    companion object {
        internal const val COMMAND_NAME = "printAST"
    }
}
