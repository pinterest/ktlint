package com.pinterest.ktlint.core.internal

import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.api.FeatureInAlphaState
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
@FeatureInAlphaState
internal class EditorConfigGenerator(
    private val editorConfigLoader: EditorConfigLoader
) {
    /**
     * Method loads merged `.editorconfig` content using [com.pinterest.ktlint.core.KtLint.Params.fileName] path,
     * and then, by querying rules from [com.pinterest.ktlint.core.KtLint.Params.ruleSets]
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
        isAndroidCodeStyle: Boolean = false,
        debug: Boolean = false
    ): String {
        val editorConfig: Map<String, Property> = editorConfigLoader.loadPropertiesForFile(
            filePath = filePath,
            rules = rules,
            debug = debug
        )

        val potentialEditorConfigSettings = rules
            .mapNotNull { rule ->
                if (rule is UsesEditorConfigProperties && rule.editorConfigProperties.isNotEmpty()) {
                    rule
                        .editorConfigProperties
                        .map { property ->
                            val value = with(rule) {
                                editorConfig.writeEditorConfigProperty(
                                    property,
                                    isAndroidCodeStyle
                                )
                            }
                            logger.debug {
                                "Rule '${rule.id}' uses property '${property.type.name}' with default value '$value'"
                            }
                            ConfigurationSetting(
                                key = property.type.name,
                                value = value,
                                ruleId = rule.id
                            )
                        }
                } else {
                    null
                }
            }.flatten()
            .also { it.reportSettingsWithMultipleDistinctValues() }

        return potentialEditorConfigSettings
            .map { "${it.key} = ${it.value}" }
            .distinct()
            .sorted()
            .joinToString(separator = System.lineSeparator())
    }

    private fun List<ConfigurationSetting>.reportSettingsWithMultipleDistinctValues() =
        groupBy { it.key }
            .filter { (_, configurationSettingsGroup) -> configurationSettingsGroup.countDistinctValues() > 1 }
            .forEach {
                logger.error {
                    val ruleIds = it.value.joinToString { it.ruleId }
                    "Property '${it.key}' is used by by multiple rules ($ruleIds) which defines different default values for the property. Check the resulting '.editorcconfig' file carefully."
                }
            }

    private fun List<ConfigurationSetting>.countDistinctValues() =
        map { it.value }.distinct().size

    private data class ConfigurationSetting(
        val key: String,
        val value: String,
        val ruleId: String
    )
}
