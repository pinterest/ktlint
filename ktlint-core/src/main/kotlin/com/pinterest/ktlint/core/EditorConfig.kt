package com.pinterest.ktlint.core

import com.pinterest.ktlint.core.EditorConfig.IndentStyle.SPACE
import com.pinterest.ktlint.core.EditorConfig.IndentStyle.TAB
import com.pinterest.ktlint.core.api.DefaultEditorConfigProperties
import com.pinterest.ktlint.core.api.FeatureInAlphaState
import com.pinterest.ktlint.core.api.UsesEditorConfigProperties
import java.util.Locale
import org.ec4j.core.model.PropertyType
import org.jetbrains.kotlin.com.intellij.lang.ASTNode

/**
 * @see [EditorConfig](https://editorconfig.org/)
 *
 * This class is injected into the user data, so it is available to rules via [KtLint.EDITOR_CONFIG_USER_DATA_KEY]
 */
@Deprecated(
    message = "Marked for removal in Ktlint 0.46. Implement interface UsesEditorConfigProperties on the rule and " +
        "retrieve values via call 'node.getEditorConfigValue(property)'."
)
interface EditorConfig {
    @Deprecated("Marked for removal in Ktlint 0.46. Replace with IndentConfig.IndentStyle")
    enum class IndentStyle { SPACE, TAB }

    @Deprecated("Marked for removal in Ktlint 0.46. Use IndentConfig.indent only or IndentConfig.indentStyle otherwise")
    public val indentStyle: IndentStyle

    @Deprecated("Marked for removal in Ktlint 0.46. Prefer to use IndentConfig.indent.length")
    val indentSize: Int

    @Deprecated("Marked for removal in Ktlint 0.46. Prefer to use IndentConfig.indent.length")
    val tabWidth: Int

    @Deprecated(
        message = "Marked for removal in Ktlint 0.46. Implement interface UsesEditorConfigProperties on the rule and " +
            "retrieve this value via call 'node.getEditorConfigValue(maxLineLengthProperty)'."
    )
    val maxLineLength: Int

    @Deprecated(
        message = "Marked for removal in Ktlint 0.46. Implement interface UsesEditorConfigProperties on the rule and " +
            "retrieve this value via call 'node.getEditorConfigValue(insertNewLineProperty)'."
    )
    val insertFinalNewline: Boolean

    @Deprecated(
        message = "Marked for removal in Ktlint 0.46. Implement interface UsesEditorConfigProperties on the rule and " +
            "retrieve values via call 'node.getEditorConfigValue(property)'."
    )
    operator fun get(key: String): String?

    companion object {
        @Deprecated(
            message = "Marked for removal in Ktlint 0.46. Implement interface UsesEditorConfigProperties on the " +
                "rule and retrieve values via call 'node.getEditorConfigValue(property)'."
        )
        fun fromMap(map: Map<String, String>): EditorConfig {
            val indentStyle = when {
                map["indent_style"]?.lowercase(Locale.getDefault()) == "tab" -> TAB
                else -> SPACE
            }
            val indentSize =
                map["indent_size"]
                    .let { value ->
                        if (value?.lowercase(Locale.getDefault()) == "unset") {
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

        @Deprecated(
            message = "Marked for removal in Ktlint 0.46. The interface UsesEditorConfigProperties needs to be " +
                "implemented first on the rule.",
            replaceWith = ReplaceWith("this.getEditorConfigValue(property)")
        )
        public fun ASTNode.loadEditorConfig(): EditorConfig = getUserData(KtLint.EDITOR_CONFIG_USER_DATA_KEY)!!

        @Deprecated(
            message = "Marked for removal in Ktlint 0.46. Implement interface UsesEditorConfigProperties on the " +
                "rule. Then configure the IndentConfig inside the rule.",
            replaceWith = ReplaceWith(
                "IndentConfig(indentStyle = IndentStyleValue, tabWidth = tabWidth)",
                "com.pinterest.ktlint.core.IndentConfig"
            )
        )
        public fun EditorConfig.loadIndentConfig(): IndentConfig =
            IndentConfig(
                indentStyle = indentStyle,
                tabWidth = tabWidth,
                disabled = indentSize <= 0 || tabWidth <= 0
            )

        @Deprecated("Marked for removal in Ktlint 0.46.")
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

        @Deprecated(
            message = "Marked for removal in Ktlint 0.46",
            replaceWith = ReplaceWith("DefaultEditorConfigProperties.indentStyleProperty")
        )
        @OptIn(FeatureInAlphaState::class)
        public val indentStyleProperty: UsesEditorConfigProperties.EditorConfigProperty<PropertyType.IndentStyleValue> =
            DefaultEditorConfigProperties.indentStyleProperty

        @Deprecated(
            message = "Marked for removal in Ktlint 0.46",
            replaceWith = ReplaceWith("DefaultEditorConfigProperties.indentSizeProperty")
        )
        @OptIn(FeatureInAlphaState::class)
        public val indentSizeProperty: UsesEditorConfigProperties.EditorConfigProperty<Int> =
            DefaultEditorConfigProperties.indentSizeProperty
    }
}
