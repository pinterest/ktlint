package com.pinterest.ktlint.core.api

import com.pinterest.ktlint.core.IndentConfig
import com.pinterest.ktlint.core.KtLint
import com.pinterest.ktlint.core.initKtLintKLogger
import mu.KotlinLogging
import org.ec4j.core.model.Property
import org.ec4j.core.model.PropertyType
import org.jetbrains.kotlin.com.intellij.lang.ASTNode

private val logger = KotlinLogging.logger {}.initKtLintKLogger()

/**
 * Indicates [com.pinterest.ktlint.core.Rule] uses properties loaded from `.editorconfig` file.
 *
 * This properties could be:
 * - universal `.editorconfig` properties defined
 * [here](https://github.com/editorconfig/editorconfig/wiki/EditorConfig-Properties#current-universal-properties)
 * - universal IntelliJ IDEA properties defined
 * [here](https://github.com/JetBrains/intellij-community/blob/master/platform/lang-api/src/com/intellij/psi/codeStyle/CommonCodeStyleSettings.java)
 * - Kotlin specific properties defined
 * [here](https://github.com/JetBrains/kotlin/blob/master/idea/formatter/src/org/jetbrains/kotlin/idea/core/formatter/KotlinCodeStyleSettings.java)
 *
 * In the best case rule should only use one property.
 *
 * See [com.pinterest.ktlint.core.KtLint.generateKotlinEditorConfigSection] documentation how to generate
 * `.editorconfig` based on [com.pinterest.ktlint.core.Rule]s with this interface implementations.
 */
public interface UsesEditorConfigProperties {

    /**
     * Provide a list of code style editorconfig properties, that rule uses in linting.
     */
    public val editorConfigProperties: List<EditorConfigProperty<*>>

    /**
     * Get the value of [EditorConfigProperty] based on loaded [EditorConfigProperties] content for the current
     * [ASTNode].
     */
    public fun <T> ASTNode.getEditorConfigValue(editorConfigProperty: EditorConfigProperty<T>): T {
        val isAndroidCodeStyle = getUserData(KtLint.ANDROID_USER_DATA_KEY) ?: false
        return getUserData(KtLint.EDITOR_CONFIG_PROPERTIES_USER_DATA_KEY)!!
            .getEditorConfigValue(editorConfigProperty, isAndroidCodeStyle)
    }

    @Deprecated(
        message = "Prefer to use extension function 'ASTNode.getEditorConfigValue(editorConfigProperty)' as " +
            "parameter 'isAndroidCodeStyle' can be derived from the ASTNode.",
        replaceWith = ReplaceWith("this.getEditorConfigValue(editorConfigProperty)")
    )
    // TODO: Change access to private after deprecation period is expired.
    public fun <T> EditorConfigProperties.getEditorConfigValue(
        editorConfigProperty: EditorConfigProperty<T>,
        isAndroidCodeStyle: Boolean = false
    ): T {
        val property = get(editorConfigProperty.type.name)

        // If the property value is remapped to a non-null value then return it immediately.
        editorConfigProperty
            .propertyMapper
            ?.invoke(property, isAndroidCodeStyle)
            ?.let { newValue ->
                when {
                    property == null ->
                        logger.trace {
                            "No value of '.editorconfig' property '${editorConfigProperty.type.name}' was found. " +
                                "Value has been defaulted to '$newValue'. Setting the value explicitly in '.editorconfig' " +
                                "remove this message from the log."
                        }
                    newValue != property.getValueAs() ->
                        logger.trace {
                            "Value of '.editorconfig' property '${editorConfigProperty.type.name}' is overridden " +
                                "from '${property.sourceValue}' to '$newValue'"
                        }
                }
                return newValue
            }

        return property?.getValueAs()
            ?: editorConfigProperty
                .getDefaultValue(isAndroidCodeStyle)
                .also {
                    logger.trace {
                        "No value of '.editorconfig' property '${editorConfigProperty.type.name}' was found. Value " +
                            "has been defaulted to '$it'. Setting the value explicitly in '.editorconfig' " +
                            "remove this message from the log."
                    }
                }
    }

    private fun <T> EditorConfigProperty<T>.getDefaultValue(isAndroidCodeStyle: Boolean) =
        if (isAndroidCodeStyle) {
            defaultAndroidValue
        } else {
            defaultValue
        }

