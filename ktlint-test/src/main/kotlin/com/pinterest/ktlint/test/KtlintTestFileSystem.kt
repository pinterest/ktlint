package com.pinterest.ktlint.test

import com.google.common.jimfs.Configuration
import com.google.common.jimfs.Jimfs
import java.nio.file.FileSystem
import java.nio.file.Files
import java.nio.file.Path

/**
 * Creates a file system based on [Jimfs] with helper methods to create and resolve files
 */
public class KtlintTestFileSystem(
    public val rootDirectory: String = "/project",
) {
    public val fileSystem: FileSystem = Jimfs.newFileSystem(Configuration.forCurrentPlatform())

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
        Files.createDirectories(pathFromRoot(relativeDirectoryToRoot))
        Files.write(pathFromRoot("$relativeDirectoryToRoot/$fileName"), content.toByteArray())
    }

    /**
     * Creates a file with path [filePath] with given [content]. Note, [filePath] is not necessarily relative to the [rootDirectory].
     */
    public fun writeFile(
        filePath: String,
        content: String,
    ) {
        pathFromFileSystemRoot(filePath)
            .let { path ->
                Files.createDirectories(path.parent)
                Files.write(path, content.toByteArray())
            }
    }

    private fun pathFromRoot(pathRelativeToRoot: String = ""): Path = pathFromFileSystemRoot("$rootDirectory/$pathRelativeToRoot")

    private fun pathFromFileSystemRoot(path: String = ""): Path =
        fileSystem
            // On Windows OS the exception below is thrown when not taking the file system root directory into account:
            //     java.nio.file.InvalidPathException: Jimfs does not currently support the Windows syntax for an absolute path on the
            //     current drive (e.g. "\foo\bar"): /project/
            // So first resolve the path starting from the first root directory of the filesystem (not be confused wit the rootDirectory
            // property of the KtLintTestFileSystem)
            .rootDirectories
            .first()
            .resolve(path)

    /**
     * Resolves the path relative to the root of the mock file system.
     */
    public fun resolve(pathRelativeToRootDirectory: String): Path =
        pathFromRoot()
            .resolve(pathRelativeToRootDirectory)

    /**
     * Closes the mock file system
     */
    public fun close(): Unit = fileSystem.close()
}
