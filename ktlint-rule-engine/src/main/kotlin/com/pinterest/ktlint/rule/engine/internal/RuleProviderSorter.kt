package com.pinterest.ktlint.rule.engine.internal

import com.pinterest.ktlint.logger.api.initKtLintKLogger
import com.pinterest.ktlint.rule.engine.core.api.RuleInstanceProvider
import com.pinterest.ktlint.rule.engine.core.api.RuleSetId
import com.pinterest.ktlint.rule.engine.core.api.RuleV2
import io.github.oshai.kotlinlogging.KotlinLogging

private val LOGGER = KotlinLogging.logger {}.initKtLintKLogger()

/**
 * Sorts the [RuleInstanceProvider]s alphabetically on rule id. Standard rules of Ktlint are executed before custom rules.
 */
internal class RuleProviderSorter {
    /**
     * Prevent duplicate debug logging whenever the same set of [RuleV2.ruleId]s (but not the same instance) are sorted. As the sorting of
     * the [RuleInstanceProvider] has to be executed for each file.
     */
    private val debugLogCache = mutableMapOf<Int, Boolean>()

    @Synchronized
    fun getSortedRuleProviders(ruleProviders: Set<RuleInstanceProvider>): List<RuleInstanceProvider> =
        ruleProviders
            .sortedWith(defaultRuleExecutionOrderComparator())
            .also { sortedRuleProviders ->
                if (LOGGER.isDebugEnabled()) {
                    logSortedRuleProviders(sortedRuleProviders)
                }
            }

    private fun logSortedRuleProviders(sortedRuleProviders: List<RuleInstanceProvider>) {
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

    private fun createHashCode(sortedRuleProviders: List<RuleInstanceProvider>): Int =
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
        compareBy<RuleInstanceProvider> {
            if (it.ruleId.ruleSetId == RuleSetId.STANDARD) {
                0
            } else {
                1
            }
        }.thenBy { it.ruleId.value }
}
