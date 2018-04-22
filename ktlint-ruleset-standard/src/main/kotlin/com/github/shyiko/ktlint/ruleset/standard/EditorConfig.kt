package com.github.shyiko.ktlint.ruleset.standard

import com.github.shyiko.ktlint.core.KtLint
import org.jetbrains.kotlin.com.intellij.lang.FileASTNode

// http://editorconfig.org/
internal data class EditorConfig(
    val indentSize: Int,
    val continuationIndentSize: Int,
    val maxLineLength: Int,
    val insertFinalNewline: Boolean?
) {

    companion object {

        private const val DEFAULT_INDENT = 4

        // https://android.github.io/kotlin-guides/style.html#line-wrapping
        private const val ANDROID_MAX_LINE_LENGTH = 100

        fun from(node: FileASTNode): EditorConfig {
            val editorConfig = node.getUserData(KtLint.EDITOR_CONFIG_USER_DATA_KEY)!!
            val indentSizeRaw = editorConfig.get("indent_size")
            val indentSize = when {
                indentSizeRaw?.toLowerCase() == "unset" -> -1
                else -> indentSizeRaw?.toIntOrNull() ?: DEFAULT_INDENT
            }
            val continuationIndentSizeRaw = editorConfig.get("continuation_indent_size")
            val continuationIndentSize = when {
                continuationIndentSizeRaw?.toLowerCase() == "unset" -> -1
                else -> continuationIndentSizeRaw?.toIntOrNull() ?: indentSize
            }
            val android = node.getUserData(KtLint.ANDROID_USER_DATA_KEY)!!
            val maxLineLength = editorConfig.get("max_line_length")?.toIntOrNull()
                ?: if (android) ANDROID_MAX_LINE_LENGTH else -1
            val insertFinalNewline = editorConfig.get("insert_final_newline")?.toBoolean()
            return EditorConfig(indentSize, continuationIndentSize, maxLineLength, insertFinalNewline)
        }
    }
}
