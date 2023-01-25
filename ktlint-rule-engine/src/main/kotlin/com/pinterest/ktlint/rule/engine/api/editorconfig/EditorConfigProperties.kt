package com.pinterest.ktlint.rule.engine.api.editorconfig

internal val DEFAULT_EDITOR_CONFIG_PROPERTIES: List<EditorConfigProperty<*>> =
    listOf(
        CODE_STYLE_PROPERTY,
        INDENT_STYLE_PROPERTY,
        INDENT_SIZE_PROPERTY,
        INSERT_FINAL_NEWLINE_PROPERTY,
        MAX_LINE_LENGTH_PROPERTY,
    )
