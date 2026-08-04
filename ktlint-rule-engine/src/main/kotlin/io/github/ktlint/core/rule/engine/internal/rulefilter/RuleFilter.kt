package io.github.ktlint.core.rule.engine.internal.rulefilter

import io.github.ktlint.core.rule.engine.api.KtLintRuleEngine
import io.github.ktlint.core.rule.engine.core.api.RuleV2Provider

/**
 * Gets the rule providers for the [KtLintRuleEngine] by applying the [ruleFilters] in the given order on the set of [RuleV2Provider]s
 * provided by the previous (or the initial list of [RuleV2Provider]s).
 */
internal fun KtLintRuleEngine.applyRuleFilters(vararg ruleFilters: RuleFilter): Set<RuleV2Provider> {
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
    fun filter(ruleProviders: Set<RuleV2Provider>): Set<RuleV2Provider>
}
