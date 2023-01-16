package com.pinterest.ktlint.core.api.editorconfig

import com.pinterest.ktlint.core.internal.ruleId
import com.pinterest.ktlint.core.internal.ruleSetId
import org.ec4j.core.model.PropertyType

@Suppress("EnumEntryName")
public enum class RuleExecution {
    enabled,
    disabled,
}

internal val RULE_EXECUTION_PROPERTY_TYPE =
    PropertyType.LowerCasingPropertyType(
        "ktlint_rule_execution",
        "When enabled, the rule is being executed.",
        SafeEnumValueParser(RuleExecution::class.java),
        RuleExecution.values().map { it.name }.toSet(),
    )

/**
 * Generates the rule execution '.editorconfig' property for the rule set of the given [qualifiedRuleId].
 */
public fun createRuleSetExecutionEditorConfigProperty(qualifiedRuleId: String): EditorConfigProperty<RuleExecution> =
    EditorConfigProperty(
        // Explicitly name the rule as multiple properties exists for the same type
        name = ktLintRuleSetExecutionPropertyName(qualifiedRuleId),
        type = RULE_EXECUTION_PROPERTY_TYPE,
        defaultValue = RuleExecution.enabled,
    )

/**
 * Generates the rule execution '.editorconfig' property for the given [qualifiedRuleId]. Note that the property is
 * the same as the [qualifiedRuleId] but prefixed with 'ktlint_' followed by the ruleSetId and ruleId which both are
 * based on the [qualifiedRuleId].
 */
public fun createRuleExecutionEditorConfigProperty(qualifiedRuleId: String): EditorConfigProperty<RuleExecution> =
    EditorConfigProperty(
        name = ktLintRuleExecutionPropertyName(qualifiedRuleId),
        type = RULE_EXECUTION_PROPERTY_TYPE,
        defaultValue = RuleExecution.enabled,
    )

/**
 * Constructs the name of the '.editorconfig' property that determines whether the given rule is to be executed.
 */
internal fun ktLintRuleExecutionPropertyName(qualifiedRuleId: String): String =
    "ktlint_${ruleSetId(qualifiedRuleId)}_${ruleId(qualifiedRuleId)}"

/**
 * Constructs the name of the '.editorconfig' property that determines whether the rule set of the given
 * [qualifiedRuleId] is to be executed.
 */
internal fun ktLintRuleSetExecutionPropertyName(qualifiedRuleId: String): String =
    "ktlint_${ruleSetId(qualifiedRuleId)}"
