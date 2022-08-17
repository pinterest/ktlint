package com.pinterest.ktlint.core.api

import com.pinterest.ktlint.core.IndentConfig
import com.pinterest.ktlint.core.KtLint
import com.pinterest.ktlint.core.api.DefaultEditorConfigProperties.CodeStyleValue
import com.pinterest.ktlint.core.api.DefaultEditorConfigProperties.CodeStyleValue.android
import com.pinterest.ktlint.core.api.DefaultEditorConfigProperties.CodeStyleValue.official
import com.pinterest.ktlint.core.api.DefaultEditorConfigProperties.codeStyleSetProperty
import com.pinterest.ktlint.core.initKtLintKLogger
import mu.KotlinLogging
import org.ec4j.core.model.Property
import org.ec4j.core.model.PropertyType
import org.ec4j.core.model.PropertyType.PropertyValueParser.EnumValueParser
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
     * Provide a list of editorconfig properties used by a class (most ofter a [com.pinterest.ktlint.core.Rule].
     * Retrieval of an editorconfig property is prohibited when the property has not been registered in [editorConfigProperties].
     * The [editorConfigProperties] is used to generate a complete set of ".editorconfig" properties.
     */
    public val editorConfigProperties: List<EditorConfigProperty<*>>

    /**
     * Get the value of [EditorConfigProperty] based on loaded [EditorConfigProperties] content for the current
     * [ASTNode].
     */
    public fun <T> EditorConfigProperties.getEditorConfigValue(editorConfigProperty: EditorConfigProperty<T>): T {
        require(editorConfigProperties.contains(editorConfigProperty)) {
            "EditorConfigProperty '${editorConfigProperty.type.name}' may only be retrieved when it is registered in the editorConfigProperties."
        }

        return getEditorConfigValue(editorConfigProperty, getEditorConfigCodeStyle())
    }

    /**
     * The code style property does not need to be defined in the [editorConfigProperties] of the class that defines
     * this interface. Those classed should not need to be aware of the different coding styles except when setting
     * different default values. As the property is not defined in the [editorConfigProperties] the value needs to
     * be parsed explicitly to prevent class cast exceptions.
     */
    private fun EditorConfigProperties.getEditorConfigCodeStyle() =
        codeStyleSetProperty
            .type
            .parse(
                get(codeStyleSetProperty.type.name)?.sourceValue,
            ).parsed
            ?: official

    /**
     * Get the value of [EditorConfigProperty] based on loaded [EditorConfigProperties] content for the current
     * [ASTNode].
     */
    @Deprecated(message = "Marked for deletion in Ktlint 0.48. EditorConfigProperties are now supplied to Rule via call on method beforeFirstNode")
    public fun <T> ASTNode.getEditorConfigValue(editorConfigProperty: EditorConfigProperty<T>): T {
        require(editorConfigProperties.contains(editorConfigProperty)) {
            "EditorConfigProperty '${editorConfigProperty.type.name}' may only be retrieved when it is registered in the editorConfigProperties."
        }
        val editorConfigPropertyValues = getUserData(KtLint.EDITOR_CONFIG_PROPERTIES_USER_DATA_KEY)!!
        return editorConfigPropertyValues.getEditorConfigValue(
            editorConfigProperty,
            editorConfigPropertyValues.getEditorConfigCodeStyle(),
        )
    }

    private fun <T> EditorConfigProperties.getEditorConfigValue(
        editorConfigProperty: EditorConfigProperty<T>,
        codeStyleValue: CodeStyleValue,
    ): T {
        if (editorConfigProperty.deprecationWarning != null) {
            logger.warn { "Property '${editorConfigProperty.type.name}' is deprecated: ${editorConfigProperty.deprecationWarning}" }
        }

        val property = get(editorConfigProperty.type.name)

        if (property != null) {
            editorConfigProperty
                .propertyMapper
                ?.invoke(property, codeStyleValue)
                ?.let { newValue ->
                    // If the property value is remapped to a non-null value then return it immediately.
                    val originalValue = property.sourceValue
                    if (newValue.toString() != originalValue) {
                        logger.trace {
                            "Value of '.editorconfig' property '${editorConfigProperty.type.name}' is remapped " +
                                "from '$originalValue' to '$newValue'"
                        }
                    }
                    return newValue
                }
        }

        return property?.getValueAs()
            ?: editorConfigProperty
                .getDefaultValue(codeStyleValue)
                .also {
                    logger.trace {
                        "No value of '.editorconfig' property '${editorConfigProperty.type.name}' was found. Value " +
                            "has been defaulted to '$it'. Setting the value explicitly in '.editorconfig' " +
                            "removes this message from the log."
                    }
                }
    }

    private fun <T> EditorConfigProperty<T>.getDefaultValue(codeStyleValue: CodeStyleValue) =
        if (codeStyleValue == android) {
            defaultAndroidValue
        } else {
            defaultValue
        }

    /**
     * Write the string representation of [EditorConfigProperty]
     */
    public fun <T> EditorConfigProperties.writeEditorConfigProperty(
        editorConfigProperty: EditorConfigProperty<T>,
        codeStyleValue: CodeStyleValue,
    ): String {
        return editorConfigProperty.propertyWriter(getEditorConfigValue(editorConfigProperty, codeStyleValue))
    }

    /**
     * Supported `.editorconfig` property.
     *
     * [Rule] preferably should expose it with `public` visibility in `companion object`,
     * so it will be possible to add/replace via [com.pinterest.ktlint.core.KtLint.ExperimentalParams].
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
        public val propertyMapper: ((Property?, CodeStyleValue) -> T?)? = null,
        public val propertyWriter: (T) -> String = { it.toString() },

        /**
         * Optional message to be displayed whenever the value of the property is being retrieved.
         */
        internal val deprecationWarning: String? = null,
    )
}

