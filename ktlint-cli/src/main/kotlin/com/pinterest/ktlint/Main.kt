@file:JvmName("Main")

package com.pinterest.ktlint

import com.pinterest.ktlint.cli.internal.GenerateEditorConfigSubCommand
import com.pinterest.ktlint.cli.internal.GitPreCommitHookSubCommand
import com.pinterest.ktlint.cli.internal.GitPrePushHookSubCommand
import com.pinterest.ktlint.cli.internal.KtlintCommandLine
import com.pinterest.ktlint.cli.internal.PrintASTSubCommand
import com.pinterest.ktlint.cli.internal.printCommandLineHelpOrVersionUsage
import picocli.CommandLine

// Ideally this file would have been moved to the cli package as well. This however is breaking change that is likely to affect each project
// that use either the Maven or Gradle and calls the Ktlint CLI. As those users likely will not read the changelog, this could lead to many
// issues. So the class is to be kept at the old location.
public fun main(args: Array<String>) {
    val ktlintCommand = KtlintCommandLine()
    val commandLine = CommandLine(ktlintCommand)
        .addSubcommand(GitPreCommitHookSubCommand.COMMAND_NAME, GitPreCommitHookSubCommand())
        .addSubcommand(GitPrePushHookSubCommand.COMMAND_NAME, GitPrePushHookSubCommand())
        .addSubcommand(PrintASTSubCommand.COMMAND_NAME, PrintASTSubCommand())
        .addSubcommand(GenerateEditorConfigSubCommand.COMMAND_NAME, GenerateEditorConfigSubCommand())
        // Keep setUsageHelpAutoWidth after all addSubcommands
        .setUsageHelpAutoWidth(true)
    val parseResult = commandLine.parseArgs(*args)

    // The logger needs to be configured for the ktlintCommand and all subcommands. The logger can however not be configured before the
    // commandline has been parsed as otherwise the loglevel conversion is not yet executed.
    ktlintCommand.configureLogger()

    commandLine.printCommandLineHelpOrVersionUsage()

    if (parseResult.hasSubcommand()) {
        handleSubCommand(commandLine, parseResult)
    } else {
        ktlintCommand.run()
    }
}

private fun handleSubCommand(
    commandLine: CommandLine,
    parseResult: CommandLine.ParseResult,
) {
    when (val subCommand = parseResult.subcommand().commandSpec().userObject()) {
        is GitPreCommitHookSubCommand -> subCommand.run()
        is GitPrePushHookSubCommand -> subCommand.run()
        is PrintASTSubCommand -> subCommand.run()
        is GenerateEditorConfigSubCommand -> subCommand.run()
        else -> commandLine.usage(System.out, CommandLine.Help.Ansi.OFF)
    }
}
