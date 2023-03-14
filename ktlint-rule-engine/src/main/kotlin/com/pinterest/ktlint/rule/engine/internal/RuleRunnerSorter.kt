package com.pinterest.ktlint.rule.engine.internal

import com.pinterest.ktlint.logger.api.initKtLintKLogger
import com.pinterest.ktlint.rule.engine.core.api.Rule
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.RuleSetId
import mu.KotlinLogging

private val LOGGER = KotlinLogging.logger {}.initKtLintKLogger()

/**
 * Sorts the [RuleRunner]s based on [Rule.VisitorModifier]s.
 */
internal class RuleRunnerSorter {
    /**
     * Prevent duplicate debug logging whenever the same set of [Rule.id]s (but not the same instance) are sorted. As
     * the sorting of the [RuleRunner] has to be executed for each file.
     */
    private val debugLogCache = mutableMapOf<Int, Boolean>()

    @Synchronized
    fun getSortedRuleRunners(ruleRunners: Set<RuleRunner>): List<RuleRunner> =
        ruleRunners
            .sort()
            .also { sortedRuleRunners ->
                if (LOGGER.isDebugEnabled) {
                    logSortedRuleRunners(sortedRuleRunners)
                }
            }

    private fun logSortedRuleRunners(sortedRuleRunners: List<RuleRunner>) {
        debugLogCache
            .putIfAbsent(createHashCode(sortedRuleRunners), true)
            .also { previousValue ->
                if (previousValue == null) {
                    // Logging was not printed for this combination of rule runners as no entry was found in the cache
                    sortedRuleRunners
                        .joinToString(prefix = "Rules will be executed in order below:") {
                            "\n           - ${it.ruleId}"
                        }.also { LOGGER.debug(it) }
                }
            }
    }

    private fun createHashCode(sortedRuleRunners: List<RuleRunner>): Int =
        sortedRuleRunners
            .joinToString(
                prefix = "rule-ids=[",
                separator = ",",
                postfix = "]",
            ) { it.ruleId.value }
            .hashCode()

    private fun Set<RuleRunner>.sort(): List<RuleRunner> {
        val ruleIdsToBeSorted = map { it.ruleId }.toSet()
        val unprocessedRuleRunners =
            sortedWith(defaultRuleExecutionOrderComparator())
                .toMutableList()
        val sortedRuleRunners = mutableListOf<RuleRunner>()
        val ruleIdsSortedRuleRunners = mutableSetOf<RuleId>()

        // Initially the list only contains the rules which have no RunAfterRules (e.g. are not depending on another rule).
        unprocessedRuleRunners
            .filter { it.hasNoRunAfterRules() }
            .forEach { ruleRunner ->
                sortedRuleRunners.add(ruleRunner)
                ruleIdsSortedRuleRunners.add(ruleRunner.ruleId)
            }
        unprocessedRuleRunners.removeAll(sortedRuleRunners)
        while (unprocessedRuleRunners.isNotEmpty()) {
            val ruleRunner = unprocessedRuleRunners
                .firstOrNull { ruleRunner ->
                    ruleRunner
                        .runAfterRules
                        .filter { it.ruleId in ruleIdsToBeSorted }
                        .all { it.ruleId in ruleIdsSortedRuleRunners }
                }
                ?: throw IllegalStateException(
                    "Can not complete sorting of rule runners as next item can not be determined.",
                )
            sortedRuleRunners.add(ruleRunner)
            ruleIdsSortedRuleRunners.add(ruleRunner.ruleId)
            unprocessedRuleRunners.remove(ruleRunner)
        }
        return sortedRuleRunners
    }

    private fun defaultRuleExecutionOrderComparator() =
        // The sort order below should guarantee a stable order of the rule between multiple invocations of KtLint given
        // the same set of input parameters. There should be no dependency on data ordering outside this class.
        compareBy<RuleRunner> {
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

    private fun RuleRunner.hasNoRunAfterRules() = runAfterRules.isEmpty()
}
