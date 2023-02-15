package com.pinterest.ktlint.cli.internal

import picocli.CommandLine

@CommandLine.Command(
    description = [
        "Install git hook to automatically check files for style violations before push",
    ],
    mixinStandardHelpOptions = true,
    versionProvider = KtlintVersionProvider::class,
)
internal class GitPrePushHookSubCommand : Runnable {
    @CommandLine.ParentCommand
    private lateinit var ktlintCommand: KtlintCommandLine

    @CommandLine.Spec
    private lateinit var commandSpec: CommandLine.Model.CommandSpec

    override fun run() {
        commandSpec.commandLine().printCommandLineHelpOrVersionUsage()

        if (ktlintCommand.codeStyle == null) {
            System.err.println("Option --code-style must be set as to generate the git pre push hook correctly")
            exitKtLintProcess(1)
        }

        GitHookInstaller.installGitHook("pre-push") {
            """
            #!/bin/sh

            # <https://github.com/pinterest/ktlint> pre-push hook

            git diff --name-only -z HEAD "origin/${'$'}(git rev-parse --abbrev-ref HEAD)" -- '*.kt' '*.kts' | ktlint --code-style=${ktlintCommand.codeStyle} --relative --patterns-from-stdin=''
            """.trimIndent().toByteArray()
        }
    }

    internal companion object {
        internal const val COMMAND_NAME = "installGitPrePushHook"
    }
}
