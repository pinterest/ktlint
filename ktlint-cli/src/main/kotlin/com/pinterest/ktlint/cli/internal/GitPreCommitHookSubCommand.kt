package com.pinterest.ktlint.cli.internal

import picocli.CommandLine

@CommandLine.Command(
    description = ["Install git hook to automatically check files for style violations on commit"],
    mixinStandardHelpOptions = true,
    versionProvider = KtlintVersionProvider::class,
)
internal class GitPreCommitHookSubCommand : Runnable {
    @CommandLine.Spec
    private lateinit var commandSpec: CommandLine.Model.CommandSpec

    override fun run() {
        commandSpec.commandLine().printCommandLineHelpOrVersionUsage()

        GitHookInstaller.installGitHook("pre-commit") {
            """
            #!/bin/sh

            # <https://github.com/pinterest/ktlint> pre-commit hook

            git diff --name-only -z --cached --relative -- '*.kt' '*.kts' | ktlint --relative --patterns-from-stdin=''
            """.trimIndent().toByteArray()
        }
    }

    internal companion object {
        internal const val COMMAND_NAME = "installGitPreCommitHook"
    }
}
