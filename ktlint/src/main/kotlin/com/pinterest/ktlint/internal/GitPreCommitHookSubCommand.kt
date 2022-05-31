package com.pinterest.ktlint.internal

import picocli.CommandLine

@CommandLine.Command(
    description = [
        "Install git hook to automatically check files for style violations on commit",
        "Usage of \"--install-git-pre-commit-hook\" command line option is deprecated!"
    ],
    aliases = ["--install-git-pre-commit-hook"],
    mixinStandardHelpOptions = true,
    versionProvider = KtlintVersionProvider::class
)
class GitPreCommitHookSubCommand : Runnable {
    @CommandLine.ParentCommand
    private lateinit var ktlintCommand: KtlintCommandLine

    @CommandLine.Spec
    private lateinit var commandSpec: CommandLine.Model.CommandSpec

    override fun run() {
        commandSpec.commandLine().printHelpOrVersionUsage()

        GitHookInstaller.installGitHook("pre-commit") {
            loadHookContent()
        }
    }

    private fun loadHookContent(): ByteArray = ClassLoader
        .getSystemClassLoader()
        .getResourceAsStream(
            "ktlint-git-pre-commit-hook${if (ktlintCommand.android) "-android" else ""}.sh"
        ).use { it.readBytes() }

    companion object {
        const val COMMAND_NAME = "installGitPreCommitHook"
    }
}
