package io.github.ktlint.core.rule.engine.core.api.editorconfig

import org.ec4j.core.model.PropertyType

public val CODE_STYLE_PROPERTY_TYPE: PropertyType.LowerCasingPropertyType<CodeStyleValue> =
    PropertyType.LowerCasingPropertyType(
        "ktlint_code_style",
        "The code style ('ktlint_official', 'intellij_idea' or 'android_studio') to be applied. By default the 'ktlint_official' code " +
            "style is used",
        SafeEnumValueParser(CodeStyleValue::class.java),
        CodeStyleValue.entries.map { it.name.lowercase() }.toSet(),
    )

public val CODE_STYLE_PROPERTY: EditorConfigProperty<CodeStyleValue> =
    EditorConfigProperty(
        type = CODE_STYLE_PROPERTY_TYPE,
        defaultValue = CodeStyleValue.KTLINT_OFFICIAL,
        androidStudioCodeStyleDefaultValue = CodeStyleValue.ANDROID_STUDIO,
        intellijIdeaCodeStyleDefaultValue = CodeStyleValue.INTELLIJ_IDEA,
        ktlintOfficialCodeStyleDefaultValue = CodeStyleValue.KTLINT_OFFICIAL,
    )
