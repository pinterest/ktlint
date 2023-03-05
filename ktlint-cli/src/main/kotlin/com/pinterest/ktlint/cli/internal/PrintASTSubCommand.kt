package com.pinterest.ktlint.cli.internal

import com.pinterest.ktlint.logger.api.initKtLintKLogger
import com.pinterest.ktlint.rule.engine.api.Code
import com.pinterest.ktlint.rule.engine.api.KtLintParseException
import com.pinterest.ktlint.rule.engine.api.KtLintRuleEngine
import com.pinterest.ktlint.rule.engine.core.api.RuleProvider
import com.pinterest.ruleset.testtooling.DumpASTRule
import mu.KotlinLogging
import picocli.CommandLine
import java.io.File
import java.nio.file.FileSystems
import java.util.Locale

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
            printAST(Code.fromStdin())
        } else {
            FileSystems
                .getDefault()
                .fileSequence(patterns.ifEmpty { DEFAULT_PATTERNS })
                .map { it.toFile() }
                .forEach {
                    printAST(Code.fromFile(it))
                }
        }
    }

    private fun printAST(code: Code) {
        if (code.fileName != null) {
            LOGGER.debug { "Analyzing ${File(code.fileName!!).location(ktlintCommand.relative)}" }
        }

        try {
            KtLintRuleEngine(
                ruleProviders = setOf(
                    RuleProvider { DumpASTRule(System.out, ktlintCommand.color) },
                ),
            ).lint(code)
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
