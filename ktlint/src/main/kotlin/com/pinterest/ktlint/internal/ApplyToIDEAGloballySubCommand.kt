package com.pinterest.ktlint.internal

import com.pinterest.ktlint.KtlintCommandLine
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.system.exitProcess
import picocli.CommandLine

@CommandLine.Command(
    description = [
        "Update Intellij IDEA Kotlin codestyle settings (global)"
    ],
    aliases = ["--apply-to-idea"],
    mixinStandardHelpOptions = true,
    versionProvider = KtlintVersionProvider::class
)
class ApplyToIDEAGloballySubCommand : Runnable {
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

        try {
            val workDir = Paths.get(".")

            if (!forceApply && !getUserAcceptanceToUpdateFiles(workDir)) {
                println("Update canceled.")
                exitProcess(1)
            }

            IntellijIDEAIntegration.apply(workDir, false, ktlintCommand.android, false)
        } catch (e: IntellijIDEAIntegration.ProjectNotFoundException) {
            println(".idea directory not found. Are you sure you are inside project root directory?")
            exitProcess(1)
        }

        println(
            """
            |Updated.
            |Please restart your IDE.
            |If you experience any issues please report them at https://github.com/pinterest/ktlint/issues.
            """.trimMargin()
        )
    }

    private fun getUserAcceptanceToUpdateFiles(workDir: Path): Boolean {
        val fileList = IntellijIDEAIntegration.apply(workDir, true, ktlintCommand.android, false)
        println(
            """
            |The following files are going to be updated:
            |${fileList.joinToString(prefix = "\t", separator = "\n\t")}
            |
            |Do you wish to proceed? [y/n]
            |(in future, use -y flag if you wish to skip confirmation)
            """.trimMargin()
        )

        val userInput = generateSequence { readLine() }
            .filter { it.trim().isNotBlank() }
            .first()

        return "y".equals(userInput, ignoreCase = true)
    }

    companion object {
        const val COMMAND_NAME = "applyToIDEA"
    }
}
