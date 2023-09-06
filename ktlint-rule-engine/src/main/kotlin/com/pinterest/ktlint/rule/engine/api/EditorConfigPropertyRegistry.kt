package com.pinterest.ktlint.rule.engine.api

import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.RuleProvider
import com.pinterest.ktlint.rule.engine.core.api.RuleSetId
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.CODE_STYLE_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.END_OF_LINE_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.EditorConfigProperty
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.INDENT_SIZE_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.INDENT_STYLE_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.INSERT_FINAL_NEWLINE_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.MAX_LINE_LENGTH_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.createRuleExecutionEditorConfigProperty
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.createRuleSetExecutionEditorConfigProperty

/**
 * Build a registry of [EditorConfigProperty]'s that can be used to instantiate the [EditorConfigOverride] of the [KtLintRuleEngine]. When
 * possible, it is strongly advised to instantiate the [EditorConfigOverride] with compile versions of the [EditorConfigProperty] as that
 * ensures that value is of the correct type.
 *
 * In case that [Rule]s and their [EditorConfigProperty]'s are loaded at runtime, the [EditorConfigPropertyRegistry] has to be instantiated
 * with the same set of [RuleProvider]s that will be passed to the [KtLintRuleEngine]. Using the [EditorConfigPropertyRegistry], the
 * [EditorConfigProperty]'s can than be retrieved with the name as is stored in the `.editorconfig` file. Note: only properties defined in
 * the `ktlint-rule-engine-core` module, and properties defined in [Rule]s provided by the [RuleProvider]s can be found via the
 * [EditorConfigPropertyRegistry].
 */
public class EditorConfigPropertyRegistry(
    ruleProviders: Set<RuleProvider>,
) {
    private val properties =
        ruleProviders
            .map { it.createNewRuleInstance() }
            .flatMap { it.usesEditorConfigProperties }
            .plus(KTLINT_RULE_ENGINE_CORE_PROPERTIES)
            .distinct()

    /**
     * Finds the first [EditorConfigProperty] with name [propertyName]. Only properties defined in the `ktlint-rule-engine-core` module,
     * and properties defined in [Rule]s provided by the [RuleProvider]s to the [EditorConfigPropertyRegistry] will be found. An
     * [EditorConfigPropertyNotFoundException] is thrown when no property with name [propertyName] is found
     */
    public fun find(propertyName: String): EditorConfigProperty<*> =
        properties.findProperty(propertyName)
            ?: propertyName.toRuleExecutionPropertyOrNull()
            ?: throw EditorConfigPropertyNotFoundException(
                properties
                    .map { it.type.name }
                    .sorted()
                    .joinToString(
                        prefix = "Property with name '$propertyName' is not found in any of given rules. Available properties:\n\t",
                        separator = "\n\t",
                        postfix = "\nNext to properties above, the properties to enable or disable ktlint rules are allowed as well.",
                    ) { "- $it" },
            )

    private fun String.toRuleExecutionPropertyOrNull() =
        takeIf { it.startsWith("ktlint_") }
            ?.removePrefix("ktlint_")
            ?.replace("_", ":")
            ?.let {
                when {
                    RuleId.isValid(it) -> RuleId(it).createRuleExecutionEditorConfigProperty()
                    RuleSetId.isValid(it) -> RuleSetId(it).createRuleSetExecutionEditorConfigProperty()
                    else -> null
                }
            }

    private fun List<EditorConfigProperty<*>>.findProperty(propertyName: String): EditorConfigProperty<*>? =
        find { it.type.name == propertyName }

    private companion object {
        val KTLINT_RULE_ENGINE_CORE_PROPERTIES =
            listOf(
                CODE_STYLE_PROPERTY,
                END_OF_LINE_PROPERTY,
                INDENT_STYLE_PROPERTY,
                INDENT_SIZE_PROPERTY,
                INSERT_FINAL_NEWLINE_PROPERTY,
                MAX_LINE_LENGTH_PROPERTY,
            )
    }
}

public class EditorConfigPropertyNotFoundException(
    message: String,
) : RuntimeException(message)
