package com.pinterest.ktlint.core.internal

import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.api.DefaultEditorConfigProperties.DISABLED_RULES_PROPERTY
import com.pinterest.ktlint.core.api.DefaultEditorConfigProperties.KTLINT_DISABLED_RULES_PROPERTY
import com.pinterest.ktlint.core.api.EditorConfigProperties
import com.pinterest.ktlint.core.api.UsesEditorConfigProperties
import com.pinterest.ktlint.core.api.UsesEditorConfigProperties.EditorConfigProperty
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
                .filter { ruleRunner -> isNotDisabled(editorConfigProperties, ruleRunner.qualifiedRuleId) }
        if (enabledRuleRunners.isEmpty()) {
            LOGGER.debug { "Skipping file as no enabled rules are found to be executed" }
            return { _ -> }
        }
        val ruleRunnersToBeSkipped =
            ruleRunnersSorted
                .filter { ruleRunner ->
                    val runAfterRule = ruleRunner.runAfterRule
                    runAfterRule != null &&
                        runAfterRule.runOnlyWhenOtherRuleIsEnabled &&
                        enabledRuleRunners.none { it.qualifiedRuleId == runAfterRule.ruleId.toQualifiedRuleId() }
                }
        if (ruleRunnersToBeSkipped.isNotEmpty()) {
            LOGGER.debug {
                ruleRunnersToBeSkipped
                    .forEach {
                        println(
                            "Skipping rule with id '${it.qualifiedRuleId}'. This rule has to run after rule with " +
                                "id '${it.runAfterRule?.ruleId?.toQualifiedRuleId()}' and will not run in case that rule is " +
                                "disabled.",
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

    private fun isNotDisabled(editorConfigProperties: EditorConfigProperties, qualifiedRuleId: String): Boolean {
        val ktlintDisabledRulesProperty =
            if (editorConfigProperties.containsKey(KTLINT_DISABLED_RULES_PROPERTY.type.name) ||
                !editorConfigProperties.containsKey(DISABLED_RULES_PROPERTY.type.name)
            ) {
                // New property takes precedence when defined, or, when both old and new property are not defined.
                editorConfigProperties.getEditorConfigValue(KTLINT_DISABLED_RULES_PROPERTY)
            } else {
                editorConfigProperties.getEditorConfigValue(DISABLED_RULES_PROPERTY)
            }
        return ktlintDisabledRulesProperty
            .split(",")
            .none {
                // The rule set id in the disabled_rules setting may be omitted for rules in the standard rule set
                it.toQualifiedRuleId() == qualifiedRuleId
            }
    }
}
