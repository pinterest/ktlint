package com.pinterest.ktlint.core.api

import com.pinterest.ktlint.core.IndentConfig
import com.pinterest.ktlint.core.api.DefaultEditorConfigProperties.CODE_STYLE_PROPERTY
import com.pinterest.ktlint.core.api.DefaultEditorConfigProperties.CodeStyleValue
import com.pinterest.ktlint.core.api.DefaultEditorConfigProperties.CodeStyleValue.android
import com.pinterest.ktlint.core.api.DefaultEditorConfigProperties.CodeStyleValue.official
import com.pinterest.ktlint.core.api.UsesEditorConfigProperties.EditorConfigProperty
import com.pinterest.ktlint.core.initKtLintKLogger
import com.pinterest.ktlint.core.internal.ruleId
import com.pinterest.ktlint.core.internal.ruleSetId
import mu.KotlinLogging
import org.ec4j.core.model.Property
import org.ec4j.core.model.PropertyType
import org.ec4j.core.model.PropertyType.PropertyValueParser.EnumValueParser

private val LOGGER = KotlinLogging.logger {}.initKtLintKLogger()

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
     * Provide a list of editorconfig properties used by a class (most often a [com.pinterest.ktlint.core.Rule]).
     * Retrieval of an editorconfig property is prohibited when the property has not been registered in
     * [editorConfigProperties]. The [editorConfigProperties] is used to generate a complete set of ".editorconfig"
     * properties.
     */
    public val editorConfigProperties: List<EditorConfigProperty<*>>

    /**
     * The code style property does not need to be defined in the [editorConfigProperties] of the class that defines
     * this interface. Those classed should not need to be aware of the different coding styles except when setting
     * different default values. As the property is not defined in the [editorConfigProperties] the value needs to
     * be parsed explicitly to prevent class cast exceptions.
     */
    private fun EditorConfigProperties.getEditorConfigCodeStyle() =
        CODE_STYLE_PROPERTY
            .type
            .parse(
                get(CODE_STYLE_PROPERTY.name)?.sourceValue,
            ).parsed
            ?: official

    /**
     * Get the value of [editorConfigProperty] from [EditorConfigProperties].
     */
    public fun <T> EditorConfigProperties.getEditorConfigValue(editorConfigProperty: EditorConfigProperty<T>): T {
        require(editorConfigProperties.contains(editorConfigProperty)) {
            "EditorConfigProperty '${editorConfigProperty.name}' may only be retrieved when it is registered in the editorConfigProperties."
        }
        when {
            editorConfigProperty.deprecationError != null ->
                throw DeprecatedEditorConfigPropertyException("Property '${editorConfigProperty.name}' is disallowed: ${editorConfigProperty.deprecationError}")
            editorConfigProperty.deprecationWarning != null ->
                LOGGER.warn { "Property '${editorConfigProperty.name}' is deprecated: ${editorConfigProperty.deprecationWarning}" }
        }

        val property = get(editorConfigProperty.name)
        val codeStyleValue = getEditorConfigCodeStyle()

        if (property != null) {
            editorConfigProperty
                .propertyMapper
                ?.invoke(property, codeStyleValue)
                ?.let { newValue ->
                    // If the property value is remapped to a non-null value then return it immediately.
                    val originalValue = property.sourceValue
                    if (newValue.toString() != originalValue) {
                        LOGGER.trace {
                            "Value of '.editorconfig' property '${editorConfigProperty.name}' is remapped " +
                                "from '$originalValue' to '$newValue'"
                        }
                    }
                    return newValue
                }
        }

        val propertyValue =
            when {
                property == null -> null
                property.type != null -> property.getValueAs()
                else -> {
                    // In case the property was loaded from the default ".editorconfig" the type field is not known as
                    // the property could not yet be linked to a property type that is defined in a rule. To prevent
                    // class cast exceptions, lookup the property by name and convert to property type.
                    @Suppress("UNCHECKED_CAST")
                    this@UsesEditorConfigProperties
                        .editorConfigProperties
                        .find { it.name == property.name }
                        ?.type
                        ?.parse(property.sourceValue)
                        ?.parsed as T?
                }
            }

        return propertyValue
            ?: editorConfigProperty
                .getDefaultValue(codeStyleValue)
                .also {
                    LOGGER.trace {
                        "No value of '.editorconfig' property '${editorConfigProperty.name}' was found. Value " +
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
    ): String =
        editorConfigProperty.propertyWriter(
            getEditorConfigValue(editorConfigProperty),
        )

    /**
     * Definition of '.editorconfig' property enriched with KtLint specific fields.
     */
    public data class EditorConfigProperty<T>(
        /**
         * Type of property. Could be one of default ones (see [PropertyType.STANDARD_TYPES]) or custom one.
         */
        public val type: PropertyType<T>,

        /**
         * Default value for property if it does not exist in loaded properties and codestyle 'official'.
         */
        public val defaultValue: T,

        /**
         * Default value for property if it does not exist in loaded properties and codestyle 'android'. This property
         * is to be set only when its value does not equal [defaultValue].
         */
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

        /**
         * Custom function that represents [T] as String. Defaults to the standard `toString()` call. Override the
         * default implementation in case you need a different behavior than the standard `toString()` (e.g. for
         * collections joinToString() is more applicable).
         */
        public val propertyWriter: (T) -> String = { it.toString() },

        /**
         * Optional message to be displayed whenever the value of the property is being retrieved.
         */
        internal val deprecationWarning: String? = null,

        /**
         * Optional message to be displayed whenever the value of the property is being retrieved.
         */
        internal val deprecationError: String? = null,

        /**
         * Name of the property. A property must be named in case multiple properties are defined for the same type.
         * Defaults to the name of the type when not set.
         */
        public val name: String = type.name,
    )
}

public class DeprecatedEditorConfigPropertyException(message: String) : RuntimeException(message)

/**
 * Defines KtLint properties which are based on default property types provided by [org.ec4j.core.model.PropertyType].
 */
public object DefaultEditorConfigProperties : UsesEditorConfigProperties {
    /**
     * Code style to be used while linting and formatting. Note that the [EnumValueParser] requires values to be lowercase.
     */
    @Suppress("EnumEntryName")
    public enum class CodeStyleValue {
        android,
        official,
    }

    public val CODE_STYLE_PROPERTY: EditorConfigProperty<CodeStyleValue> =
        EditorConfigProperty(
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
        message = "Marked for removal in KtLint 0.49",
        replaceWith = ReplaceWith("CODE_STYLE_PROPERTY"),
    )
    @Suppress("ktlint:experimental:property-naming")
    public val codeStyleSetProperty: EditorConfigProperty<CodeStyleValue> =
        CODE_STYLE_PROPERTY

    @Deprecated(
        // Keep postponing the deprecation period until around 0.50. Some projects irregular update to newer KtLint
        // version and skipping intermediate version. As of such they might have missed the deprecation warning in
        // KtLint 0.47.
        "Marked for removal in KtLint 0.49. Update all your all '.editorconfig' files. See " +
            "https://pinterest.github.io/ktlint/faq/#why-is-editorconfig-property-disabled_rules-deprecated-and-how-do-i-resolve-this " +
            "for more information",
    )
    public val DISABLED_RULES_PROPERTY: EditorConfigProperty<String> =
        EditorConfigProperty(
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
            // TODO: Mark this property with a deprecationError starting from KtLint 0.49
            deprecationWarning = "Update all your all '.editorconfig' files. See " +
                "https://pinterest.github.io/ktlint/faq/#why-is-editorconfig-property-disabled_rules-deprecated-and-how-do-i-resolve-this " +
                "for more information",
        )

    /**
     * Marked for removal in KtLint 0.49.
     *
     * The current property consist of a comma separated list of rules that have to be disabled. This has the following
     * disadvantages:
     * - If the property is overridden in an '.editorconfig' in a subpackage then it is not possible to change the
     *   for specific ruleId's. All ruleIds which were set in an '.editorconfig' file at a higher package level need to
     *   be set again.
     * - The property is more difficult to be commented when multiple rules are disabled.
     *
     * This property is replaced with a separate property per rule (set). In this way a single line per disabled
     * property is required in the '.editorconfig'. It allows to enable/disable a specific rule (set) on package level
     * by setting the property of that rule (set) in an '.editorconfig' in that package.
     */
    @Deprecated(
        // Keep postponing the deprecation period until around 0.50. Some projects irregular update to newer KtLint
        // version and skipping intermediate version.
        "Marked for removal in KtLint 0.49. Update all your all '.editorconfig' files. See " +
            "https://pinterest.github.io/ktlint/faq/#why-is-editorconfig-property-disabled_rules-deprecated-and-how-do-i-resolve-this " +
            "for more information",
    )
    public val KTLINT_DISABLED_RULES_PROPERTY: EditorConfigProperty<String> =
        EditorConfigProperty(
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
            // TODO: Mark this property with a deprecationError starting from KtLint 0.49
            deprecationWarning = "Update all your all '.editorconfig' files. See " +
                "https://pinterest.github.io/ktlint/faq/#why-is-editorconfig-property-disabled_rules-deprecated-and-how-do-i-resolve-this " +
                "for more information",
        )

    @Suppress("EnumEntryName")
    public enum class RuleExecution {
        enabled,
        disabled,
    }

    internal val RULE_EXECUTION_PROPERTY_TYPE =
        PropertyType.LowerCasingPropertyType(
            "ktlint_rule_execution",
            "When enabled, the rule is being executed.",
            EnumValueParser(RuleExecution::class.java),
            CodeStyleValue.values().map { it.name }.toSet(),
        )

    /**
     * Generates the rule execution '.editorconfig' property for the rule set of the given [qualifiedRuleId].
     */
    public fun createRuleSetExecutionEditorConfigProperty(qualifiedRuleId: String): EditorConfigProperty<RuleExecution> =
        EditorConfigProperty(
            // Explicitly name the rule as multiple properties exists for the same type
            name = ktLintRuleSetExecutionPropertyName(qualifiedRuleId),
            type = RULE_EXECUTION_PROPERTY_TYPE,
            defaultValue = RuleExecution.enabled,
        )

    /**
     * Generates the rule execution '.editorconfig' property for the given [qualifiedRuleId]. Note that the property is
     * the same as the [qualifiedRuleId] but prefixed with 'ktlint_' followed by the ruleSetId and ruleId which both are
     * based on the [qualifiedRuleId].
     */
    public fun createRuleExecutionEditorConfigProperty(qualifiedRuleId: String): EditorConfigProperty<RuleExecution> =
        EditorConfigProperty(
            name = ktLintRuleExecutionPropertyName(qualifiedRuleId),
            type = RULE_EXECUTION_PROPERTY_TYPE,
            defaultValue = RuleExecution.enabled,
        )

    /**
     * Constructs the name of the '.editorconfig' property that determines whether the given rule is to be executed.
     */
    internal fun ktLintRuleExecutionPropertyName(qualifiedRuleId: String): String =
        "ktlint_${ruleSetId(qualifiedRuleId)}_${ruleId(qualifiedRuleId)}"

    /**
     * Constructs the name of the '.editorconfig' property that determines whether the rule set of the given
     * [qualifiedRuleId] is to be executed.
     */
    internal fun ktLintRuleSetExecutionPropertyName(qualifiedRuleId: String): String =
        "ktlint_${ruleSetId(qualifiedRuleId)}"

    @Deprecated(
        message = "Marked for removal in KtLint 0.49",
        replaceWith = ReplaceWith("KTLINT_DISABLED_RULES_PROPERTY"),
    )
    @Suppress("ktlint:experimental:property-naming")
    public val ktlintDisabledRulesProperty: EditorConfigProperty<String> =
        KTLINT_DISABLED_RULES_PROPERTY

    public val INDENT_STYLE_PROPERTY: EditorConfigProperty<PropertyType.IndentStyleValue> =
        EditorConfigProperty(
            name = PropertyType.indent_style.name,
            type = PropertyType.indent_style,
            defaultValue = PropertyType.IndentStyleValue.space,
        )

    @Deprecated(
        message = "Marked for removal in KtLint 0.49",
        replaceWith = ReplaceWith("INDENT_STYLE_PROPERTY"),
    )
    @Suppress("ktlint:experimental:property-naming")
    public val indentStyleProperty: EditorConfigProperty<PropertyType.IndentStyleValue> =
        INDENT_STYLE_PROPERTY

    public val INDENT_SIZE_PROPERTY: EditorConfigProperty<Int> =
        EditorConfigProperty(
            name = PropertyType.indent_size.name,
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

    @Deprecated(
        message = "Marked for removal in KtLint 0.49",
        replaceWith = ReplaceWith("INDENT_SIZE_PROPERTY"),
    )
    @Suppress("ktlint:experimental:property-naming")
    public val indentSizeProperty: EditorConfigProperty<Int> =
        INDENT_SIZE_PROPERTY

    public val INSERT_FINAL_NEWLINE_PROPERTY: EditorConfigProperty<Boolean> =
        EditorConfigProperty(
            name = PropertyType.insert_final_newline.name,
            type = PropertyType.insert_final_newline,
            defaultValue = true,
        )

    @Deprecated(
        message = "Marked for removal in KtLint 0.49",
        replaceWith = ReplaceWith("INSERT_FINAL_NEWLINE_PROPERTY"),
    )
    @Suppress("ktlint:experimental:property-naming")
    public val insertNewLineProperty: EditorConfigProperty<Boolean> =
        INSERT_FINAL_NEWLINE_PROPERTY

    public val MAX_LINE_LENGTH_PROPERTY: EditorConfigProperty<Int> =
        EditorConfigProperty(
            name = PropertyType.max_line_length.name,
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

    @Deprecated(
        message = "Marked for removal in KtLint 0.49",
        replaceWith = ReplaceWith("MAX_LINE_LENGTH_PROPERTY"),
    )
    @Suppress("ktlint:experimental:property-naming")
    public val maxLineLengthProperty: EditorConfigProperty<Int> =
        MAX_LINE_LENGTH_PROPERTY

    override val editorConfigProperties: List<EditorConfigProperty<*>> = listOf(
        CODE_STYLE_PROPERTY,
        DISABLED_RULES_PROPERTY,
        KTLINT_DISABLED_RULES_PROPERTY,
        INDENT_STYLE_PROPERTY,
        INDENT_SIZE_PROPERTY,
        INSERT_FINAL_NEWLINE_PROPERTY,
        MAX_LINE_LENGTH_PROPERTY,
    )
}
