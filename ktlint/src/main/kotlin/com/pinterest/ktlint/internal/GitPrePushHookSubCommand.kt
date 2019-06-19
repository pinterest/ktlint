package com.pinterest.ktlint.internal

import com.pinterest.ktlint.KtlintCommandLine
import picocli.CommandLine

@CommandLine.Command(
    description = [
        "Install git hook to automatically check files for style violations before push",
        "Usage of \"--install-git-pre-push-hook\" command line option is deprecated!"
    ],
    aliases = ["--install-git-pre-push-hook"],
    mixinStandardHelpOptions = true,
    versionProvider = KtlintVersionProvider::class
)
class GitPrePushHookSubCommand : Runnable {
    @CommandLine.ParentCommand
    private lateinit var ktlintCommand: KtlintCommandLine

    @CommandLine.Spec
    private lateinit var commandSpec: CommandLine.Model.CommandSpec

    override fun run() {
        commandSpec.commandLine().printHelpOrVersionUsage()

        GitHookInstaller.installGitHook("pre-push") {
            loadHookContent()
        }
    }

    private fun loadHookContent() = ClassLoader
        .getSystemClassLoader()
        .getResourceAsStream(
            "ktlint-git-pre-push-hook${if (ktlintCommand.android) "-android" else ""}.sh"
        )
        .readBytes()

    companion object {
        const val COMMAND_NAME = "installGitPrePushHook"
    }
}
