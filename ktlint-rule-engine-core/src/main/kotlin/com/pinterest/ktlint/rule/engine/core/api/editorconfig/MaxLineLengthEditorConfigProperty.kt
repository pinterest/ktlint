package com.pinterest.ktlint.rule.engine.core.api.editorconfig

import com.pinterest.ktlint.logger.api.initKtLintKLogger
import mu.KotlinLogging
import org.ec4j.core.model.PropertyType

private const val MAX_LINE_LENGTH_PROPERTY_ANDROID_STUDIO_CODE_STYLE = 100 // https://developer.android.com/kotlin/style-guide#line_wrapping
private const val MAX_LINE_LENGTH_PROPERTY_KTLINT_OFFICIAL_CODE_STYLE = 140
private const val MAX_LINE_LENGTH_PROPERTY_OFF = "off"
private const val MAX_LINE_LENGTH_PROPERTY_OFF_INTERNALLY = -1

private val LOGGER = KotlinLogging.logger {}.initKtLintKLogger()
private var isInvalidValueLoggedBefore = false

public val MAX_LINE_LENGTH_PROPERTY: EditorConfigProperty<Int> =
    EditorConfigProperty(
        name = PropertyType.max_line_length.name,
        type = PropertyType.max_line_length,
        defaultValue = MAX_LINE_LENGTH_PROPERTY_OFF_INTERNALLY,
        androidStudioCodeStyleDefaultValue = MAX_LINE_LENGTH_PROPERTY_ANDROID_STUDIO_CODE_STYLE,
        intellijIdeaCodeStyleDefaultValue = MAX_LINE_LENGTH_PROPERTY_OFF_INTERNALLY,
        ktlintOfficialCodeStyleDefaultValue = MAX_LINE_LENGTH_PROPERTY_KTLINT_OFFICIAL_CODE_STYLE,
        propertyMapper = { property, codeStyleValue ->
            when {
                property == null || property.isUnset -> {
                    when (codeStyleValue) {
                        CodeStyleValue.android,
                        CodeStyleValue.android_studio,
                        -> {
                            MAX_LINE_LENGTH_PROPERTY_ANDROID_STUDIO_CODE_STYLE
                        }
                        CodeStyleValue.ktlint_official -> {
                            MAX_LINE_LENGTH_PROPERTY_KTLINT_OFFICIAL_CODE_STYLE
                        }
                        else -> {
                            MAX_LINE_LENGTH_PROPERTY_OFF_INTERNALLY
                        }
                    }
                }

                /**
                 * Internally, Ktlint uses value '-1' to indicate that the max line length has to be ignored as this is easier in
                 * comparisons to check whether the maximum length of a line is exceeded.
                 */
                property.sourceValue == MAX_LINE_LENGTH_PROPERTY_OFF -> MAX_LINE_LENGTH_PROPERTY_OFF_INTERNALLY

                else ->
                    PropertyType
                        .max_line_length
                        .parse(property.sourceValue)
                        .let {
                            if (!it.isValid) {
                                if (!isInvalidValueLoggedBefore) {
                                    isInvalidValueLoggedBefore = true
                                    LOGGER.warn { "Found invalid '.editorconfig' property value: ${it.errorMessage}" }
                                }
                                MAX_LINE_LENGTH_PROPERTY_OFF_INTERNALLY
                            } else {
                                it.parsed
                            }
                        }
            }
        },
        propertyWriter = { property ->
            if (property <= 0) {
                MAX_LINE_LENGTH_PROPERTY_OFF
            } else {
                property.toString()
            }
        },
    )
