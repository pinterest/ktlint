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
        Files.createDirectories(fileSystem.getPath(filePath).parent)
        Files.write(fileSystem.getPath(filePath), content.toByteArray())
    }

    private fun pathFromRoot(pathRelativeToRoot: String = ""): Path = fileSystem.getPath("$rootDirectory/$pathRelativeToRoot")

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
