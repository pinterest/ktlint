package com.pinterest.ktlint.core

import com.pinterest.ktlint.core.EditorConfig.IndentStyle.SPACE
import com.pinterest.ktlint.core.EditorConfig.IndentStyle.TAB
import com.pinterest.ktlint.core.api.FeatureInAlphaState
import com.pinterest.ktlint.core.api.UsesEditorConfigProperties
import org.ec4j.core.model.PropertyType
import org.jetbrains.kotlin.com.intellij.lang.ASTNode

/**
 * @see [EditorConfig](https://editorconfig.org/)
 *
 * This class is injected into the user data, so it is available to rules via [KtLint.EDITOR_CONFIG_USER_DATA_KEY]
 */
interface EditorConfig {
    @Deprecated("Replace with IndentConfig.IndentStyle")
    enum class IndentStyle { SPACE, TAB }

    @Deprecated("Prefer to use IndentConfig.indent only or IndentConfig.indentStyle otherwise")
    public val indentStyle: IndentStyle

    @Deprecated("Prefer to use IndentConfig.indent.length")
    val indentSize: Int

    @Deprecated("Prefer to use IndentConfig.indent.length")
    val tabWidth: Int

    val maxLineLength: Int

    @Deprecated(
        message = "Not used anymore by rules, please use 'insert_final_newline' directly via get()"
    )
    val insertFinalNewline: Boolean

    operator fun get(key: String): String?

    companion object {
        fun fromMap(map: Map<String, String>): EditorConfig {
            val indentStyle = when {
                map["indent_style"]?.toLowerCase() == "tab" -> TAB
                else -> SPACE
            }
            val indentSize =
                map["indent_size"]
                    .let { value ->
                        if (value?.toLowerCase() == "unset") {
                            -1
                        } else {
                            value?.toIntOrNull() ?: IndentConfig.DEFAULT_INDENT_CONFIG.tabWidth
                        }
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

        public fun ASTNode.loadEditorConfig(): EditorConfig = getUserData(KtLint.EDITOR_CONFIG_USER_DATA_KEY)!!

        public fun EditorConfig.loadIndentConfig(): IndentConfig =
            IndentConfig(
                indentStyle = when (indentStyle) {
                    TAB -> IndentConfig.IndentStyle.TAB
                    SPACE -> IndentConfig.IndentStyle.SPACE
                },
                tabWidth = tabWidth,
                disabled = indentSize <= 0 || tabWidth <= 0
            )

        /**
         * Use this value to define a non-nullable class variable of type EditorConfig. When loading the root node in
         * rule visitor, the value should be replaced with the real value.
         */
        public val UNINITIALIZED: EditorConfig = object : EditorConfig {
            override val indentStyle: IndentStyle
                get() = throw EditorConfigNotInitializedException()
            override val indentSize: Int
                get() = throw EditorConfigNotInitializedException()
            override val tabWidth: Int
                get() = throw EditorConfigNotInitializedException()
            override val maxLineLength: Int
                get() = throw EditorConfigNotInitializedException()
            override val insertFinalNewline: Boolean
                get() = throw EditorConfigNotInitializedException()
            override fun get(key: String): String? {
                throw EditorConfigNotInitializedException()
            }

            inner class EditorConfigNotInitializedException : RuntimeException("EditorConfig is not yet initialized")
        }

        @OptIn(FeatureInAlphaState::class)
        public val indentStyleProperty: UsesEditorConfigProperties.EditorConfigProperty<PropertyType.IndentStyleValue> =
            UsesEditorConfigProperties.EditorConfigProperty(
                type = PropertyType.indent_style,
                defaultValue = PropertyType.IndentStyleValue.space
            )

        @OptIn(FeatureInAlphaState::class)
        public val indentSizeProperty: UsesEditorConfigProperties.EditorConfigProperty<Int> =
            UsesEditorConfigProperties.EditorConfigProperty(
                type = PropertyType.indent_size,
                defaultValue = IndentConfig.DEFAULT_INDENT_CONFIG.tabWidth
            )
    }
}
