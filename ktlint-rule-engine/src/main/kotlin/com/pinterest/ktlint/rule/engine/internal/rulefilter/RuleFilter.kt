package com.pinterest.ktlint.rule.engine.internal.rulefilter

import com.pinterest.ktlint.rule.engine.api.KtLintRuleEngine
import com.pinterest.ktlint.rule.engine.internal.RuleRunner

/**
 * Gets the rule runners for the [KtLintRuleEngine] by applying the [ruleFilters] in the given order on the set of [RuleRunner]s provided
 * by the previous (or the initial list of [RuleRunner]s).
 */
internal fun KtLintRuleEngine.ruleRunners(vararg ruleFilters: RuleFilter): Set<RuleRunner> {
    var ruleRunners = initialRuleRunners()
    val ruleFilterIterator = ruleFilters.iterator()
    while (ruleFilterIterator.hasNext()) {
        val ruleFilter = ruleFilterIterator.next()
        ruleRunners = ruleFilter.filter(ruleRunners)
    }
    return ruleRunners
}

private fun KtLintRuleEngine.initialRuleRunners() =
    ruleProviders
        .map { RuleRunner(it) }
        .distinctBy { it.ruleId }
        .toSet()

internal interface RuleFilter {
    fun filter(ruleRunners: Set<RuleRunner>): Set<RuleRunner>
}
