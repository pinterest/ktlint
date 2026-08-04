package io.github.ktlint.core.rule.engine.internal

import io.github.ktlint.core.logger.api.initKtLintKLogger
import io.github.ktlint.core.rule.engine.core.api.RuleSetId
import io.github.ktlint.core.rule.engine.core.api.RuleV2
import io.github.ktlint.core.rule.engine.core.api.RuleV2Provider
import io.github.oshai.kotlinlogging.KotlinLogging

private val LOGGER = KotlinLogging.logger {}.initKtLintKLogger()

/**
 * Sorts the [RuleV2Provider]s alphabetically on rule id. Standard rules of Ktlint are executed before custom rules.
 */
internal class RuleProviderSorter {
    /**
     * Prevent duplicate debug logging whenever the same set of [RuleV2.ruleId]s (but not the same instance) are sorted. As the sorting of
     * the [RuleV2Provider] has to be executed for each file.
     */
    private val debugLogCache = mutableMapOf<Int, Boolean>()

    @Synchronized
    fun getSortedRuleProviders(ruleProviders: Set<RuleV2Provider>): List<RuleV2Provider> =
        ruleProviders
            .sortedWith(defaultRuleExecutionOrderComparator())
            .also { sortedRuleProviders ->
                if (LOGGER.isDebugEnabled()) {
                    logSortedRuleProviders(sortedRuleProviders)
                }
            }

    private fun logSortedRuleProviders(sortedRuleProviders: List<RuleV2Provider>) {
        debugLogCache
            .putIfAbsent(createHashCode(sortedRuleProviders), true)
            .also { previousValue ->
                if (previousValue == null) {
                    // Logging was not printed for this combination of rule providers as no entry was found in the cache
                    sortedRuleProviders
                        .joinToString(prefix = "Rules will be executed in order below:") {
                            "\n           - ${it.ruleId.value}"
                        }.also { LOGGER.debug { it } }
                }
            }
    }

    private fun createHashCode(sortedRuleProviders: List<RuleV2Provider>): Int =
        sortedRuleProviders
            .joinToString(
                prefix = "rule-ids=[",
                separator = ",",
                postfix = "]",
            ) { it.ruleId.value }
            .hashCode()

    private fun defaultRuleExecutionOrderComparator() =
        // The sort order below should guarantee a stable order of the rule between multiple invocations of KtLint given
        // the same set of input parameters. There should be no dependency on data ordering outside this class.
        compareBy<RuleV2Provider> {
            if (it.ruleId.ruleSetId == RuleSetId.STANDARD) {
                0
            } else {
                1
            }
        }.thenBy { it.ruleId.value }
}
