package com.pinterest.ktlint.cli.internal

import picocli.CommandLine

@CommandLine.Command(
    description = ["Install git hook to automatically check files for style violations before push"],
    mixinStandardHelpOptions = true,
    versionProvider = KtlintVersionProvider::class,
)
internal class GitPrePushHookSubCommand : Runnable {
    @CommandLine.Spec
    private lateinit var commandSpec: CommandLine.Model.CommandSpec

    override fun run() {
        commandSpec.commandLine().printCommandLineHelpOrVersionUsage()

        GitHookInstaller.installGitHook("pre-push") {
            """
            #!/bin/sh

            # <https://github.com/pinterest/ktlint> pre-push hook

            git diff --name-only -z HEAD "origin/${'$'}(git rev-parse --abbrev-ref HEAD)" -- '*.kt' '*.kts' | ktlint --relative --patterns-from-stdin=''
            """.trimIndent().toByteArray()
        }
    }

    internal companion object {
        internal const val COMMAND_NAME = "installGitPrePushHook"
    }
}
