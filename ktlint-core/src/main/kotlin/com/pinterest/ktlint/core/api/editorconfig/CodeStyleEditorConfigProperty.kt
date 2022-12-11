package com.pinterest.ktlint.core.api.editorconfig

import org.ec4j.core.model.PropertyType
import org.ec4j.core.model.PropertyType.PropertyValueParser.EnumValueParser

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
            SafeEnumValueParser(CodeStyleValue::class.java),
            CodeStyleValue.values().map { it.name }.toSet(),
        ),
        defaultValue = CodeStyleValue.official,
        defaultAndroidValue = CodeStyleValue.android,
    )
