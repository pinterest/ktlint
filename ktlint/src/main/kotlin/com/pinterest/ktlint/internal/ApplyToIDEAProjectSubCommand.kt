package com.pinterest.ktlint.internal

import picocli.CommandLine

@CommandLine.Command(
    description = [
        "Update Intellij IDEA project settings"
    ],
    aliases = ["--apply-to-idea-project"],
    mixinStandardHelpOptions = true,
    versionProvider = KtlintVersionProvider::class
)
class ApplyToIDEAProjectSubCommand : Runnable {
    @CommandLine.ParentCommand
    private lateinit var ktlintCommand: KtlintCommandLine

    @CommandLine.Spec
    private lateinit var commandSpec: CommandLine.Model.CommandSpec

    @CommandLine.Option(
        names = ["-y"],
        description = ["Overwrite existing Kotlin codestyle settings without asking"]
    )
    private var forceApply: Boolean = false

    override fun run() {
        commandSpec.commandLine().printHelpOrVersionUsage()

        ApplyToIDEACommandHelper(
            true,
            forceApply,
            ktlintCommand.android
        ).apply()
    }

    companion object {
        const val COMMAND_NAME = "applyToIDEAProject"
    }
}
