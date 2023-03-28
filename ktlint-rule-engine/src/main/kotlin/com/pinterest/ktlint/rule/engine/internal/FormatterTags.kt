package com.pinterest.ktlint.rule.engine.internal

import com.pinterest.ktlint.rule.engine.core.api.editorconfig.EditorConfig
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.EditorConfigProperty
import org.ec4j.core.model.PropertyType

internal data class FormatterTags(
    val formatterTagOff: String?,
    val formatterTagOn: String?,
) {
    companion object {
        private val DISABLED_FORMATTER_TAGS = FormatterTags(null, null)

        fun from(editorConfig: EditorConfig): FormatterTags =
            if (editorConfig[FORMATTER_TAGS_ENABLED_PROPERTY]) {
                FormatterTags(
                    formatterTagOff = editorConfig[FORMATTER_TAG_OFF_ENABLED_PROPERTY],
                    formatterTagOn = editorConfig[FORMATTER_TAG_ON_ENABLED_PROPERTY],
                )
            } else {
                DISABLED_FORMATTER_TAGS
            }

        val FORMATTER_TAGS_ENABLED_PROPERTY: EditorConfigProperty<Boolean> =
            EditorConfigProperty(
                type =
                    PropertyType.LowerCasingPropertyType(
                        "ij_formatter_tags_enabled",
                        "When enabled, IntelliJ IDEA Formatter tags will be respected (e.g. disable and enable all ktlint rules for the " +
                            "code enclosed between the formatter tags.",
                        PropertyType.PropertyValueParser.BOOLEAN_VALUE_PARSER,
                        setOf(true.toString(), false.toString()),
                    ),
                defaultValue = false,
            )

        val FORMATTER_TAG_OFF_ENABLED_PROPERTY: EditorConfigProperty<String> =
            EditorConfigProperty(
                type =
                    PropertyType.LowerCasingPropertyType(
                        "ij_formatter_off_tag",
                        "The IntelliJ IDEA formatter tag to disable formatting. This also disables the ktlint rules.",
                        PropertyType.PropertyValueParser.IDENTITY_VALUE_PARSER,
                    ),
                defaultValue = "@formatter:off",
            )

        val FORMATTER_TAG_ON_ENABLED_PROPERTY: EditorConfigProperty<String> =
            EditorConfigProperty(
                type =
                    PropertyType.LowerCasingPropertyType(
                        "ij_formatter_on_tag",
                        "The IntelliJ IDEA formatter tag to enable formatting. This also enables the ktlint rules.",
                        PropertyType.PropertyValueParser.IDENTITY_VALUE_PARSER,
                    ),
                defaultValue = "@formatter:on",
            )
    }
}
