package com.pinterest.ktlint.core.api.editorconfig

import org.ec4j.core.model.PropertyType

private const val MAX_LINE_LENGTH_PROPERTY_ANDROID_STUDIO_CODE_STYLE = 100 // https://developer.android.com/kotlin/style-guide#line_wrapping
private const val MAX_LINE_LENGTH_PROPERTY_KTLINT_OFFICIAL_CODE_STYLE = 120
private const val MAX_LINE_LENGTH_PROPERTY_NONE = -1

public val MAX_LINE_LENGTH_PROPERTY: EditorConfigProperty<Int> =
    EditorConfigProperty(
        name = PropertyType.max_line_length.name,
        type = PropertyType.max_line_length,
        defaultValue = MAX_LINE_LENGTH_PROPERTY_NONE,
        androidStudioCodeStyleDefaultValue = MAX_LINE_LENGTH_PROPERTY_ANDROID_STUDIO_CODE_STYLE,
        intellijIdeaCodeStyleDefaultValue = MAX_LINE_LENGTH_PROPERTY_NONE,
        ktlintOfficialCodeStyleDefaultValue = MAX_LINE_LENGTH_PROPERTY_KTLINT_OFFICIAL_CODE_STYLE,
        propertyMapper = { property, codeStyleValue ->
            when {
                property == null || property.isUnset -> {
                    when (codeStyleValue) {
                        CodeStyleValue.android_studio -> {
                            MAX_LINE_LENGTH_PROPERTY_ANDROID_STUDIO_CODE_STYLE
                        }
                        CodeStyleValue.ktlint_official -> {
                            MAX_LINE_LENGTH_PROPERTY_KTLINT_OFFICIAL_CODE_STYLE
                        }
                        else -> {
                            property?.getValueAs()
                        }
                    }
                }

                property.sourceValue == "off" -> MAX_LINE_LENGTH_PROPERTY_NONE
                else -> PropertyType.max_line_length.parse(property.sourceValue).parsed
            }
        },
    )
