package com.pinterest.ktlint.core.api.editorconfig

import org.ec4j.core.model.PropertyType

public val MAX_LINE_LENGTH_PROPERTY: EditorConfigProperty<Int> =
    EditorConfigProperty(
        name = PropertyType.max_line_length.name,
        type = PropertyType.max_line_length,
        defaultValue = -1,
        defaultAndroidValue = 100,
        propertyMapper = { property, codeStyleValue ->
            when {
                property == null || property.isUnset -> {
                    if (codeStyleValue == CodeStyleValue.android) {
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
