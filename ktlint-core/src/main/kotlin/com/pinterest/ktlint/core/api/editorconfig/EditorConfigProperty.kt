package com.pinterest.ktlint.core.api.editorconfig

import org.ec4j.core.model.Property
import org.ec4j.core.model.PropertyType

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
