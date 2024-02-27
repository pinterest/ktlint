package com.pinterest.ktlint.rule.engine.core.api.editorconfig

import org.ec4j.core.model.PropertyType
import org.ec4j.core.model.PropertyType.PropertyValueParser.EnumValueParser

/**
 * Code style to be used while linting and formatting. Note that the [EnumValueParser] requires values to be lowercase.
 */
@Suppress("EnumEntryName")
public enum class CodeStyleValue {
    /**
     * Code formatting based on Android's Kotlin styleguide (https://developer.android.com/kotlin/style-guide). This
     * code style aims to be compatible with code formatting in Android Studio.
     */
    android_studio,

    /**
     * Code formatting based on Kotlin Coding conventions (https://kotlinlang.org/docs/coding-conventions.html). This
     * code style aims to be compatible with code formatting in IntelliJ IDEA.
     */
    intellij_idea,

    /**
     * Code formatting based on the best of both the Kotlin Coding conventions
     * (https://kotlinlang.org/docs/coding-conventions.html) and Android's Kotlin styleguide
     * (https://developer.android.com/kotlin/style-guide). This codestyle also provides additional formatting on topics
     * which are not (explicitly) mentioned in the before mentioned styleguide. Also, this code style sometimes formats
     * code in a way which is not compatible with the default code formatters in IntelliJ IDEA and Android Studio. When
     * using this codestyle, it is best to disable (e.g. not use) automatic code formatting in the editor. Mean reason
     * for deviating from the code formatting provided by the editor is that those contain bugs which after some years
     * are still not fixed.
     * In the long run, this code style becomes the default code style provided by KtLint.
     */
    ktlint_official,
}

public val CODE_STYLE_PROPERTY_TYPE: PropertyType.LowerCasingPropertyType<CodeStyleValue> =
    PropertyType.LowerCasingPropertyType(
        "ktlint_code_style",
        "The code style ('ktlint_official', 'intellij_idea' or 'android_studio') to be applied. By default the 'ktlint_official' code " +
            "style is used",
        SafeEnumValueParser(CodeStyleValue::class.java),
        CodeStyleValue.entries.map { it.name }.toSet(),
    )

public val CODE_STYLE_PROPERTY: EditorConfigProperty<CodeStyleValue> =
    EditorConfigProperty(
        type = CODE_STYLE_PROPERTY_TYPE,
        defaultValue = CodeStyleValue.ktlint_official,
        androidStudioCodeStyleDefaultValue = CodeStyleValue.android_studio,
        intellijIdeaCodeStyleDefaultValue = CodeStyleValue.intellij_idea,
        ktlintOfficialCodeStyleDefaultValue = CodeStyleValue.ktlint_official,
    )
