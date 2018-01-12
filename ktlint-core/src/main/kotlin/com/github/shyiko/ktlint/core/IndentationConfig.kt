package com.github.shyiko.ktlint.core

import org.jetbrains.kotlin.com.intellij.lang.ASTNode

data class IndentationConfig(val regular: Int, val continuation: Int, val disabled: Boolean) {
    companion object {
        private const val DEFAULT_INDENT = 4
        private const val DEFAULT_ANDROID_CONTINUATION_INDENT = 8
        const val REGULAR_KEY = "indent_size"
        const val CONTINUATION_KEY = "continuation_indent_size"

        fun create(node: ASTNode): IndentationConfig {
            val android = node.getUserData(KtLint.ANDROID_USER_DATA_KEY)!!
            val editorConfig = node.getUserData(KtLint.EDITOR_CONFIG_USER_DATA_KEY)!!
            val indentSize = editorConfig.get(REGULAR_KEY)
            val continuationIndentSize = editorConfig.get(CONTINUATION_KEY)
            val regular = indentSize?.toIntOrNull() ?: DEFAULT_INDENT
            val continuation = continuationIndentSize?.toIntOrNull() ?: defaultContinuationIndent(android)
            val disabled = indentSize?.toLowerCase() == "unset" || continuationIndentSize?.toLowerCase() == "unset"
            return IndentationConfig(regular = regular, continuation = continuation, disabled = disabled)
        }

        private fun defaultContinuationIndent(isAndroid: Boolean): Int {
            return if (isAndroid) {
                DEFAULT_ANDROID_CONTINUATION_INDENT
            } else {
                DEFAULT_INDENT
            }
        }
    }
}
