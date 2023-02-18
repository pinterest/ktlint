package com.pinterest.ktlint.rule.engine.internal

import com.pinterest.ktlint.rule.engine.api.EditorConfigOverride
import com.pinterest.ktlint.rule.engine.core.api.Rule
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.CODE_STYLE_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.CodeStyleValue
import java.nio.file.Path

/**
 * Generates a Kotlin section content for the `.editorconfig` file. The set of properties is based on the properties used by the given set
 * of [Rule]s. The default values for missing properties are based on the given [CodeStyleValue].
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
        val usedEditorConfigProperties =
            rules
                .map { it.usesEditorConfigProperties }
                .flatten()
                .toSet()

        val editorConfig =
            editorConfigLoader
                .load(
                    filePath = filePath,
                    rules = rules,
                    editorConfigOverride = EditorConfigOverride.from(CODE_STYLE_PROPERTY to codeStyle.name),
                ).filterBy(usedEditorConfigProperties.plus(DEFAULT_EDITOR_CONFIG_PROPERTIES))

        return editorConfig
            .map { "${it.name} = ${it.sourceValue}" }
            .distinct()
            .sorted()
            .joinToString(separator = System.lineSeparator())
    }
}
