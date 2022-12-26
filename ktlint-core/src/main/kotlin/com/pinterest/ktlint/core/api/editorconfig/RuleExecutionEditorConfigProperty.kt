package com.pinterest.ktlint.core.api.editorconfig

import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.ruleId
import com.pinterest.ktlint.core.ruleSetId
import org.ec4j.core.model.PropertyType

@Suppress("EnumEntryName")
public enum class RuleExecution {
    enabled,
    disabled,
}

internal val RULE_EXECUTION_PROPERTY_TYPE =
    PropertyType.LowerCasingPropertyType(
        "ktlint_rule_execution",
        "When enabled, rule execution is allowed. This property can de defined at different levels like an entire ruleset, a specific " +
            "rule or a specific property of the rule.",
        SafeEnumValueParser(RuleExecution::class.java),
        CodeStyleValue.values().map { it.name }.toSet(),
    )

/**
 * When enabled, a rule that implements interface "Rule.Experimental" is executed unless that rule itself is explicitly disabled.
 */
public val EXPERIMENTAL_RULES_EXECUTION_PROPERTY: EditorConfigProperty<RuleExecution> =
    EditorConfigProperty(
        // Explicitly name the rule as multiple properties exists for this property type
        name = "ktlint_experimental",
        type = RULE_EXECUTION_PROPERTY_TYPE,
        defaultValue = RuleExecution.disabled,
    )

/**
 * Generates the rule execution '.editorconfig' property for the given rule set id.
 */
public fun createRuleSetExecutionEditorConfigProperty(ruleSetId: String): EditorConfigProperty<RuleExecution> =
    EditorConfigProperty(
        // Explicitly name the rule as multiple properties exists for this property type
        name = ktLintRuleSetExecutionPropertyName(ruleSetId),
        type = RULE_EXECUTION_PROPERTY_TYPE,
        defaultValue = RuleExecution.enabled,
    )

/**
 * Generates the rule execution '.editorconfig' property for the rule set id of the given [Rule].
 */
public fun Rule.createRuleSetExecutionEditorConfigProperty(): EditorConfigProperty<RuleExecution> =
    EditorConfigProperty(
        // Explicitly name the rule as multiple properties exists for this property type
        name = ktLintRuleSetExecutionPropertyName(),
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
    "ktlint_${qualifiedRuleId.ruleSetId()}_${qualifiedRuleId.ruleId()}"

/**
 * Constructs the name of the '.editorconfig' property that determines whether the given [Rule] is to be executed.
 */
internal fun Rule.ktLintRuleExecutionPropertyName(): String =
    "ktlint_${ruleSetId}_$ruleId"

/**
 * Constructs the name of the '.editorconfig' property that determines whether the rule set of the given
 * [qualifiedRuleId] is to be executed.
 */
internal fun ktLintRuleSetExecutionPropertyName(qualifiedRuleId: String): String =
    "ktlint_${qualifiedRuleId.ruleSetId()}"

/**
 * Constructs the name of the '.editorconfig' property that determines whether the rule set of the given
 * [Rule] is to be executed.
 */
internal fun Rule.ktLintRuleSetExecutionPropertyName(): String =
    "ktlint_$ruleSetId"