    /**
     * Write the string representation of [EditorConfigProperty]
     */
    public fun <T> EditorConfigProperties.writeEditorConfigProperty(
        editorConfigProperty: EditorConfigProperty<T>,
        isAndroidCodeStyle: Boolean
    ): String {
        return editorConfigProperty.propertyWriter(getEditorConfigValue(editorConfigProperty, isAndroidCodeStyle))
    }

    /**
     * Supported `.editorconfig` property.
     *
     * [Rule] preferably should expose it with `public` visibility in `companion object`,
     * so it will be possible to add/replace via [com.pinterest.ktlint.core.KtLint.Params].
     *
     * @param type type of property. Could be one of default ones (see [PropertyType.STANDARD_TYPES]) or custom one.
     * @param defaultValue default value for property if it does not exist in loaded properties.
     * @param defaultAndroidValue default value for android codestyle. You should set different value only when it
     * differs from [defaultValue].
     * @param propertyWriter custom function that represents [T] as String. Defaults to the standard `toString()` call.
     * You should override the default implementation in case you need a different behavior than the standard `toString()`
     * (e.g. for collections joinToString() is more applicable).
     */
    public data class EditorConfigProperty<T>(
        public val type: PropertyType<T>,
        public val defaultValue: T,
        public val defaultAndroidValue: T = defaultValue,
        /**
         * If set, it maps the actual value set for the property, to another valid value for that property. See example
         * below where
         * ```kotlin
         * propertyMapper = { property, isAndroidCodeStyle ->
         *     when {
         *         property == null ->
         *             // property is not defined in ".editorconfig" file
         *         property.isUnset ->
         *             // property is defined in ".editorconfig" file with special value "unset"
         *         property.sourceValue == "some-string-value" ->
         *             // property is defined in ".editorconfig" file with a value that needs to be remapped to another
         *             // valid value. For example the "max_line_length" property accepts value "off" but is remapped to
         *             // "-1" in ktlint.
         *        else ->
         *             property.getValueAs() // or null
         *     }
         * }
         * ```
         * In case the lambda returns a null value then, the [defaultValue] or [defaultAndroidValue] will be set as
         * value of the property. The
         */
        public val propertyMapper: ((Property?, Boolean) -> T?)? = null,
        public val propertyWriter: (T) -> String = { it.toString() }
    )
}

/**
 * Defines KtLint properties which are based on default property types provided by [org.ec4j.core.model.PropertyType].
 */
public object DefaultEditorConfigProperties {
    public val indentStyleProperty: UsesEditorConfigProperties.EditorConfigProperty<PropertyType.IndentStyleValue> =
        UsesEditorConfigProperties.EditorConfigProperty(
            type = PropertyType.indent_style,
            defaultValue = PropertyType.IndentStyleValue.space
        )

    public val indentSizeProperty: UsesEditorConfigProperties.EditorConfigProperty<Int> =
        UsesEditorConfigProperties.EditorConfigProperty(
            type = PropertyType.indent_size,
            defaultValue = IndentConfig.DEFAULT_INDENT_CONFIG.tabWidth,
            propertyMapper = { property, _ ->
                when {
                    property == null -> IndentConfig.DEFAULT_INDENT_CONFIG.tabWidth
                    property.isUnset -> -1
                    else -> property.getValueAs()
                }
            }
        )

    public val insertNewLineProperty: UsesEditorConfigProperties.EditorConfigProperty<Boolean> =
        UsesEditorConfigProperties.EditorConfigProperty(
            type = PropertyType.insert_final_newline,
            defaultValue = true
        )

    public val maxLineLengthProperty: UsesEditorConfigProperties.EditorConfigProperty<Int> =
        UsesEditorConfigProperties.EditorConfigProperty(
            type = PropertyType.max_line_length,
            defaultValue = -1,
            propertyMapper = { property, isAndroidCodeStyle ->
                when {
                    property == null || property.isUnset -> {
                        if (isAndroidCodeStyle) {
                            // https://developer.android.com/kotlin/style-guide#line_wrapping
                            100
                        } else {
                            property?.getValueAs()
                        }
                    }
                    property.sourceValue == "off" -> -1
                    else -> property.getValueAs()
                }
            }
        )

    public val defaultEditorConfigProperties: List<UsesEditorConfigProperties.EditorConfigProperty<out Any>> = listOf(
        indentStyleProperty,
        indentSizeProperty,
        insertNewLineProperty,
        maxLineLengthProperty
    )
}

/**
 * Loaded [Property]s from `.editorconfig` files.
 */
public typealias EditorConfigProperties = Map<String, Property>
