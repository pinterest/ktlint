package com.pinterest.ktlint.rule.engine.core.api.editorconfig

import org.ec4j.core.model.PropertyType

@Deprecated(message = "Provides backwards compatibility of custom ruleset JARs created for Ktlint 1.x. Don't use for RuleV2")
public val INDENT_STYLE_PROPERTY: EditorConfigProperty<PropertyType.IndentStyleValue> =
    EditorConfigProperty(
        name = PropertyType.indent_style.name,
        type = PropertyType.indent_style,
        defaultValue = PropertyType.IndentStyleValue.space,
    )
