package com.pinterest.ktlint.internal

import com.pinterest.ktlint.KtlintCommandLine
import java.io.File
import kotlin.system.exitProcess
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

        val gitHooksDir = resolveGitHooksDir()
        val preCommitHookFile = gitHooksDir.resolve("pre-commit")
        val preCommitHook = loadGitPreCommitHookTemplate()

        if (preCommitHookFile.exists()) {
            backupExistingPreCommitHook(gitHooksDir, preCommitHookFile, preCommitHook)
        }

        // > .git/hooks/pre-commit && chmod +x .git/hooks/pre-commit
        preCommitHookFile.writeBytes(preCommitHook)
        preCommitHookFile.setExecutable(true)
        println(".git/hooks/pre-commit installed")
    }

    private fun resolveGitHooksDir(): File {
        val gitDir = File(".git")
        if (!gitDir.isDirectory) {
            System.err.println(
                ".git directory not found. Are you sure you are inside project root directory?"
            )
            exitProcess(1)
        }

        val hooksDir = gitDir.resolve("hooks")
        if (!hooksDir.exists() && !hooksDir.mkdir()) {
            System.err.println("Failed to create .git/hooks folder")
            exitProcess(1)
        }

        return hooksDir
    }

    private fun loadGitPreCommitHookTemplate(): ByteArray = ClassLoader
        .getSystemClassLoader()
        .getResourceAsStream(
            "ktlint-git-pre-commit-hook${if (ktlintCommand.android) "-android" else ""}.sh"
        ).use { it.readBytes() }

    private fun backupExistingPreCommitHook(
        hooksDir: File,
        preCommitHookFile: File,
        expectedPreCommitHook: ByteArray
    ) {
        // backup existing hook (if any)
        val actualPreCommitHook = preCommitHookFile.readBytes()
        if (actualPreCommitHook.isNotEmpty() &&
            !actualPreCommitHook.contentEquals(expectedPreCommitHook)
        ) {
            val backupFile = hooksDir.resolve("pre-commit.ktlint-backup.${actualPreCommitHook.hex}")
            println(".git/hooks/pre-commit -> $backupFile")
            preCommitHookFile.copyTo(backupFile, overwrite = true)
        }
    }

    companion object {
        const val COMMAND_NAME = "installGitPreCommitHook"
    }
}
