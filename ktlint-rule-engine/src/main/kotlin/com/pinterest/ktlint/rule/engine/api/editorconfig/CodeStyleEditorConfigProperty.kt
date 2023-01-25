package com.pinterest.ktlint.rule.engine.api.editorconfig

import org.ec4j.core.model.PropertyType
import org.ec4j.core.model.PropertyType.PropertyValueParser.EnumValueParser

/**
 * Code style to be used while linting and formatting. Note that the [EnumValueParser] requires values to be lowercase.
 */
@Suppress("EnumEntryName")
public enum class CodeStyleValue {
    @Deprecated(
        message = "Marked for removal in KtLint 0.50. Value is renamed to 'android_studio'.",
        replaceWith = ReplaceWith("android_studio"),
    )
    android,

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

    @Deprecated(
        message = "Marked for removal in KtLint 0.50. Value is renamed to 'intellij_idea'.",
        replaceWith = ReplaceWith("intellij_idea"),
    )
    official,
}

public val CODE_STYLE_PROPERTY_TYPE: PropertyType.LowerCasingPropertyType<CodeStyleValue> =
    PropertyType.LowerCasingPropertyType(
        "ktlint_code_style",
        "The code style ('intellij_idea', 'android_studio' or 'ktlint_official') to be applied. Currently, the " +
            "value is defaulted to 'intellij_idea' when not set. However, in the future the default code style " +
            "will be changed to 'ktlint_official'.",
        SafeEnumValueParser(CodeStyleValue::class.java),
        CodeStyleValue.values().map { it.name }.toSet(),
    )

public val CODE_STYLE_PROPERTY: EditorConfigProperty<CodeStyleValue> =
    EditorConfigProperty(
        type = CODE_STYLE_PROPERTY_TYPE,
        /**
         * Once the [CodeStyleValue.ktlint_official] is matured, it will become the default code style of ktlint. Until
         * then the [CodeStyleValue.intellij_idea] is used to remain backwards compatible.
         */
        defaultValue = CodeStyleValue.intellij_idea,
        androidStudioCodeStyleDefaultValue = CodeStyleValue.android_studio,
        intellijIdeaCodeStyleDefaultValue = CodeStyleValue.intellij_idea,
        ktlintOfficialCodeStyleDefaultValue = CodeStyleValue.ktlint_official,
    )
