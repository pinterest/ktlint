package com.pinterest.ktlint.rule.engine.core.api.editorconfig

import org.ec4j.core.model.PropertyType
import org.ec4j.core.model.PropertyType.PropertyValueParser

/**
 * A [PropertyValueParser] implementation that allows a comma separate list of strings.
 */
public class CommaSeparatedListValueParser : PropertyValueParser<Set<String>> {
    override fun parse(
        name: String?,
        value: String?,
    ): PropertyType.PropertyValue<Set<String>> =
        if (value == "unset") {
            PropertyType.PropertyValue.valid(value, emptySet())
        } else {
            PropertyType.PropertyValue.valid(
                value,
                value
                    .orEmpty()
                    .split(",")
                    .map { it.trim() }
                    .toSet(),
            )
        }
}
