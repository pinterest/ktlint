package com.pinterest.ktlint.rule.engine.core.api.editorconfig

import org.ec4j.core.model.PropertyType

public val INSERT_FINAL_NEWLINE_PROPERTY: EditorConfigProperty<Boolean> =
    EditorConfigProperty(
        name = PropertyType.insert_final_newline.name,
        type = PropertyType.insert_final_newline,
        defaultValue = true,
    )
