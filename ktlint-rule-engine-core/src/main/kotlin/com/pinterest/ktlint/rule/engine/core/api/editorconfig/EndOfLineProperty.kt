package com.pinterest.ktlint.rule.engine.core.api.editorconfig

import org.ec4j.core.model.PropertyType

public val END_OF_LINE_PROPERTY: EditorConfigProperty<PropertyType.EndOfLineValue> =
    EditorConfigProperty(
        name = PropertyType.end_of_line.name,
        type = PropertyType.end_of_line,
        defaultValue = PropertyType.EndOfLineValue.lf,
    )
