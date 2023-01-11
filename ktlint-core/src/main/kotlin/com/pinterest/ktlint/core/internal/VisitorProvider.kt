package com.pinterest.ktlint.core.internal

import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.api.EditorConfigProperties
import com.pinterest.ktlint.core.api.UsesEditorConfigProperties
import com.pinterest.ktlint.core.api.editorconfig.DISABLED_RULES_PROPERTY
import com.pinterest.ktlint.core.api.editorconfig.EditorConfigProperty
import com.pinterest.ktlint.core.api.editorconfig.KTLINT_DISABLED_RULES_PROPERTY
import com.pinterest.ktlint.core.api.editorconfig.RULE_EXECUTION_PROPERTY_TYPE
import com.pinterest.ktlint.core.api.editorconfig.RuleExecution
import com.pinterest.ktlint.core.api.editorconfig.createRuleExecutionEditorConfigProperty
import com.pinterest.ktlint.core.api.editorconfig.ktLintRuleExecutionPropertyName
import com.pinterest.ktlint.core.api.editorconfig.ktLintRuleSetExecutionPropertyName
import com.pinterest.ktlint.core.initKtLintKLogger
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
            createRuleExecutionEditorConfigProperty(toQualifiedRuleId(it.ruleSetId, it.ruleId))
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
                .filter { ruleRunner -> isEnabled(editorConfigProperties, ruleRunner.qualifiedRuleId) }
        if (enabledRuleRunners.isEmpty()) {
            LOGGER.debug { "Skipping file as no enabled rules are found to be executed" }
            return { _ -> }
        }
        val ruleRunnersToBeSkipped =
            ruleRunnersSorted
                .filter { ruleRunner ->
                    val runAfterRules = ruleRunner.runAfterRules
                    val runAfterRuleIds = runAfterRules.map { it.ruleId.toQualifiedRuleId() }
                    runAfterRules
                        .any {
                            it.runOnlyWhenOtherRuleIsEnabled &&
                                enabledRuleRunners.none { it.qualifiedRuleId in runAfterRuleIds }
                        }
                }
        if (ruleRunnersToBeSkipped.isNotEmpty()) {
            LOGGER.debug {
                ruleRunnersToBeSkipped
                    .forEach {
                        println(
                            "Skipping rule with id '${it.qualifiedRuleId}'. This rule has to run after rules with " +
                                "ids '${it.runAfterRules.joinToString { it.ruleId.toQualifiedRuleId() }}' and will " +
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

    private fun isEnabled(editorConfigProperties: EditorConfigProperties, qualifiedRuleId: String): Boolean =
        // For backwards compatibility all different properties which affects enabling/disabling of properties have to
        // be checked. Note that properties are checked in order of precedence. If a property of higher precedence has
        // been defined, all properties with lower precedence are ignore entirely.
        when {
            editorConfigProperties.containsKey(ktLintRuleExecutionPropertyName(qualifiedRuleId)) ||
                editorConfigProperties.containsKey(ktLintRuleSetExecutionPropertyName(qualifiedRuleId)) ->
                editorConfigProperties.isRuleEnabled(qualifiedRuleId)

            editorConfigProperties.containsKey(KTLINT_DISABLED_RULES_PROPERTY.name) ->
                editorConfigProperties.isEnabled(
                    KTLINT_DISABLED_RULES_PROPERTY,
                    qualifiedRuleId,
                ) && ruleSetId(qualifiedRuleId) != "experimental"

            editorConfigProperties.containsKey(DISABLED_RULES_PROPERTY.name) ->
                editorConfigProperties.isEnabled(
                    DISABLED_RULES_PROPERTY,
                    qualifiedRuleId,
                ) && ruleSetId(qualifiedRuleId) != "experimental"

            else ->
                ruleSetId(qualifiedRuleId) != "experimental"
        }

    private fun EditorConfigProperties.isRuleEnabled(qualifiedRuleId: String) =
        ruleExecution(ktLintRuleExecutionPropertyName(qualifiedRuleId))
            ?.let { it == RuleExecution.enabled }
            ?: isRuleSetEnabled(qualifiedRuleId)

    private fun EditorConfigProperties.isRuleSetEnabled(qualifiedRuleId: String) =
        ruleExecution(ktLintRuleSetExecutionPropertyName(qualifiedRuleId))
            .let { ruleSetExecution ->
                if (ruleSetExecution.name == "ktlint_experimental") {
                    // Rules in the experimental rule set are only run when enabled explicitly.
                    ruleSetExecution == RuleExecution.enabled
                } else {
                    // Rules in other rule sets are enabled by default.
                    ruleSetExecution != RuleExecution.disabled
                }
            }

    private fun EditorConfigProperties.ruleExecution(ruleExecutionPropertyName: String) =
        RULE_EXECUTION_PROPERTY_TYPE
            .parse(
                this[ruleExecutionPropertyName]?.sourceValue,
            ).parsed

    private fun EditorConfigProperties.isEnabled(
        disabledRulesProperty: EditorConfigProperty<String>,
        qualifiedRuleId: String,
    ) =
        this
            .getEditorConfigValue(disabledRulesProperty)
            .split(",")
            .none {
                // The rule set id in the disabled_rules setting may be omitted for rules in the standard rule set
                it.toQualifiedRuleId() == qualifiedRuleId
            }
}
