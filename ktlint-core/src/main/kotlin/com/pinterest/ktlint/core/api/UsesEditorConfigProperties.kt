package com.pinterest.ktlint.core.api

import com.pinterest.ktlint.core.api.editorconfig.CODE_STYLE_PROPERTY
import com.pinterest.ktlint.core.api.editorconfig.CodeStyleValue
import com.pinterest.ktlint.core.api.editorconfig.DEFAULT_EDITOR_CONFIG_PROPERTIES
import com.pinterest.ktlint.core.api.editorconfig.DISABLED_RULES_PROPERTY
import com.pinterest.ktlint.core.api.editorconfig.EditorConfigProperty
import com.pinterest.ktlint.core.api.editorconfig.INDENT_SIZE_PROPERTY
import com.pinterest.ktlint.core.api.editorconfig.INDENT_STYLE_PROPERTY
import com.pinterest.ktlint.core.api.editorconfig.INSERT_FINAL_NEWLINE_PROPERTY
import com.pinterest.ktlint.core.api.editorconfig.KTLINT_DISABLED_RULES_PROPERTY
import com.pinterest.ktlint.core.api.editorconfig.MAX_LINE_LENGTH_PROPERTY
import com.pinterest.ktlint.core.initKtLintKLogger
import mu.KotlinLogging
import org.ec4j.core.model.PropertyType

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
            ?: CodeStyleValue.official

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
        if (codeStyleValue == CodeStyleValue.android) {
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
}

public class DeprecatedEditorConfigPropertyException(message: String) : RuntimeException(message)

/**
 * Defines KtLint properties which are based on default property types provided by [org.ec4j.core.model.PropertyType].
 */
@Deprecated(
    "Marked for removal of public API in KtLint 0.49. Please raise an issue if you have a use case why this " +
        "should be kept public.",
)
public object DefaultEditorConfigProperties : UsesEditorConfigProperties {
    @Deprecated(
        message = "Marked for removal in KtLint 0.49",
        replaceWith = ReplaceWith(
            expression = "CODE_STYLE_PROPERTY",
            imports = ["com.pinterest.ktlint.core.api.editorconfig.CODE_STYLE_PROPERTY"],
        ),
    )
    @Suppress("ktlint:experimental:property-naming")
    public val codeStyleSetProperty: EditorConfigProperty<CodeStyleValue> =
        CODE_STYLE_PROPERTY

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
        "Marked for removal in KtLint 0.49. Update all your all '.editorconfig' files. See " +
            "https://pinterest.github.io/ktlint/faq/#why-is-editorconfig-property-disabled_rules-deprecated-and-how-do-i-resolve-this " +
            "for more information",
        // ReplaceWith is not specified as the property should not be migrated to KTLINT_DISABLED_RULES_PROPERTY but to
        // the RuleExecution property.
    )
    @Suppress("ktlint:experimental:property-naming")
    public val disabledRulesProperty: EditorConfigProperty<String> =
        DISABLED_RULES_PROPERTY

    @Deprecated(
        message = "Marked for removal in KtLint 0.49",
        replaceWith = ReplaceWith(
            expression = "KTLINT_DISABLED_RULES_PROPERTY",
            imports = ["com.pinterest.ktlint.core.api.editorconfig.INDENT_STYLE_PROPERTY"],
        ),
    )
    @Suppress("ktlint:experimental:property-naming")
    public val ktlintDisabledRulesProperty: EditorConfigProperty<String> =
        KTLINT_DISABLED_RULES_PROPERTY

    @Deprecated(
        message = "Marked for removal in KtLint 0.49",
        replaceWith = ReplaceWith(
            expression = "INDENT_STYLE_PROPERTY",
            imports = ["com.pinterest.ktlint.core.api.editorconfig.INDENT_STYLE_PROPERTY"],
        ),
    )
    @Suppress("ktlint:experimental:property-naming")
    public val indentStyleProperty: EditorConfigProperty<PropertyType.IndentStyleValue> =
        INDENT_STYLE_PROPERTY

    @Deprecated(
        message = "Marked for removal in KtLint 0.49",
        replaceWith = ReplaceWith(
            expression = "INDENT_SIZE_PROPERTY",
            imports = ["com.pinterest.ktlint.core.api.editorconfig.INDENT_SIZE_PROPERTY"],
        ),
    )
    @Suppress("ktlint:experimental:property-naming")
    public val indentSizeProperty: EditorConfigProperty<Int> =
        INDENT_SIZE_PROPERTY

    @Deprecated(
        message = "Marked for removal in KtLint 0.49",
        replaceWith = ReplaceWith(
            expression = "INSERT_FINAL_NEWLINE_PROPERTY",
            imports = ["com.pinterest.ktlint.core.api.editorconfig.INSERT_FINAL_NEWLINE_PROPERTY"],
        ),
    )
    @Suppress("ktlint:experimental:property-naming")
    public val insertNewLineProperty: EditorConfigProperty<Boolean> =
        INSERT_FINAL_NEWLINE_PROPERTY

    @Deprecated(
        message = "Marked for removal in KtLint 0.49",
        replaceWith = ReplaceWith(
            expression = "MAX_LINE_LENGTH_PROPERTY",
            imports = ["com.pinterest.ktlint.core.api.editorconfig.MAX_LINE_LENGTH_PROPERTY"],
        ),
    )
    @Suppress("ktlint:experimental:property-naming")
    public val maxLineLengthProperty: EditorConfigProperty<Int> =
        MAX_LINE_LENGTH_PROPERTY

    @Deprecated(
        "Marked for removal of public API in KtLint 0.49. Please raise an issue if you have a use case why this " +
            "should be kept public.",
    )
    override val editorConfigProperties: List<EditorConfigProperty<*>> =
        DEFAULT_EDITOR_CONFIG_PROPERTIES
}
