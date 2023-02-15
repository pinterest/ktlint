package com.pinterest.ktlint.rule.engine.core.api.editorconfig

import org.ec4j.core.model.PropertyType

private const val DEFAULT_INDENT_SIZE = 4

public val INDENT_SIZE_PROPERTY: EditorConfigProperty<Int> =
    EditorConfigProperty(
        name = PropertyType.indent_size.name,
        type = PropertyType.indent_size,
        defaultValue = DEFAULT_INDENT_SIZE,
        propertyMapper = { property, _ ->
            if (property?.isUnset == true) {
                -1
            } else {
                property
                    ?.getValueAs<Int>()
                    .let {
                        if (it == null || it <= 0) {
                            DEFAULT_INDENT_SIZE
                        } else {
                            it
                        }
                    }
            }
        },
    )