/**
 * Defines KtLint properties which are based on default property types provided by [org.ec4j.core.model.PropertyType].
 */
public object DefaultEditorConfigProperties : UsesEditorConfigProperties {
    /**
     * Code style to be used while linting and formatting. Note that the [EnumValueParser] requires values to be lowercase.
     */
    @Suppress("EnumEntryName", "ktlint:enum-entry-name-case")
    public enum class CodeStyleValue {
        android,
        official,
    }

    public val codeStyleSetProperty: UsesEditorConfigProperties.EditorConfigProperty<CodeStyleValue> =
        UsesEditorConfigProperties.EditorConfigProperty(
            type = PropertyType.LowerCasingPropertyType(
                "ktlint_code_style",
                "The code style ('official' or 'android') to be applied. Defaults to 'official' when not set.",
                EnumValueParser(CodeStyleValue::class.java),
                CodeStyleValue.values().map { it.name }.toSet(),
            ),
            defaultValue = official,
            defaultAndroidValue = android,
        )

    @Deprecated(
        message = "Marked for removal in KtLint 0.48",
        replaceWith = ReplaceWith("ktlintDisabledRulesProperty"),
    )
    public val disabledRulesProperty: UsesEditorConfigProperties.EditorConfigProperty<String> =
        UsesEditorConfigProperties.EditorConfigProperty(
            type = PropertyType.LowerCasingPropertyType(
                "disabled_rules",
                "A comma separated list of rule ids which should not be run. For rules not defined in the 'standard' ruleset, the qualified rule-id should be used.",
                PropertyType.PropertyValueParser.IDENTITY_VALUE_PARSER,
                emptySet(),
            ),
            defaultValue = "",
            propertyMapper = { property, _ ->
                when {
                    property?.isUnset == true -> ""
                    property?.getValueAs<String>() != null -> {
                        // Remove spaces (most likely they occur only around the comma) as they otherwise will be seen
                        // as part of the rule-id which is to be disabled. But as the space is not allowed in the id's
                        // of rule sets and rule ids, they are just removed all.
                        property.getValueAs<String>().replace(" ", "")
                    }
                    else -> property?.getValueAs()
                }
            },
            deprecationWarning = "Rename property 'disabled_rules' to 'ktlint_disabled_rules' in all '.editorconfig' files.",
        )

    public val ktlintDisabledRulesProperty: UsesEditorConfigProperties.EditorConfigProperty<String> =
        UsesEditorConfigProperties.EditorConfigProperty(
            type = PropertyType.LowerCasingPropertyType(
                "ktlint_disabled_rules",
                "A comma separated list of rule ids which should not be run. For rules not defined in the 'standard' ruleset, the qualified rule-id should be used.",
                PropertyType.PropertyValueParser.IDENTITY_VALUE_PARSER,
                emptySet(),
            ),
            defaultValue = "",
            propertyMapper = { property, _ ->
                when {
                    property?.isUnset == true -> ""
                    property?.getValueAs<String>() != null -> {
                        // Remove spaces (most likely they occur only around the comma) as they otherwise will be seen
                        // as part of the rule-id which is to be disabled. But as the space is not allowed in the id's
                        // of rule sets and rule ids, they are just removed all.
                        property.getValueAs<String>().replace(" ", "")
                    }
                    else -> property?.getValueAs()
                }
            },
        )

    public val indentStyleProperty: UsesEditorConfigProperties.EditorConfigProperty<PropertyType.IndentStyleValue> =
        UsesEditorConfigProperties.EditorConfigProperty(
            type = PropertyType.indent_style,
            defaultValue = PropertyType.IndentStyleValue.space,
        )

    public val indentSizeProperty: UsesEditorConfigProperties.EditorConfigProperty<Int> =
        UsesEditorConfigProperties.EditorConfigProperty(
            type = PropertyType.indent_size,
            defaultValue = IndentConfig.DEFAULT_INDENT_CONFIG.tabWidth,
            propertyMapper = { property, _ ->
                when {
                    property?.isUnset == true -> -1
                    property?.getValueAs<Int>() == null ->
                        IndentConfig.DEFAULT_INDENT_CONFIG.tabWidth
                    else -> property.getValueAs()
                }
            },
        )

    public val insertNewLineProperty: UsesEditorConfigProperties.EditorConfigProperty<Boolean> =
        UsesEditorConfigProperties.EditorConfigProperty(
            type = PropertyType.insert_final_newline,
            defaultValue = true,
        )

    public val maxLineLengthProperty: UsesEditorConfigProperties.EditorConfigProperty<Int> =
        UsesEditorConfigProperties.EditorConfigProperty(
            type = PropertyType.max_line_length,
            defaultValue = -1,
            defaultAndroidValue = 100,
            propertyMapper = { property, codeStyleValue ->
                when {
                    property == null || property.isUnset -> {
                        if (codeStyleValue == android) {
                            // https://developer.android.com/kotlin/style-guide#line_wrapping
                            100
                        } else {
                            property?.getValueAs()
                        }
                    }
                    property.sourceValue == "off" -> -1
                    else -> PropertyType.max_line_length.parse(property.sourceValue).parsed
                }
            },
        )

    override val editorConfigProperties: List<UsesEditorConfigProperties.EditorConfigProperty<*>> = listOf(
        codeStyleSetProperty,
        disabledRulesProperty,
        indentStyleProperty,
        indentSizeProperty,
        insertNewLineProperty,
        maxLineLengthProperty,
    )
}

/**
 * Loaded [Property]s from `.editorconfig` files.
 */
public typealias EditorConfigProperties = Map<String, Property>
