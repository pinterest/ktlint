package com.pinterest.ktlint.rule.engine.internal

import com.pinterest.ktlint.logger.api.initKtLintKLogger
import com.pinterest.ktlint.rule.engine.core.api.Rule
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.RuleProvider
import com.pinterest.ktlint.rule.engine.core.api.RuleSetId
import com.pinterest.ktlint.rule.engine.internal.rules.KTLINT_SUPPRESSION_RULE_ID
import io.github.oshai.kotlinlogging.KotlinLogging

private val LOGGER = KotlinLogging.logger {}.initKtLintKLogger()

/**
 * Sorts the [RuleProvider]s based on [Rule.VisitorModifier]s.
 */
internal class RuleProviderSorter {
    /**
     * Prevent duplicate debug logging whenever the same set of [Rule.id]s (but not the same instance) are sorted. As
     * the sorting of the [RuleProvider] has to be executed for each file.
     */
    private val debugLogCache = mutableMapOf<Int, Boolean>()

    @Synchronized
    fun getSortedRuleProviders(ruleProviders: Set<RuleProvider>): List<RuleProvider> =
        ruleProviders
            .sort()
            .also { sortedRuleProviders ->
                if (LOGGER.isDebugEnabled()) {
                    logSortedRuleProviders(sortedRuleProviders)
                }
            }

    private fun logSortedRuleProviders(sortedRuleProviders: List<RuleProvider>) {
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

    private fun createHashCode(sortedRuleProviders: List<RuleProvider>): Int =
        sortedRuleProviders
            .joinToString(
                prefix = "rule-ids=[",
                separator = ",",
                postfix = "]",
            ) { it.ruleId.value }
            .hashCode()

    private fun Set<RuleProvider>.sort(): List<RuleProvider> {
        forEach { ruleProvider ->
            ruleProvider.hasNoVisitorModifierReferringToSelf()
        }

        val ruleIdsToBeSorted = map { it.ruleId }.toSet()
        val unprocessedRuleProviders =
            sortedWith(defaultRuleExecutionOrderComparator())
                .toMutableList()
        val sortedRuleProviders = mutableListOf<RuleProvider>()
        val ruleIdsSortedRuleProviders = mutableSetOf<RuleId>()

        // Initially the list only contains the rules not depending on another rule.
        unprocessedRuleProviders
            .filter { !it.runAsLateAsPossible && it.hasNoRunAfterRules() }
            .forEach { ruleProvider ->
                sortedRuleProviders.add(ruleProvider)
                ruleIdsSortedRuleProviders.add(ruleProvider.ruleId)
            }
        unprocessedRuleProviders.removeAll(sortedRuleProviders)
        while (unprocessedRuleProviders.isNotEmpty()) {
            val ruleProvider =
                unprocessedRuleProviders
                    .firstOrNull { ruleProvider ->
                        ruleProvider
                            .runAfterRules
                            .filter { it.ruleId in ruleIdsToBeSorted }
                            .all { it.ruleId in ruleIdsSortedRuleProviders }
                    }
                    ?: throw IllegalStateException(
                        "Can not complete sorting of rule providers as next item can not be determined.",
                    )
            sortedRuleProviders.add(ruleProvider)
            ruleIdsSortedRuleProviders.add(ruleProvider.ruleId)
            unprocessedRuleProviders.remove(ruleProvider)
        }
        return sortedRuleProviders
    }

    private fun RuleProvider.hasNoVisitorModifierReferringToSelf() {
        runAfterRules.forEach { runAfterRuleVisitorModifier ->
            check(ruleId != runAfterRuleVisitorModifier.ruleId) {
                "Rule with id '${ruleId.value}' has a visitor modifier of type '${Rule.VisitorModifier.RunAfterRule::class.simpleName}' " +
                    "which may not refer to the rule itself."
            }
        }
    }

    private fun defaultRuleExecutionOrderComparator() =
        // The sort order below should guarantee a stable order of the rule between multiple invocations of KtLint given
        // the same set of input parameters. There should be no dependency on data ordering outside this class.
        compareBy<RuleProvider> {
            if (it.ruleId == KTLINT_SUPPRESSION_RULE_ID) {
                // This rule replaces the old ktlint-disable directives with @Suppress or @SuppressWarnings annotations. It should run as
                // first rule as the SuppressionLocatorBuilder no longer transforms the ktlint-disable directives to suppressions.
                0
            } else {
                1
            }
        }.thenBy {
            if (it.runAsLateAsPossible) {
                1
            } else {
                0
            }
        }.thenBy {
            if (it.ruleId.ruleSetId == RuleSetId.STANDARD) {
                0
            } else {
                1
            }
        }.thenBy { it.ruleId.value }

    private fun RuleProvider.hasNoRunAfterRules() = runAfterRules.isEmpty()
}
