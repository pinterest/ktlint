package com.pinterest.ktlint.core.api.editorconfig

internal val DEFAULT_EDITOR_CONFIG_PROPERTIES: List<EditorConfigProperty<*>> =
    listOf(
        CODE_STYLE_PROPERTY,
        DISABLED_RULES_PROPERTY,
        KTLINT_DISABLED_RULES_PROPERTY,
        INDENT_STYLE_PROPERTY,
        INDENT_SIZE_PROPERTY,
        INSERT_FINAL_NEWLINE_PROPERTY,
        MAX_LINE_LENGTH_PROPERTY,
    )
