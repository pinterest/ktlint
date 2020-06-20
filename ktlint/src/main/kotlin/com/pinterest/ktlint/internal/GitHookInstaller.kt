package com.pinterest.ktlint.internal

import java.io.File
import java.io.IOException
import kotlin.system.exitProcess

object GitHookInstaller {
    fun installGitHook(
        gitHookName: String,
        gitHookLoader: () -> ByteArray
    ) {
        val gitHooksDir = try {
            resolveGitHooksDir()
        } catch (e: IOException) {
            System.err.println(e.message)
            exitProcess(1)
        }

        val gitHookFile = gitHooksDir.resolve(gitHookName)
        val hookContent = gitHookLoader()

        if (gitHookFile.exists()) {
            backupExistingHook(gitHooksDir, gitHookFile, hookContent, gitHookName)
        }

        gitHookFile.writeBytes(hookContent)
        gitHookFile.setExecutable(true)
        println(".git/hooks/$gitHookName installed")
    }

    @Throws(IOException::class)
    private fun resolveGitHooksDir(): File {
        // Try to find the .git directory automatically, falling back to `./.git`
        val gitDir = try {
            val root = Runtime.getRuntime().exec("git rev-parse --show-toplevel")
                .inputStream
                .bufferedReader()
                .readText()
                .trim()

            File(root).resolve(".git")
        } catch (ex: IOException) {
            File(".git")
        }

        if (!gitDir.isDirectory) {
            throw IOException(".git directory not found. Are you sure you are inside project directory?")
        }

        val hooksDir = gitDir.resolve("hooks")
        if (!hooksDir.exists() && !hooksDir.mkdir()) {
            throw IOException("Failed to create .git/hooks folder")
        }

        return hooksDir
    }

    private fun backupExistingHook(
        hooksDir: File,
        hookFile: File,
        expectedHookContent: ByteArray,
        gitHookName: String
    ) {
        // backup existing hook (if any)
        val actualHookContent = hookFile.readBytes()
        if (actualHookContent.isNotEmpty() &&
            !actualHookContent.contentEquals(expectedHookContent)
        ) {
            val backupFile = hooksDir.resolve("$gitHookName.ktlint-backup.${actualHookContent.hex}")
            println(".git/hooks/$gitHookName -> $backupFile")
            hookFile.copyTo(backupFile, overwrite = true)
        }
    }
}
