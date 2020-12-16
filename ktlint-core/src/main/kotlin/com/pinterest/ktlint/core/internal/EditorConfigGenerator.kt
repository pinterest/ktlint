package com.pinterest.ktlint.core.internal

import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.api.FeatureInAlphaState
import com.pinterest.ktlint.core.api.UsesEditorConfigProperties
import java.nio.file.Path
import org.ec4j.core.model.Property

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

        return rules
            .fold(mutableMapOf<String, String?>()) { acc, rule ->
                if (rule is UsesEditorConfigProperties) {
                    if (debug) println("Checking properties for '${rule.id}' rule")
                    rule.editorConfigProperties.forEach { prop ->
                        if (debug) println("Setting '${prop.type.name}' property value")
                        acc[prop.type.name] = with(rule) {
                            editorConfig.writeEditorConfigProperty(
                                prop,
                                isAndroidCodeStyle
                            )
                        }
                    }
                }

                acc
            }
            .filterValues { it != null }
            .map {
                "${it.key} = ${it.value}"
            }
            .joinToString(separator = System.lineSeparator())
    }
}
