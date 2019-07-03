package com.pinterest.ktlint.core

/**
 * @see [EditorConfig](http://editorconfig.org/)
 *
 * This class is injected into the user data, so it is available to rules via [KtLint.EDITOR_CONFIG_USER_DATA_KEY]
 */
interface EditorConfig {

    enum class IndentStyle { SPACE, TAB }

    val indentStyle: IndentStyle
    val indentSize: Int
    val tabWidth: Int
    val maxLineLength: Int
    val insertFinalNewline: Boolean
    fun get(key: String): String?

    companion object {
        fun fromMap(map: Map<String, String>): EditorConfig {
            val indentStyle = when {
                map["indent_style"]?.toLowerCase() == "tab" -> IndentStyle.TAB
                else -> IndentStyle.SPACE
            }
            val indentSize = map["indent_size"].let { v ->
                if (v?.toLowerCase() == "unset") -1 else v?.toIntOrNull() ?: 4
            }
            val tabWidth = map["indent_size"]?.toIntOrNull()
            val maxLineLength = map["max_line_length"]?.toIntOrNull() ?: -1
            val insertFinalNewline = map["insert_final_newline"]?.toBoolean() ?: true
            return object : EditorConfig {
                override val indentStyle = indentStyle
                override val indentSize = indentSize
                override val tabWidth = tabWidth ?: indentSize
                override val maxLineLength = maxLineLength
                override val insertFinalNewline = insertFinalNewline
                override fun get(key: String): String? = map[key]
            }
        }
    }
}
