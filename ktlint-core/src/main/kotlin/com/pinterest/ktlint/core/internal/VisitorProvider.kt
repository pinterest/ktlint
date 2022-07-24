package com.pinterest.ktlint.core.internal

import com.pinterest.ktlint.core.KtLint
import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.RuleRunner
import com.pinterest.ktlint.core.api.DefaultEditorConfigProperties.disabledRulesProperty
import com.pinterest.ktlint.core.api.EditorConfigProperties
import com.pinterest.ktlint.core.api.UsesEditorConfigProperties
import com.pinterest.ktlint.core.api.UsesEditorConfigProperties.EditorConfigProperty

/**
 * The VisitorProvider is created for each file being scanned. As the [RuleRunnerSorter] logs the order in which the
 * rules are executed, a singleton instance of the class is used to prevent that the logs are flooded with duplicate
 * log lines.
 */
private val ruleRunnerSorter = RuleRunnerSorter()

internal class VisitorProvider(
    private val params: KtLint.ExperimentalParams,
    /**
     * Creates a new [RuleRunnerSorter]. Only to be used in unit tests where the same set of rules are used with distinct [Rule.VisitorModifier]s.
     */
    recreateRuleSorter: Boolean = false
) : UsesEditorConfigProperties {
    override val editorConfigProperties: List<EditorConfigProperty<*>> = listOf(disabledRulesProperty)

    /**
     * The list of [ruleRunners] is sorted based on the [Rule.VisitorModifier] of the rules.
     */
    private val ruleRunners: List<RuleRunner> =
        if (recreateRuleSorter) {
            RuleRunnerSorter()
        } else {
            ruleRunnerSorter
        }.getSortedRuleRunners(params.ruleRunners, params.debug)

    internal fun visitor(editorConfigProperties: EditorConfigProperties): ((rule: Rule, fqRuleId: String) -> Unit) -> Unit {
        val enabledRuleRunners =
            ruleRunners
                .filter { ruleRunner -> isNotDisabled(editorConfigProperties, ruleRunner.qualifiedRuleId) }
        if (enabledRuleRunners.isEmpty()) {
            if (params.debug && enabledRuleRunners.isEmpty()) {
                println(
                    "[DEBUG] Skipping file as no enabled rules are found to be executed"
                )
            }
            return { _ -> }
        }
        val ruleRunnersToBeSkipped =
            ruleRunners
                .filter { ruleRunner ->
                    val runAfterRule = ruleRunner.runAfterRule
                    runAfterRule != null &&
                        runAfterRule.runOnlyWhenOtherRuleIsEnabled &&
                        enabledRuleRunners.none { it.qualifiedRuleId == runAfterRule.ruleId.toQualifiedRuleId() }
                }
        if (params.debug && ruleRunnersToBeSkipped.isNotEmpty()) {
            ruleRunnersToBeSkipped
                .forEach {
                    println(
                        "[DEBUG] Skipping rule with id '${it.qualifiedRuleId}'. This rule has to run after rule with " +
                            "id '${it.runAfterRule?.ruleId?.toQualifiedRuleId()}' and will not run in case that rule is " +
                            "disabled."
                    )
                }
        }
        val ruleRunnersToExecute = enabledRuleRunners - ruleRunnersToBeSkipped.toSet()
        if (ruleRunnersToExecute.isEmpty()) {
            if (params.debug) {
                println(
                    "[DEBUG] Skipping file as no enabled rules are found to be executed"
                )
            }
            return { _ -> }
        }
        return { visit ->
            ruleRunnersToExecute.forEach {
                visit(it.getRule(), it.shortenedQualifiedRuleId)
            }
        }
    }

    private fun isNotDisabled(editorConfigProperties: EditorConfigProperties, qualifiedRuleId: String): Boolean =
        editorConfigProperties
            .getEditorConfigValue(disabledRulesProperty)
            .split(",")
            .none {
                // The rule set id in the disabled_rules setting may be omitted for rules in the standard rule set
                it.toQualifiedRuleId() == qualifiedRuleId
            }
}
