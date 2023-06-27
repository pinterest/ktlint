package com.pinterest.ktlint.rule.engine.internal.rulefilter

import com.pinterest.ktlint.rule.engine.api.KtLintRuleEngine
import com.pinterest.ktlint.rule.engine.core.api.RuleProvider

/**
 * Gets the rule provider for the [KtLintRuleEngine] by applying the [ruleFilters] in the given order on the set of [RuleProvider]s provided
 * by the previous (or the initial list of [RuleProvider]s).
 */
internal fun KtLintRuleEngine.applyRuleFilters(vararg ruleFilters: RuleFilter): Set<RuleProvider> {
    var ruleProviders = initialRuleProviders()
    val ruleFilterIterator = ruleFilters.iterator()
    while (ruleFilterIterator.hasNext()) {
        val ruleFilter = ruleFilterIterator.next()
        ruleProviders = ruleFilter.filter(ruleProviders)
    }
    return ruleProviders
}

private fun KtLintRuleEngine.initialRuleProviders() =
    ruleProviders
        .distinctBy { it.ruleId }
        .toSet()

internal interface RuleFilter {
    fun filter(ruleProviders: Set<RuleProvider>): Set<RuleProvider>
}
