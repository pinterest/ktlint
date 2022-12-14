package com.pinterest.ktlint.core.api.editorconfig

import java.util.Locale
import org.ec4j.core.model.PropertyType
import org.ec4j.core.model.PropertyType.PropertyValueParser

/**
 * A [PropertyValueParser] implementation that allows only members of a given [Enum] type. This class is almost
 * identical to the original [EnumValueParser] provided by ec4j. Difference is that values are trimmed before trying to
 * match the enum values.
 *
 * As the ec4j project has not provided any new release since version 1.0 (2019-08-01) a custom implementation has been
 * added.
 *
 * @param <T> the type of the value <T>
 *
 */
internal class SafeEnumValueParser<T : Enum<*>?>(enumType: Class<out T?>) : PropertyValueParser<T> {
    private val enumType: Class<out Enum<*>?>

    init {
        this.enumType = enumType
    }

    override fun parse(name: String?, value: String?): PropertyType.PropertyValue<T> =
        if (value == null) {
            PropertyType.PropertyValue.invalid(value, "Cannot make enum " + enumType.name + " out of null")
        } else {
            try {
                PropertyType.PropertyValue.valid(
                    value,
                    java.lang.Enum.valueOf(
                        enumType,
                        value
                            // In case an EOL comment (separated with a space) is appended after the value then the comment
                            // itself is removed but not the space. This results in the enum value not being parsed
                            // correctly.
                            .trim()
                            .lowercase(Locale.getDefault()),
                    ) as T,
                )
            } catch (e: IllegalArgumentException) {
                PropertyType.PropertyValue.invalid(
                    value,
                    "Unexpected parsed \"" + value + "\" for enum " + enumType.name,
                )
            }
        }
}
