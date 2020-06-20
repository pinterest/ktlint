package com.pinterest.ktlint.internal

import com.pinterest.ktlint.KtlintCommandLine
import com.pinterest.ktlint.core.KtLint
import com.pinterest.ktlint.core.ParseException
import com.pinterest.ktlint.core.RuleSet
import com.pinterest.ktlint.test.DumpAST
import java.io.File
import picocli.CommandLine

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
            RuleSet("debug", DumpAST(System.out, ktlintCommand.color))
        )
    }

    override fun run() {
        commandSpec.commandLine().printHelpOrVersionUsage()

        if (stdin) {
            printAST(KtLint.STDIN_FILE, String(System.`in`.readBytes()))
        } else {
            for (file in patterns.fileSequence()) {
                printAST(file.path, file.readText())
            }
        }
    }

    private fun printAST(
        fileName: String,
        fileContent: String
    ) {
        if (ktlintCommand.debug) {
            val fileLocation = if (fileName != KtLint.STDIN_FILE) {
                File(fileName).location(ktlintCommand.relative)
            } else {
                "stdin"
            }
            println("[DEBUG] Analyzing $fileLocation")
        }

        try {
            lintFile(fileName, fileContent, astRuleSet, debug = ktlintCommand.debug)
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
