package com.pinterest.ktlint.rule.engine.internal.rulefilter

import com.pinterest.ktlint.logger.api.initKtLintKLogger
import com.pinterest.ktlint.rule.engine.api.KtLintRuleEngine
import com.pinterest.ktlint.rule.engine.core.api.Rule
import com.pinterest.ktlint.rule.engine.core.api.RuleProvider
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.ALL_RULES_EXECUTION_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.CODE_STYLE_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.CodeStyleValue
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.EXPERIMENTAL_RULES_EXECUTION_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.EditorConfig
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.EditorConfigProperty
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.RULE_EXECUTION_PROPERTY_TYPE
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.RuleExecution
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.ktLintRuleExecutionPropertyName
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.ktLintRuleSetExecutionPropertyName
import com.pinterest.ktlint.rule.engine.internal.rules.KTLINT_SUPPRESSION_RULE_ID
import io.github.oshai.kotlinlogging.KotlinLogging

private val LOGGER = KotlinLogging.logger {}.initKtLintKLogger()

/**
 * Filters the [RuleProvider]s defined in the [KtLintRuleEngine] for [Rule]s which are enabled for the given [EditorConfig].
 */
internal class RuleExecutionRuleFilter(
    private val editorConfig: EditorConfig,
) : RuleFilter {
    override fun filter(ruleProviders: Set<RuleProvider>): Set<RuleProvider> =
        if (disableKtlintEntirely()) {
            emptySet()
        } else {
            val ruleExecutionFilter =
                RuleExecutionFilter(
                    editorConfig.ruleExecutionProperties(ruleProviders),
                    editorConfig[CODE_STYLE_PROPERTY],
                )
            ruleProviders
                .filter { ruleExecutionFilter.isEnabled(it) }
                .toSet()
        }

    private fun disableKtlintEntirely() =
        editorConfig.getEditorConfigValueOrNull(RULE_EXECUTION_PROPERTY_TYPE, ALL_RULES_EXECUTION_PROPERTY.name) == RuleExecution.disabled

    private fun EditorConfig.ruleExecutionProperties(ruleProviders: Set<RuleProvider>): Map<String, RuleExecution> {
        val ruleExecutionPropertyNames =
            ruleExecutionPropertyNames(ruleProviders)
                .plus(ruleSetExecutionPropertyNames(ruleProviders))
                .plus(EXPERIMENTAL_RULES_EXECUTION_PROPERTY.name)
        return map { it.name to it.sourceValue }
            .toMap()
            .filterKeys { it in ruleExecutionPropertyNames }
            .mapValues { RULE_EXECUTION_PROPERTY_TYPE.parse(it.value).parsed }
    }

    private fun ruleExecutionPropertyNames(ruleProviders: Set<RuleProvider>) =
        ruleProviders
            .map { it.ruleId.ktLintRuleExecutionPropertyName() }
            .distinct()

    private fun ruleSetExecutionPropertyNames(ruleProviders: Set<RuleProvider>) =
        ruleProviders
            .map { it.ruleId.ruleSetId.ktLintRuleSetExecutionPropertyName() }
            .distinct()
}

/**
 * Creates a filter of [RuleExecution] properties. This filter is basically an extract of the [EditorConfig] which allows to retrieve rule
 * execution properties for which no proper [EditorConfigProperty] is defined as rules have no need to be able to extract such a property
 * from [EditorConfig].
 */
private class RuleExecutionFilter(
    val ruleExecutionProperties: Map<String, RuleExecution>,
    val codeStyleValue: CodeStyleValue,
) {
    fun isEnabled(ruleProvider: RuleProvider) = isRuleEnabled(ruleProvider.createNewRuleInstance())

    private fun isRuleEnabled(rule: Rule) =
        /*
         * If set for the rule, the rule execution property takes precedence above other checks. This allows for execution of a specific
         * experimental or ktlint_official code style rule without enabling them all. Also, this allows to disable a specific rule in case
         * the experimental and/or ktlint_official code style rules are enabled.
         */
        ruleExecution(rule.ruleId.ktLintRuleExecutionPropertyName())
            ?.let {
                if (rule.ruleId == KTLINT_SUPPRESSION_RULE_ID && it == RuleExecution.disabled) {
                    LOGGER.warn { "Rule '${rule.ruleId.value}' can not be disabled via the '.editorconfig'" }
                    true
                } else {
                    it == RuleExecution.enabled
                }
            }
            ?: isRuleConditionallyEnabled(rule)

    private fun isRuleConditionallyEnabled(rule: Rule) =
        when {
            rule is Rule.Experimental && rule is Rule.OfficialCodeStyle ->
                isExperimentalEnabled(rule) && isOfficialCodeStyleEnabled(rule)

            rule is Rule.Experimental ->
                isExperimentalEnabled(rule)

            rule is Rule.OfficialCodeStyle ->
                isOfficialCodeStyleEnabled(rule)

            else ->
                isRuleSetEnabled(rule)
        }

    private fun isExperimentalEnabled(rule: Rule) =
        ruleExecution(EXPERIMENTAL_RULES_EXECUTION_PROPERTY.name) == RuleExecution.enabled &&
            ruleExecution(rule.ruleId.ruleSetId.ktLintRuleSetExecutionPropertyName()) != RuleExecution.disabled &&
            ruleExecution(rule.ruleId.ktLintRuleExecutionPropertyName()) != RuleExecution.disabled

    private fun isOfficialCodeStyleEnabled(rule: Rule) =
        codeStyleValue == CodeStyleValue.ktlint_official &&
            ruleExecution(rule.ruleId.ruleSetId.ktLintRuleSetExecutionPropertyName()) != RuleExecution.disabled &&
            ruleExecution(rule.ruleId.ktLintRuleExecutionPropertyName()) != RuleExecution.disabled

    private fun isRuleSetEnabled(rule: Rule) =
        ruleExecution(rule.ruleId.ruleSetId.ktLintRuleSetExecutionPropertyName())
            .let { ruleSetExecution ->
                if (ruleSetExecution?.name == EXPERIMENTAL_RULES_EXECUTION_PROPERTY.name) {
                    // Rules in the experimental rule set are only run when enabled explicitly.
                    ruleSetExecution == RuleExecution.enabled
                } else {
                    // Rules in other rule sets are enabled by default.
                    ruleSetExecution != RuleExecution.disabled
                }
            }

    private fun ruleExecution(ruleExecutionPropertyName: String) = ruleExecutionProperties[ruleExecutionPropertyName]
}
