package com.pinterest.ktlint.rule.engine.internal

import com.pinterest.ktlint.logger.api.initKtLintKLogger
import com.pinterest.ktlint.rule.engine.core.api.Rule
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.CODE_STYLE_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.CodeStyleValue
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.EXPERIMENTAL_RULES_EXECUTION_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.EditorConfig
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.RULE_EXECUTION_PROPERTY_TYPE
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.RuleExecution
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.ktLintRuleExecutionPropertyName
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.ktLintRuleSetExecutionPropertyName
import mu.KotlinLogging

private val LOGGER = KotlinLogging.logger {}.initKtLintKLogger()

/**
 * The VisitorProvider is created for each file being scanned. As the [RuleRunnerSorter] logs the order in which the
 * rules are executed, a singleton instance of the class is used to prevent that the logs are flooded with duplicate
 * log lines.
 */
private val RULE_RUNNER_SORTER = RuleRunnerSorter()

internal class VisitorProvider(
    ruleRunners: Set<RuleRunner>,
    /**
     * Creates a new [RuleRunnerSorter]. Only to be used in unit tests where the same set of rules are used with distinct
     * [Rule.VisitorModifier]s.
     */
    recreateRuleSorter: Boolean = false,
) {
    /**
     * The list of [ruleRunnersSorted] is sorted based on the [Rule.VisitorModifier] of the rules.
     */
    private val ruleRunnersSorted: List<RuleRunner> =
        if (recreateRuleSorter) {
            RuleRunnerSorter()
        } else {
            RULE_RUNNER_SORTER
        }.getSortedRuleRunners(ruleRunners)

    internal fun visitor(editorConfig: EditorConfig): ((rule: Rule, fqRuleId: String) -> Unit) -> Unit {
        val enabledRuleRunners =
            ruleRunnersSorted
                .filter { ruleRunner -> editorConfig.isRuleEnabled(ruleRunner.getRule()) }
        if (enabledRuleRunners.isEmpty()) {
            LOGGER.debug { "Skipping file as no enabled rules are found to be executed" }
            return { _ -> }
        }
        val ruleRunnersToBeSkipped =
            ruleRunnersSorted
                .filter { ruleRunner ->
                    val runAfterRules = ruleRunner.runAfterRules
                    val runAfterRuleIds = runAfterRules.map { it.ruleId }
                    runAfterRules
                        .any { runAfterRule ->
                            runAfterRule.runOnlyWhenOtherRuleIsEnabled &&
                                enabledRuleRunners.none { it.ruleId in runAfterRuleIds }
                        }
                }
        if (ruleRunnersToBeSkipped.isNotEmpty()) {
            LOGGER.debug {
                ruleRunnersToBeSkipped
                    .forEach { ruleRunner ->
                        println(
                            "Skipping rule with id '${ruleRunner.ruleId.value}'. This rule has to run after rules with " +
                                "ids '${ruleRunner.runAfterRules.joinToString { it.ruleId.value }}' and will " +
                                "not run in case that rule is disabled.",
                        )
                    }
            }
        }
        val ruleRunnersToExecute = enabledRuleRunners - ruleRunnersToBeSkipped.toSet()
        if (ruleRunnersToExecute.isEmpty()) {
            LOGGER.debug { "Skipping file as no enabled rules are found to be executed" }
            return { _ -> }
        }
        return { visit ->
            ruleRunnersToExecute.forEach {
                // TODO: Remove it.ruleId.value parameter as it can be deducted from it.getRule().ruleId
                visit(it.getRule(), it.ruleId.value)
            }
        }
    }

    private fun EditorConfig.isRuleEnabled(rule: Rule) =
        // If set for the rule, the rule execution property takes precedence above other checks. This allows for execution of a specific
        // experimental or ktlint_official code style rule without enabling them all. Also, this allows to disable a specific rule in case
        // the experimental and/or ktlint_official code style rules are enabled.
        ruleExecution(rule.ruleId.ktLintRuleExecutionPropertyName())
            ?.let { it == RuleExecution.enabled }
            ?: isRuleConditionallyEnabled(rule)

    private fun EditorConfig.isRuleConditionallyEnabled(rule: Rule) =
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

    private fun EditorConfig.isExperimentalEnabled(rule: Rule) =
        ruleExecution(EXPERIMENTAL_RULES_EXECUTION_PROPERTY.name) == RuleExecution.enabled &&
            ruleExecution(rule.ruleId.ruleSetId.ktLintRuleSetExecutionPropertyName()) != RuleExecution.disabled &&
            ruleExecution(rule.ruleId.ktLintRuleExecutionPropertyName()) != RuleExecution.disabled

    private fun EditorConfig.isOfficialCodeStyleEnabled(rule: Rule) =
        this[CODE_STYLE_PROPERTY] == CodeStyleValue.ktlint_official &&
            ruleExecution(rule.ruleId.ruleSetId.ktLintRuleSetExecutionPropertyName()) != RuleExecution.disabled &&
            ruleExecution(rule.ruleId.ktLintRuleExecutionPropertyName()) != RuleExecution.disabled

    private fun EditorConfig.isRuleSetEnabled(rule: Rule) =
        ruleExecution(rule.ruleId.ruleSetId.ktLintRuleSetExecutionPropertyName())
            .let { ruleSetExecution ->
                if (ruleSetExecution?.name == "ktlint_experimental") {
                    // Rules in the experimental rule set are only run when enabled explicitly.
                    ruleSetExecution == RuleExecution.enabled
                } else {
                    // Rules in other rule sets are enabled by default.
                    ruleSetExecution != RuleExecution.disabled
                }
            }

    private fun EditorConfig.ruleExecution(ruleExecutionPropertyName: String) =
        this.getEditorConfigValue(RULE_EXECUTION_PROPERTY_TYPE, ruleExecutionPropertyName)
}
