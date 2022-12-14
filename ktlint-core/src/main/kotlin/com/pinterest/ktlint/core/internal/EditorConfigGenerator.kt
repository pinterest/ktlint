package com.pinterest.ktlint.core.internal

import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.api.DefaultEditorConfigProperties.writeEditorConfigProperty
import com.pinterest.ktlint.core.api.EditorConfigOverride
import com.pinterest.ktlint.core.api.UsesEditorConfigProperties
import com.pinterest.ktlint.core.api.editorconfig.CODE_STYLE_PROPERTY
import com.pinterest.ktlint.core.api.editorconfig.CodeStyleValue
import com.pinterest.ktlint.core.api.editorconfig.DEFAULT_EDITOR_CONFIG_PROPERTIES
import com.pinterest.ktlint.core.initKtLintKLogger
import java.nio.file.Path
import mu.KotlinLogging
import org.ec4j.core.model.Property

private val LOGGER = KotlinLogging.logger {}.initKtLintKLogger()

/**
 * Generates Kotlin section content for `.editorconfig` file.
 *
 * Rule should implement [UsesEditorConfigProperties] interface to support this.
 */
internal class EditorConfigGenerator(
    private val editorConfigLoader: EditorConfigLoader,
) {
    /**
     * Method loads merged `.editorconfig` content using [filePath], and then, by querying the set of [Rule]s
     * generates Kotlin section (default is `[*.{kt,kts}]`) content including expected default values.
     *
     * @return Kotlin section editorconfig content. For example:
     * ```properties
     * final-newline = true
     * indent-size = 4
     * ```
     */
    fun generateEditorconfig(
        filePath: Path,
        rules: Set<Rule>,
        codeStyle: CodeStyleValue,
    ): String {
        val editorConfig: Map<String, Property> =
            editorConfigLoader
                .load(
                    filePath = filePath,
                    rules = rules,
                    editorConfigOverride = EditorConfigOverride.from(CODE_STYLE_PROPERTY to codeStyle.name),
                )

        val potentialEditorConfigSettings =
            getConfigurationSettingsForRules(rules, editorConfig, codeStyle)
                .plus(getConfigurationSettingsForDefaultEditorConfigProperties(editorConfig, codeStyle))
                .also { it.reportSettingsWithMultipleDistinctValues() }

        return potentialEditorConfigSettings
            .map { "${it.key} = ${it.value}" }
            .distinct()
            .sorted()
            .joinToString(separator = System.lineSeparator())
    }

    private fun getConfigurationSettingsForRules(
        rules: Set<Rule>,
        editorConfig: Map<String, Property>,
        codeStyle: CodeStyleValue,
    ) = rules
        .mapNotNull { rule ->
            if (rule is UsesEditorConfigProperties && rule.editorConfigProperties.isNotEmpty()) {
                rule
                    .editorConfigProperties
                    .map { property ->
                        val value = with(rule) {
                            editorConfig.writeEditorConfigProperty(
                                property,
                                codeStyle,
                            )
                        }
                        LOGGER.debug {
                            "Rule '${rule.id}' uses property '${property.type.name}' with value '$value'"
                        }
                        ConfigurationSetting(
                            key = property.type.name,
                            value = value,
                            usage = "Rule '${rule.id}'",
                        )
                    }
            } else {
                null
            }
        }.flatten()

    private fun getConfigurationSettingsForDefaultEditorConfigProperties(
        editorConfig: Map<String, Property>,
        codeStyle: CodeStyleValue,
    ): List<ConfigurationSetting> {
        return DEFAULT_EDITOR_CONFIG_PROPERTIES
            .filter { it.deprecationWarning == null && it.deprecationError == null }
            .map { editorConfigProperty ->
                val value = editorConfig.writeEditorConfigProperty(
                    editorConfigProperty,
                    codeStyle,
                )

                LOGGER.debug {
                    "Editor config property '${editorConfigProperty.name}' has default value '$value'"
                }
                ConfigurationSetting(
                    key = editorConfigProperty.name,
                    value = value,
                    usage = "Default value",
                )
            }
    }

    private fun List<ConfigurationSetting>.reportSettingsWithMultipleDistinctValues() =
        groupBy { it.key }
            .filter { (_, configurationSettingsGroup) -> configurationSettingsGroup.countDistinctValues() > 1 }
            .forEach {
                LOGGER.error {
                    val usages = it.value.joinToString { it.usage }.toList().sorted()
                    "Property '${it.key}' has multiple usages ($usages) which defines different default values for the property. Check the resulting '.editorcconfig' file carefully."
                }
            }

    private fun List<ConfigurationSetting>.countDistinctValues() =
        map { it.value }.distinct().size

    private data class ConfigurationSetting(
        val key: String,
        val value: String,
        val usage: String,
    )
}
