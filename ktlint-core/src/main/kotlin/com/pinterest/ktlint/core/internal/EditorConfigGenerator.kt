package com.pinterest.ktlint.core.internal

import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.api.DefaultEditorConfigProperties
import com.pinterest.ktlint.core.api.UsesEditorConfigProperties
import com.pinterest.ktlint.core.initKtLintKLogger
import java.nio.file.Path
import mu.KotlinLogging
import org.ec4j.core.model.Property

private val logger = KotlinLogging.logger {}.initKtLintKLogger()

/**
 * Generates Kotlin section content for `.editorconfig` file.
 *
 * Rule should implement [UsesEditorConfigProperties] interface to support this.
 */
internal class EditorConfigGenerator(
    private val editorConfigLoader: EditorConfigLoader,
) {
    /**
     * Method loads merged `.editorconfig` content using [com.pinterest.ktlint.core.KtLint.ExperimentalParams.fileName] path,
     * and then, by querying rules from [com.pinterest.ktlint.core.KtLint.ExperimentalParams.ruleSets]
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
        debug: Boolean = false,
        codeStyle: DefaultEditorConfigProperties.CodeStyleValue,
    ): String {
        val editorConfig: Map<String, Property> = editorConfigLoader.loadPropertiesForFile(
            filePath = filePath,
            rules = rules,
            debug = debug,
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
        codeStyle: DefaultEditorConfigProperties.CodeStyleValue,
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
                        logger.debug {
                            "Rule '${rule.id}' uses property '${property.type.name}' with default value '$value'"
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
        codeStyle: DefaultEditorConfigProperties.CodeStyleValue,
    ) = DefaultEditorConfigProperties
        .editorConfigProperties
        .map { editorConfigProperty ->
            val value = with((DefaultEditorConfigProperties as UsesEditorConfigProperties)) {
                editorConfig.writeEditorConfigProperty(
                    editorConfigProperty,
                    codeStyle,
                )
            }
            logger.debug {
                "Class '${DefaultEditorConfigProperties::class.simpleName}' uses property '${editorConfigProperty.type.name}' with default value '$value'"
            }
            ConfigurationSetting(
                key = editorConfigProperty.type.name,
                value = value,
                usage = "Class '${DefaultEditorConfigProperties::class.simpleName}'",
            )
        }

    private fun List<ConfigurationSetting>.reportSettingsWithMultipleDistinctValues() =
        groupBy { it.key }
            .filter { (_, configurationSettingsGroup) -> configurationSettingsGroup.countDistinctValues() > 1 }
            .forEach {
                logger.error {
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
