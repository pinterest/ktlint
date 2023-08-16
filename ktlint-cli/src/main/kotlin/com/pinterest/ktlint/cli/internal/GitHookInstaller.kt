package com.pinterest.ktlint.cli.internal

import com.pinterest.ktlint.logger.api.initKtLintKLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.File
import java.io.IOException
import java.math.BigInteger
import java.security.MessageDigest

private val LOGGER = KotlinLogging.logger {}.initKtLintKLogger()

private const val DEFAULT_GIT_DIR = ".git"
private const val DEFAULT_GIT_HOOKS_DIR = "hooks"

internal object GitHookInstaller {
    fun installGitHook(
        gitHookName: String,
        hookContentProvider: () -> ByteArray,
    ) {
        val gitHooksDir =
            try {
                resolveGitHooksDir()
            } catch (e: IOException) {
                System.err.println(e.message)
                exitKtLintProcess(1)
            }
        val gitHookFile = gitHooksDir.resolve(gitHookName)
        val hookContent = hookContentProvider()

        if (gitHookFile.exists()) {
            backupExistingHook(gitHooksDir, gitHookFile, hookContent, gitHookName)
        }

        gitHookFile.writeBytes(hookContent)
        gitHookFile.setExecutable(true)
        LOGGER.info {
            "${gitHookFile.path} is installed. Be aware that this hook assumes to find ktlint on the PATH. Either " +
                "ensure that ktlint is actually added to the path or expand the ktlint command in the hook with the " +
                "path."
        }
    }

    @Throws(IOException::class)
    private fun resolveGitHooksDir(): File {
        val gitDir = getGitDir()
        val gitHooksDirName = getHooksDirName()

        val hooksDir = gitDir.resolve(gitHooksDirName)
        if (!hooksDir.exists() && !hooksDir.mkdir()) {
            throw IOException("Failed to create ${hooksDir.path} folder")
        }

        return hooksDir
    }

    // Try to find the .git directory automatically, falling back to `./.git`
    private fun getGitDir(): File {
        val gitDir =
            try {
                with(ProcessBuilder("git", "rev-parse", "--show-toplevel").start()) {
                    val rootDir =
                        inputStream
                            .bufferedReader()
                            .readLine()
                    waitFor()
                    File(rootDir ?: DEFAULT_GIT_DIR).resolve(".git")
                }
            } catch (_: IOException) {
                File(DEFAULT_GIT_DIR)
            }
        if (!gitDir.isDirectory) {
            throw IOException(".git directory not found. Are you sure you are inside project directory?")
        }

        return gitDir
    }

    private fun getHooksDirName() =
        try {
            with(ProcessBuilder("git", "config", "--get", "core.hooksPath").start()) {
                val hooksDir =
                    inputStream
                        .bufferedReader()
                        .readLine()
                waitFor()
                hooksDir
                    ?.trim()
                    .orEmpty()
                    .ifEmpty { DEFAULT_GIT_HOOKS_DIR }
            }
        } catch (_: IOException) {
            DEFAULT_GIT_HOOKS_DIR
        }

    private fun backupExistingHook(
        hooksDir: File,
        hookFile: File,
        expectedHookContent: ByteArray,
        gitHookName: String,
    ) {
        // backup existing hook (if any)
        val actualHookContent = hookFile.readBytes()
        if (actualHookContent.isNotEmpty() &&
            !actualHookContent.contentEquals(expectedHookContent)
        ) {
            val backupFile = hooksDir.resolve("$gitHookName.ktlint-backup.${actualHookContent.toUniqueId()}")
            LOGGER.info { "Existing git hook ${hookFile.path} is copied to ${backupFile.path}" }
            hookFile.copyTo(backupFile, overwrite = true)
        }
    }

    // Generates a unique id based on the byte array
    private fun ByteArray.toUniqueId() =
        MessageDigest
            .getInstance("SHA-256")
            .digest(this)
            .let { BigInteger(it) }
            .toString(16)
}
