@file:JvmName("Main")

package com.pinterest.ktlint

import com.pinterest.ktlint.internal.ApplyToIDEAGloballySubCommand
import com.pinterest.ktlint.internal.ApplyToIDEAProjectSubCommand
import com.pinterest.ktlint.internal.GenerateEditorConfigSubCommand
import com.pinterest.ktlint.internal.GitPreCommitHookSubCommand
import com.pinterest.ktlint.internal.GitPrePushHookSubCommand
import com.pinterest.ktlint.internal.KtlintCommandLine
import com.pinterest.ktlint.internal.PrintASTSubCommand
import com.pinterest.ktlint.internal.printHelpOrVersionUsage
import picocli.CommandLine

fun main(args: Array<String>) {
    val ktlintCommand = KtlintCommandLine()
    val commandLine = CommandLine(ktlintCommand)
        .addSubcommand(GitPreCommitHookSubCommand.COMMAND_NAME, GitPreCommitHookSubCommand())
        .addSubcommand(GitPrePushHookSubCommand.COMMAND_NAME, GitPrePushHookSubCommand())
        .addSubcommand(PrintASTSubCommand.COMMAND_NAME, PrintASTSubCommand())
        .addSubcommand(ApplyToIDEAGloballySubCommand.COMMAND_NAME, ApplyToIDEAGloballySubCommand())
        .addSubcommand(ApplyToIDEAProjectSubCommand.COMMAND_NAME, ApplyToIDEAProjectSubCommand())
        .addSubcommand(GenerateEditorConfigSubCommand.COMMAND_NAME, GenerateEditorConfigSubCommand())
    val parseResult = commandLine.parseArgs(*args)

    commandLine.printHelpOrVersionUsage()

    if (parseResult.hasSubcommand()) {
        handleSubCommand(commandLine, parseResult)
    } else {
        ktlintCommand.run()
    }
}

private fun handleSubCommand(
    commandLine: CommandLine,
    parseResult: CommandLine.ParseResult
) {
    when (val subCommand = parseResult.subcommand().commandSpec().userObject()) {
        is GitPreCommitHookSubCommand -> subCommand.run()
        is GitPrePushHookSubCommand -> subCommand.run()
        is PrintASTSubCommand -> subCommand.run()
        is ApplyToIDEAGloballySubCommand -> subCommand.run()
        is ApplyToIDEAProjectSubCommand -> subCommand.run()
        is GenerateEditorConfigSubCommand -> subCommand.run()
        else -> commandLine.usage(System.out, CommandLine.Help.Ansi.OFF)
    }
}
