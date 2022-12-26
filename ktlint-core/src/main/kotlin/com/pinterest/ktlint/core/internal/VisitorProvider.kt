package com.pinterest.ktlint.core.internal

import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.api.EditorConfigProperties
import com.pinterest.ktlint.core.api.UsesEditorConfigProperties
import com.pinterest.ktlint.core.api.editorconfig.DISABLED_RULES_PROPERTY
import com.pinterest.ktlint.core.api.editorconfig.EXPERIMENTAL_RULES_EXECUTION_PROPERTY
import com.pinterest.ktlint.core.api.editorconfig.EditorConfigProperty
import com.pinterest.ktlint.core.api.editorconfig.KTLINT_DISABLED_RULES_PROPERTY
import com.pinterest.ktlint.core.api.editorconfig.RULE_EXECUTION_PROPERTY_TYPE
import com.pinterest.ktlint.core.api.editorconfig.RuleExecution
import com.pinterest.ktlint.core.api.editorconfig.createRuleExecutionEditorConfigProperty
import com.pinterest.ktlint.core.api.editorconfig.ktLintRuleExecutionPropertyName
import com.pinterest.ktlint.core.api.editorconfig.ktLintRuleSetExecutionPropertyName
import com.pinterest.ktlint.core.initKtLintKLogger
import com.pinterest.ktlint.core.qualifiedRuleId
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
     * Creates a new [RuleRunnerSorter]. Only to be used in unit tests where the same set of rules are used with distinct [Rule.VisitorModifier]s.
     */
    recreateRuleSorter: Boolean = false,
) : UsesEditorConfigProperties {
    override val editorConfigProperties: List<EditorConfigProperty<*>> = listOf(
        KTLINT_DISABLED_RULES_PROPERTY,
        DISABLED_RULES_PROPERTY,
    ).plus(
        ruleRunners.map {
            createRuleExecutionEditorConfigProperty(it.qualifiedRuleId)
        },
    )

    /**
     * The list of [ruleRunnersSorted] is sorted based on the [Rule.VisitorModifier] of the rules.
     */
    private val ruleRunnersSorted: List<RuleRunner> =
        if (recreateRuleSorter) {
            RuleRunnerSorter()
        } else {
            RULE_RUNNER_SORTER
        }.getSortedRuleRunners(ruleRunners)

    internal fun visitor(editorConfigProperties: EditorConfigProperties): ((rule: Rule, fqRuleId: String) -> Unit) -> Unit {
        val enabledRuleRunners =
            ruleRunnersSorted
                .filter { ruleRunner -> ruleRunner.getRule().isEnabled(editorConfigProperties) }
        if (enabledRuleRunners.isEmpty()) {
            LOGGER.debug { "Skipping file as no enabled rules are found to be executed" }
            return { _ -> }
        }
        val ruleRunnersToBeSkipped =
            ruleRunnersSorted
                .filter { ruleRunner ->
                    val runAfterRules = ruleRunner.runAfterRules
                    val runAfterRuleIds = runAfterRules.map { it.qualifiedRuleId }
                    runAfterRules
                        .any {
                            it.runOnlyWhenOtherRuleIsEnabled &&
                                enabledRuleRunners.none { it.qualifiedRuleId in runAfterRuleIds }
                        }
                }
        if (ruleRunnersToBeSkipped.isNotEmpty()) {
            LOGGER.debug {
                ruleRunnersToBeSkipped
                    .forEach { ruleRunner ->
                        println(
                            "Skipping rule with id '${ruleRunner.qualifiedRuleId}'. This rule has to run after rules with " +
                                "ids '${ruleRunner.runAfterRules.joinToString { it.qualifiedRuleId }}' and will " +
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
                visit(it.getRule(), it.shortenedQualifiedRuleId)
            }
        }
    }

    private fun Rule.isEnabled(editorConfigProperties: EditorConfigProperties): Boolean =
        // For backwards compatibility all different properties which affects enabling/disabling of properties have to
        // be checked. Note that properties are checked in order of precedence. If a property of higher precedence has
        // been defined, all properties with lower precedence are ignore entirely.
        when {
            editorConfigProperties.containsKey(ktLintRuleExecutionPropertyName(qualifiedRuleId)) ||
                editorConfigProperties.containsKey(ktLintRuleSetExecutionPropertyName(qualifiedRuleId)) ->
                editorConfigProperties.isRuleEnabled(this)

            editorConfigProperties.containsKey(KTLINT_DISABLED_RULES_PROPERTY.name) ->
                editorConfigProperties.isEnabled(
                    KTLINT_DISABLED_RULES_PROPERTY,
                    this,
                )

            editorConfigProperties.containsKey(DISABLED_RULES_PROPERTY.name) ->
                editorConfigProperties.isEnabled(
                    DISABLED_RULES_PROPERTY,
                    this,
                )

            else ->
                editorConfigProperties.isRuleEnabled(this)
        }

    private fun EditorConfigProperties.isRuleEnabled(rule: Rule) =
        ruleExecution(rule.ktLintRuleExecutionPropertyName())
            ?.let { it == RuleExecution.enabled }
            ?: if (rule is Rule.Experimental) {
                isExperimentalEnabled(rule)
            } else {
                isRuleSetEnabled(rule)
            }

    private fun EditorConfigProperties.isExperimentalEnabled(rule: Rule) =
        ruleExecution(EXPERIMENTAL_RULES_EXECUTION_PROPERTY.name) == RuleExecution.enabled &&
            ruleExecution(rule.ktLintRuleSetExecutionPropertyName()) != RuleExecution.disabled &&
            ruleExecution(rule.ktLintRuleExecutionPropertyName()) != RuleExecution.disabled

    private fun EditorConfigProperties.isRuleSetEnabled(rule: Rule) =
        ruleExecution(rule.ktLintRuleSetExecutionPropertyName())
            .let { ruleSetExecution ->
                if (rule.ruleSetId == "standard") {
                    // Rules in the standard rule set are enabled by default. So those rules should run unless the rule set
                    // is disabled explicitly.
                    ruleSetExecution != RuleExecution.disabled
                } else {
                    // Rules in non-standard rule set are disabled by default. So rules may only run when the rule set is
                    // enabled explicitly.
                    ruleSetExecution == RuleExecution.enabled
                }
            }
            ?: false

    private fun EditorConfigProperties.ruleExecution(ruleExecutionPropertyName: String) =
        RULE_EXECUTION_PROPERTY_TYPE
            .parse(
                this[ruleExecutionPropertyName]?.sourceValue,
            ).parsed

    private fun EditorConfigProperties.isEnabled(
        disabledRulesProperty: EditorConfigProperty<String>,
        rule: Rule,
    ) =
        this
            .getEditorConfigValue(disabledRulesProperty)
            // When IntelliJ IDEA is reformatting the ".editorconfig" file it sometimes adds a space after the comma in a
            // comma-separate-list which should not be a part of the ruleId
            .replace(" ", "")
            .split(",")
            .none {
                // The rule set id in the disabled_rules setting may be omitted for rules in the standard rule set
                it.qualifiedRuleId() == rule.qualifiedRuleId
            }
}
