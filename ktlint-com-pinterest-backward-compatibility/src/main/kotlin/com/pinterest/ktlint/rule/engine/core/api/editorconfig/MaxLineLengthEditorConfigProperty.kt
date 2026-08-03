@file:Suppress("DEPRECATION")

package com.pinterest.ktlint.rule.engine.core.api.editorconfig

import io.github.ktlint.core.logger.api.initKtLintKLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import org.ec4j.core.model.PropertyType

private const val MAX_LINE_LENGTH_PROPERTY_ANDROID_STUDIO_CODE_STYLE = 100
private const val MAX_LINE_LENGTH_PROPERTY_KTLINT_OFFICIAL_CODE_STYLE = 140
private const val MAX_LINE_LENGTH_PROPERTY_OFF_EDITOR_CONFIG = "off"

@Deprecated(message = "Provides backwards compatibility of custom ruleset JARs created for Ktlint 1.x. Don't use for RuleV2")
public const val MAX_LINE_LENGTH_PROPERTY_OFF: Int = Int.MAX_VALUE

private val LOGGER = KotlinLogging.logger {}.initKtLintKLogger()
private var isInvalidValueLoggedBefore = false

@Deprecated(message = "Provides backwards compatibility of custom ruleset JARs created for Ktlint 1.x. Don't use for RuleV2")
public val MAX_LINE_LENGTH_PROPERTY: EditorConfigProperty<Int> =
    EditorConfigProperty(
        name = PropertyType.max_line_length.name,
        type = PropertyType.max_line_length,
        defaultValue = MAX_LINE_LENGTH_PROPERTY_OFF,
        androidStudioCodeStyleDefaultValue = MAX_LINE_LENGTH_PROPERTY_ANDROID_STUDIO_CODE_STYLE,
        intellijIdeaCodeStyleDefaultValue = MAX_LINE_LENGTH_PROPERTY_OFF,
        ktlintOfficialCodeStyleDefaultValue = MAX_LINE_LENGTH_PROPERTY_KTLINT_OFFICIAL_CODE_STYLE,
        propertyMapper = { property, codeStyleValue ->
            when {
                property == null || property.isUnset -> {
                    codeStyleValue.defaultValue()
                }

                property.sourceValue == MAX_LINE_LENGTH_PROPERTY_OFF_EDITOR_CONFIG -> {
                    MAX_LINE_LENGTH_PROPERTY_OFF
                }

                else -> {
                    PropertyType
                        .max_line_length
                        .parse(property.sourceValue)
                        .let {
                            if (!it.isValid) {
                                if (!isInvalidValueLoggedBefore) {
                                    isInvalidValueLoggedBefore = true
                                    LOGGER.warn { "Found invalid '.editorconfig' property value: ${it.errorMessage}" }
                                }
                                if (it.source == "-1") {
                                    MAX_LINE_LENGTH_PROPERTY_OFF
                                } else {
                                    codeStyleValue.defaultValue()
                                }
                            } else {
                                it.parsed
                            }
                        }
                }
            }
        },
        propertyWriter = { property ->
            if (property <= 0 || property == MAX_LINE_LENGTH_PROPERTY_OFF) {
                MAX_LINE_LENGTH_PROPERTY_OFF_EDITOR_CONFIG
            } else {
                property.toString()
            }
        },
    )

private fun CodeStyleValue.defaultValue() =
    when (this) {
        CodeStyleValue.android_studio -> MAX_LINE_LENGTH_PROPERTY_ANDROID_STUDIO_CODE_STYLE
        CodeStyleValue.intellij_idea -> MAX_LINE_LENGTH_PROPERTY_OFF
        CodeStyleValue.ktlint_official -> MAX_LINE_LENGTH_PROPERTY_KTLINT_OFFICIAL_CODE_STYLE
    }
