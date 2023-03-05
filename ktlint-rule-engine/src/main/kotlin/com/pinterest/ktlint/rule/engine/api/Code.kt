package com.pinterest.ktlint.rule.engine.api

import org.jetbrains.kotlin.konan.file.file
import java.io.File
import java.nio.file.Path
import kotlin.io.path.pathString

/**
 * A representation of a block of code. Use one of the factory methods [fromFile], [fromPath], [fromSnippet] or [fromStdin] to instantiate.
 */
public class Code private constructor(
    public val content: String,
    public val fileName: String?,
    public val filePath: Path?,
    public val script: Boolean,
    public val isStdIn: Boolean,
) {
    public companion object {
        /**
         * Create [Code] from a [file] containing valid Kotlin code or script. The '.editorconfig' files on the path to [file] are taken
         * into account.
         */
        public fun fromFile(file: File): Code =
            Code(
                content = file.readText(),
                fileName = file.name,
                filePath = file.toPath(),
                script = file.name.endsWith(".kts", ignoreCase = true),
                isStdIn = false,
            )

        /**
         * Create [Code] from a [path] to a file containing valid Kotlin code or script. The '.editorconfig' files on the path to [file] are
         * taken into account. This method is intended to be used in unit tests. In order to work with the Ktlint test file system it needs
         * to make additional call to get the file system which makes it slower compared to [fromFile]. Prefer to use [fromFile].
         */
        public fun fromPath(path: Path): Code {
            // Resolve the file based on the file system of the original path given.
            val file =
                path
                    .fileSystem
                    .file(path.pathString)
            return Code(
                content = file.readStrings().joinToString(separator = "\n"),
                fileName = file.name,
                filePath = path,
                script = file.name.endsWith(".kts", ignoreCase = true),
                isStdIn = false,
            )
        }

        /**
         * The [content] represent a valid piece of Kotlin code or Kotlin script. The '.editorconfig' files on the filesystem are ignored as
         * the snippet is not associated with a file path. Use [Code.fromFile] for scanning a file while at the same time respecting the
         * '.editorconfig' files on the path to the file.
         */
        public fun fromSnippet(
            content: String,
            script: Boolean = false,
        ): Code =
            Code(
                content = content,
                filePath = null,
                fileName = null,
                script = script,
                isStdIn = true,
            )

        /**
         * Create [Code] by reading the snippet from 'stdin'. No '.editorconfig' are taken into account.  The '.editorconfig' files on the
         * filesystem are ignored as the snippet is not associated with a file path. Use [Code.fromFile] for scanning a file while at the
         * same time respecting the '.editorconfig' files on the path to the file.
         */
        public fun fromStdin(): Code = fromSnippet(String(System.`in`.readBytes()))
    }
}
