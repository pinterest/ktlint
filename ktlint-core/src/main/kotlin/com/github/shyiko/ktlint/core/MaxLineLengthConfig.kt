package com.github.shyiko.ktlint.core

import org.jetbrains.kotlin.com.intellij.lang.ASTNode

data class MaxLineLengthConfig(val lineLength: Int) {

    fun isDisabled(): Boolean = lineLength <= 0
    fun isEnabled(): Boolean = !isDisabled()

    companion object {
        private const val DEFAULT_ANDROID_MAX_LINE_SIZE = 100
        private const val DEFAULT_MAX_LINE_SIZE = 0
        private const val KEY_MAX_LINE_LENGTH = "max_line_length"
        fun create(node: ASTNode): MaxLineLengthConfig {
            val android = node.getUserData(KtLint.ANDROID_USER_DATA_KEY)!!
            val editorConfig = node.getUserData(KtLint.EDITOR_CONFIG_USER_DATA_KEY)!!
            val maxLineLength = editorConfig.get(KEY_MAX_LINE_LENGTH)?.toIntOrNull() ?: defaultLineSize(android)
            return MaxLineLengthConfig(maxLineLength)
        }

        private fun defaultLineSize(isAndroid: Boolean): Int {
            return if (isAndroid) {
                DEFAULT_ANDROID_MAX_LINE_SIZE
            } else {
                DEFAULT_MAX_LINE_SIZE
            }
        }
    }
}
