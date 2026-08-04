@file:Suppress("DEPRECATION")

package com.pinterest.ktlint.rule.engine.core.api.editorconfig

import org.ec4j.core.model.PropertyType

@Deprecated(message = "Provides backwards compatibility of custom ruleset JARs created for Ktlint 1.x. Don't use for RuleV2")
public val END_OF_LINE_PROPERTY: EditorConfigProperty<PropertyType.EndOfLineValue> =
    EditorConfigProperty(
        name = PropertyType.end_of_line.name,
        type = PropertyType.end_of_line,
        defaultValue = PropertyType.EndOfLineValue.lf,
    )
