package com.pinterest.ktlint.rule.engine.internal

import com.pinterest.ktlint.rule.engine.core.api.editorconfig.CODE_STYLE_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.END_OF_LINE_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.EditorConfigProperty
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.INDENT_SIZE_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.INDENT_STYLE_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.INSERT_FINAL_NEWLINE_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.MAX_LINE_LENGTH_PROPERTY

internal val DEFAULT_EDITOR_CONFIG_PROPERTIES: List<EditorConfigProperty<*>> =
    listOf(
        CODE_STYLE_PROPERTY,
        END_OF_LINE_PROPERTY,
        INDENT_STYLE_PROPERTY,
        INDENT_SIZE_PROPERTY,
        INSERT_FINAL_NEWLINE_PROPERTY,
        MAX_LINE_LENGTH_PROPERTY,
    )
