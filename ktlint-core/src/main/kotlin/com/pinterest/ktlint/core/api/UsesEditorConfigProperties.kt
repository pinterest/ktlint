package com.pinterest.ktlint.core.api

import org.ec4j.core.model.Property
import org.ec4j.core.model.PropertyType

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
@FeatureInAlphaState
public interface UsesEditorConfigProperties {

    /**
     * Provide a list of code style editorconfig properties, that rule uses in linting.
     */
    public val editorConfigProperties: List<EditorConfigProperty<*>>

    /**
     * Get the value of [EditorConfigProperty] based on loaded [EditorConfigProperties] content.
     */
    public fun <T> EditorConfigProperties.getEditorConfigValue(
        property: EditorConfigProperty<T>,
        isAndroidCodeStyle: Boolean = false
    ): T {
        return get(property.type.name)
            ?.getValueAs()
            ?: if (isAndroidCodeStyle) property.defaultAndroidValue else property.defaultValue
    }

    /**
     * Supported `.editorconfig` property.
     *
     * @param type type of property. Could be one of default ones (see [PropertyType.STANDARD_TYPES]) or custom one.
     * @param defaultValue default value for property if it does not exist in loaded properties.
     * @param defaultAndroidValue default value for android codestyle. You should set different value only when it
     * differs from [defaultValue].
     */
    public data class EditorConfigProperty<T>(
        public val type: PropertyType<T>,
        public val defaultValue: T,
        public val defaultAndroidValue: T = defaultValue
    )
}

/**
 * Loaded [Property]s from `.editorconfig` files.
 */
public typealias EditorConfigProperties = Map<String, Property>
