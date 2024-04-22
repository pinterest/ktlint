package com.pinterest.ktlint.test

import com.google.common.jimfs.Configuration
import com.google.common.jimfs.Jimfs
import com.pinterest.ktlint.logger.api.initKtLintKLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import org.jetbrains.kotlin.util.suffixIfNot
import java.nio.file.FileSystem
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.pathString

private val LOGGER = KotlinLogging.logger {}.initKtLintKLogger()

/**
 * Creates a file system based on [Jimfs] with helper methods to create and resolve files
 */
public class KtlintTestFileSystem(
    private val rootDirectory: String = "/project",
) {
    private val useNativeFileSystem = true // Set to false for running with WindowsOS on a non-Windows machine but DO NOT COMMIT

    public val fileSystem: FileSystem =
        if (useNativeFileSystem) {
            Jimfs.newFileSystem(Configuration.forCurrentPlatform())
        } else {
            // Windows OS use a drive letter followed by a colon designate an absolute path. Testing this on a non-native machine is hard.
            // Relying on the build street which run for both Unix and Windows platforms is slow and cumbersome.
            Jimfs
                .newFileSystem(Configuration.windows())
                .also {
                    LOGGER.warn {
                        """
                        ${this::class.simpleName} has been overridden and set to Windows Platform. This should only be used for testing on
                        the local machine running on a different OS. Also system property 'os.name' is set to 'windows' as some classes use
                        this property to detect the Windows OS. Unit tests annotated with '@DisabledOnOs(OS.WINDOWS)' will still be executed
                        and likely will fail.
                        """.trimIndent()
                    }
                    System.setProperty("os.name", "windows")
                }
        }

    private val fileSystemRootPath =
        fileSystem
            .rootDirectories
            .first()

    private val rootDirectoryPathString = operatingSystemPath(rootDirectory).pathString

    /**
     * Creates the ".editorconfig" file in the root directory with given [content].
     */
    public fun writeRootEditorConfigFile(content: String): Unit = writeEditorConfigFile("", content)

    /**
     * Creates a file with name [editorConfigFileName] in directory [relativeDirectoryToRoot] with given [content].
     */
    public fun writeEditorConfigFile(
        relativeDirectoryToRoot: String,
        content: String,
        editorConfigFileName: String = ".editorconfig",
    ): Unit = writeFile(relativeDirectoryToRoot, editorConfigFileName, content)

    /**
     * Creates a file with name [fileName] in directory [relativeDirectoryToRoot] with given [content].
     */
    public fun writeFile(
        relativeDirectoryToRoot: String,
        fileName: String,
        content: String,
    ) {
        Files.createDirectories(operatingSystemPath(rootDirectory, relativeDirectoryToRoot))
        Files.write(
            operatingSystemPath(rootDirectory, relativeDirectoryToRoot, fileName),
            content.toByteArray(),
        )
    }

    /**
     * Creates a file with path [filePath] with given [content]. Note, [filePath] is not necessarily relative to the [rootDirectory].
     */
    public fun writeFile(
        filePath: String,
        content: String,
    ) {
        operatingSystemPath(filePath)
            .let { path ->
                Files.createDirectories(path.parent)
                Files.write(path, content.toByteArray())
            }
    }

    private fun operatingSystemPath(vararg segments: String?) =
        segments
            .filterNotNull()
            .filter { it.isNotBlank() }
            .toList()
            .joinToString(separator = fileSystem.separator) { segment ->
                segment
                    .replace("/", fileSystem.separator)
                    .removePrefix(fileSystem.separator)
                    .removePrefix(fileSystem.separator)
            }.let { path ->
                // On Windows OS the exception below is thrown when not taking the file system root directory into account:
                //     java.nio.file.InvalidPathException: Jimfs does not currently support the Windows syntax for an absolute path on the
                //     current drive (e.g. "\foo\bar"): /project/
                // So first resolve the path starting from the first root directory of the filesystem (not be confused with the rootDirectory
                // property of the KtLintTestFileSystem)
                fileSystemRootPath.resolve(path)
            }

    public fun unixPathStringRelativeToRootDirectoryOfFileSystem(nativePath: String): String =
        nativePath
            .removePrefix(rootDirectoryPathString.suffixIfNot(fileSystem.separator))
            .replace(fileSystem.separator, "/")

    /**
     * Resolves the path relative to the root of the mock file system.
     */
    public fun resolve(pathRelativeToRootDirectory: String? = null): Path = operatingSystemPath(rootDirectory, pathRelativeToRootDirectory)

    /**
     * Closes the mock file system
     */
    public fun close(): Unit = fileSystem.close()
}
