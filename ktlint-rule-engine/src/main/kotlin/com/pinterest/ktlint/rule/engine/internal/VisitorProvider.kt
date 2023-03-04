package com.pinterest.ktlint.rule.engine.internal

import com.pinterest.ktlint.logger.api.initKtLintKLogger
import com.pinterest.ktlint.rule.engine.core.api.Rule
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.EditorConfig
import mu.KotlinLogging

private val LOGGER = KotlinLogging.logger {}.initKtLintKLogger()

/**
 * The VisitorProvider is created for each file being scanned. As the [RuleRunnerSorter] logs the order in which the
 * rules are executed, a singleton instance of the class is used to prevent that the logs are flooded with duplicate
 * log lines.
 *
 * TODO: Investigate whether the [VisitorProvider] and [RULE_RUNNER_SORTER] can be moved to class [KtLintRuleEngine]. The set of RuleRunners
 *   and the [EditorConfig] will not change during the lifetime of the instance of the [KtLintRuleEngine].
 */
private val RULE_RUNNER_SORTER = RuleRunnerSorter()

internal class VisitorProvider(
    /**
     * The set of [RuleRunner]s to be executed. This set should not contain any [RuleRunner]s which are disabled via the [EditorConfig].
     */
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

    internal fun visitor(): ((rule: Rule, fqRuleId: String) -> Unit) -> Unit {
        if (ruleRunnersSorted.isEmpty()) {
            LOGGER.debug { "Skipping file as no enabled rules are found to be executed" }
            return { _ -> }
        }
        return { visit ->
            ruleRunnersSorted.forEach {
                // TODO: Remove it.ruleId.value parameter as it can be deducted from it.getRule().ruleId
                visit(it.getRule(), it.ruleId.value)
            }
        }
    }
}
