package com.pinterest.ktlint.ruleset.core.api.editorconfig

import com.pinterest.ktlint.logger.api.initKtLintKLogger
import com.pinterest.ktlint.ruleset.core.api.EditorConfigProperties
import com.pinterest.ktlint.ruleset.core.api.editorconfig.CodeStyleValue.android_studio
import com.pinterest.ktlint.ruleset.core.api.editorconfig.CodeStyleValue.intellij_idea
import com.pinterest.ktlint.ruleset.core.api.editorconfig.CodeStyleValue.ktlint_official
import mu.KotlinLogging

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
            ?: CODE_STYLE_PROPERTY.defaultValue

    /**
     * Get the value of [editorConfigProperty] from [EditorConfigProperties].
     */
    public fun <T> EditorConfigProperties.getEditorConfigValue(editorConfigProperty: EditorConfigProperty<T>): T {
        require(editorConfigProperties.contains(editorConfigProperty)) {
            "EditorConfigProperty '${editorConfigProperty.name}' may only be retrieved when it is registered in the editorConfigProperties."
        }
        when {
            editorConfigProperty.deprecationError != null ->
                throw DeprecatedEditorConfigPropertyException(
                    "Property '${editorConfigProperty.name}' is disallowed: ${editorConfigProperty.deprecationError}",
                )
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
        when (codeStyleValue) {
            android_studio -> androidStudioCodeStyleDefaultValue
            intellij_idea -> intellijIdeaCodeStyleDefaultValue
            ktlint_official -> ktlintOfficialCodeStyleDefaultValue
            else -> {
                defaultValue
            }
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
