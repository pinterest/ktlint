package com.pinterest.ktlint.core.api.editorconfig

import com.pinterest.ktlint.core.IndentConfig
import org.ec4j.core.model.PropertyType

public val INDENT_SIZE_PROPERTY: EditorConfigProperty<Int> =
    EditorConfigProperty(
        name = PropertyType.indent_size.name,
        type = PropertyType.indent_size,
        defaultValue = IndentConfig.DEFAULT_INDENT_CONFIG.tabWidth,
        propertyMapper = { property, _ ->
            when {
                property?.isUnset == true -> -1
                property?.getValueAs<Int>() == null ->
                    IndentConfig.DEFAULT_INDENT_CONFIG.tabWidth

                else -> property.getValueAs()
            }
        },
    )
