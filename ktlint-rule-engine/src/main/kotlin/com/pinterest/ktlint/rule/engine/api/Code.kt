package com.pinterest.ktlint.rule.engine.api

import com.pinterest.ktlint.rule.engine.api.Code.Companion.fromFile
import com.pinterest.ktlint.rule.engine.api.Code.Companion.fromPath
import com.pinterest.ktlint.rule.engine.api.Code.Companion.fromSnippet
import com.pinterest.ktlint.rule.engine.api.Code.Companion.fromStdin
import com.pinterest.ktlint.rule.engine.api.KtLintRuleEngine.Companion.STDIN_FILE
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
    public fun fileNameOrStdin(): String =
        if (isStdIn) {
            STDIN_FILE
        } else {
            fileName.orEmpty()
        }

    public fun filePathOrStdin(): String =
        if (isStdIn) {
            STDIN_FILE
        } else {
            filePath?.pathString.orEmpty()
        }

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
         * The [content] represent a valid piece of Kotlin code or Kotlin script. The '.editorconfig' file on the filesystem is based on
         * the [virtualPath]. The filesystem is not expected to actually contain a file with this name. Use [Code.fromFile] for scanning a
         * file that does exist on the filesystem.
         */
        public fun fromSnippetWithPath(
            /**
             * Code to be linted/formatted.
             */
            content: String,
            /**
             * Virtual path of file. Contents of the file is *not* read. The path is only used to determine the '.editorconfig' file
             * containing the configuration to be applied on the code that is to be linted/formatted. When not specified, no '.editorconfig'
             * is loaded at all.
             */
            virtualPath: Path? = null,
        ): Code =
            Code(
                content = content,
                filePath = virtualPath,
                fileName = null,
                script = virtualPath?.pathString.orEmpty().endsWith(".kts", ignoreCase = true),
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
