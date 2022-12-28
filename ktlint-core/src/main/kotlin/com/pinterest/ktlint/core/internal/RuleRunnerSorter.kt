package com.pinterest.ktlint.core.internal

import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.Rule.VisitorModifier.RunAfterRule
import com.pinterest.ktlint.core.initKtLintKLogger
import com.pinterest.ktlint.core.internal.RuleRunnerSorter.RuleRunnerOrderModifier.ADD
import com.pinterest.ktlint.core.internal.RuleRunnerSorter.RuleRunnerOrderModifier.BLOCK_UNTIL_RUN_AFTER_RULE_IS_LOADED
import com.pinterest.ktlint.core.internal.RuleRunnerSorter.RuleRunnerOrderModifier.REQUIRED_RUN_AFTER_RULE_NOT_LOADED
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
    fun getSortedRuleRunners(ruleRunners: Set<RuleRunner>): List<RuleRunner> {
        return ruleRunners
            .sortedWith(defaultRuleExecutionOrderComparator())
            .applyRunAfterRuleToRuleExecutionOrder()
            .also { ruleReferences ->
                if (LOGGER.isDebugEnabled) {
                    logSortedRuleRunners(ruleRunners, ruleReferences)
                }
            }
    }

    private fun logSortedRuleRunners(
        ruleRunners: Set<RuleRunner>,
        ruleReferences: List<RuleRunner>,
    ) {
        debugLogCache
            .putIfAbsent(createHashCode(ruleRunners), true)
            .also { previousValue ->
                if (previousValue == null) {
                    // Logging was not printed for this combination of rule runners as no entry was found in the cache
                    ruleReferences
                        .map { toQualifiedRuleId(it.ruleSetId, it.ruleId) }
                        .joinToString(prefix = "Rules will be executed in order below (unless disabled):") {
                            "\n           - $it"
                        }.also { LOGGER.debug(it) }
                        .also { Unit }
                }
            }
    }

    private fun createHashCode(ruleRunners: Set<RuleRunner>): Int {
        val cacheKey = ruleRunners
            .map { it.qualifiedRuleId }
            .sorted()
            .joinToString(prefix = "rule-ids=[", separator = ",", postfix = "]")
        return cacheKey.hashCode()
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
            when (it.ruleSetId) {
                "standard" -> 0
                "experimental" -> 1
                else -> 2
            }
        }.thenBy { it.qualifiedRuleId }

    private fun List<RuleRunner>.applyRunAfterRuleToRuleExecutionOrder(): List<RuleRunner> {
        // The new list of rule runners retains the order of the original list of rule runners as much as possible. Rule
        // runners will only be deferred till later in the list when needed.
        val newRuleRunners = mutableListOf<RuleRunner>()
        // Blocked rule runners can not be processed because the rule should run after another rule which is not yet
        // added to the new list of rule runners. As long a no cycle of rule runners exists which refer to each other,
        // this list will be empty at completion of this function.
        val blockedRuleRunners = mutableSetOf<RuleRunner>()
        // The list of rule ids which are required to be loaded but are missing. Such rules are referenced in a
        // RunAfterRule visitor modifier and are marked as required to be loaded.
        val requiredButMissingRuleIds = mutableSetOf<RunAfterRuleRequiredButNotLoaded>()

        var newRuleRunnersAdded = false
        var ruleRunnersIterator: Iterator<RuleRunner>
        do {
            if (newRuleRunnersAdded) {
                newRuleRunnersAdded = false
                // All rule runners which were (previously) blocked can now be checked again
                ruleRunnersIterator =
                    blockedRuleRunners
                        .canRunWith(newRuleRunners)
                        .toSet()
                        .iterator()
                blockedRuleRunners.clear()
            } else {
                ruleRunnersIterator = this.iterator()
            }
            while (ruleRunnersIterator.hasNext()) {
                val currentRuleRunner = ruleRunnersIterator.next()

                val runAfterRulesToBeRemoved = mutableSetOf<RunAfterRule>()
                val ruleRunnerOrderModifier =
                    currentRuleRunner
                        .runAfterRules
                        .map { runAfterRule ->
                            when {
                                runAfterRule.ruleId in newRuleRunners.map { it.qualifiedRuleId } -> {
                                    ADD
                                }

                                runAfterRule.ruleId in this.map { it.qualifiedRuleId } -> {
                                    BLOCK_UNTIL_RUN_AFTER_RULE_IS_LOADED
                                }

                                runAfterRule.loadOnlyWhenOtherRuleIsLoaded -> {
                                    // The runAfterRule depends on a rule which will not be loaded. As the rule on which the
                                    // current rule depends is required, the current rule has to be skipped.
                                    LOGGER.warn {
                                        "Skipping rule with id '${currentRuleRunner.qualifiedRuleId}' as it requires " +
                                            "that the rule with id '${runAfterRule.ruleId}' is loaded. However, " +
                                            "no rule with this id is loaded."
                                    }
                                    requiredButMissingRuleIds.add(
                                        RunAfterRuleRequiredButNotLoaded(
                                            currentRuleRunner.qualifiedRuleId,
                                            runAfterRule.ruleId,
                                        ),
                                    )
                                    REQUIRED_RUN_AFTER_RULE_NOT_LOADED
                                }

                                else -> {
                                    LOGGER.debug {
                                        "Rule with id '${currentRuleRunner.qualifiedRuleId}' should run after the " +
                                            "rule with id '${runAfterRule.ruleId}'. However, the latter " +
                                            "rule is not loaded and is allowed to be ignored. For best results, it is " +
                                            "advised load the rule."
                                    }
                                    runAfterRulesToBeRemoved.add(runAfterRule)
                                    ADD
                                }
                            }
                        }.maxByOrNull { it.severity }
                        ?: ADD
                if (runAfterRulesToBeRemoved.isNotEmpty()) {
                    currentRuleRunner.removeRunAfterRules(runAfterRulesToBeRemoved)
                }
                when (ruleRunnerOrderModifier) {
                    ADD -> {
                        // This rule does not depend on any other rule which will be loaded, or it depends on rule which was
                        // already added to the new list of rule which will be loaded before the current rule.
                        newRuleRunners.add(currentRuleRunner)
                        newRuleRunnersAdded = true
                    }

                    BLOCK_UNTIL_RUN_AFTER_RULE_IS_LOADED -> {
                        // The rule depends on another rule which will be loaded but currently is not yet added to the list
                        // of rules which are loaded before the current rule.
                        blockedRuleRunners.add(currentRuleRunner)
                        continue
                    }

                    REQUIRED_RUN_AFTER_RULE_NOT_LOADED -> {
                        // The rule depends on a rule which will not be loaded
                        continue
                    }
                }
            }
        } while (newRuleRunnersAdded)
        check(requiredButMissingRuleIds.isEmpty()) {
            val separator = "\n  - "
            requiredButMissingRuleIds
                .joinToString(
                    prefix = "Skipping rule(s) which are depending on a rule which is not loaded. Please check if " +
                        "you need to additional rule sets before creating an issue.$separator",
                    separator = separator,
                ) {
                    "Rule with id '${it.ruleId}' requires rule with id '${it.runAfterRuleId}' to be loaded"
                }
        }
        check(blockedRuleRunners.isEmpty()) {
            val customRuleSetIds =
                blockedRuleRunners
                    .map { it.ruleSetId }
                    .filterNot { it == "standard" || it == "experimental" }
                    .distinct()
                    .sorted()
            val prefix =
                if (customRuleSetIds.isEmpty()) {
                    "Found cyclic dependencies between required rules that should run after another rule:"
                } else {
                    "Found cyclic dependencies between required rules that should run after another rule. Please contact " +
                        "the maintainer(s) of the custom rule set(s) [${customRuleSetIds.joinToString()}] before " +
                        "creating an issue in the KtLint project. Dependencies:"
                }
            val separator = "\n  - "
            blockedRuleRunners.joinToString(prefix = prefix + separator, separator = separator) {
                "Rule with id '${it.qualifiedRuleId}' should run after rule(s) with id '${it.runAfterRules.joinToString(separator = ", ") { it.ruleId } }'"
            }
        }
        check(newRuleRunners.isNotEmpty()) {
            "No runnable rules found. Please ensure that at least one is enabled."
        }
        return newRuleRunners
    }

    private fun Set<RuleRunner>.canRunWith(loadedRuleRunners: List<RuleRunner>): List<RuleRunner> =
        canRunWithRuleIds(loadedRuleRunners.map { it.qualifiedRuleId })

    private fun Set<RuleRunner>.canRunWithRuleIds(loadedRuleIds: List<String>): List<RuleRunner> {
        return this
            .filter { it.canRunWith(loadedRuleIds) }
            .let { unblockedRuleRunners ->
                if (unblockedRuleRunners.isEmpty()) {
                    unblockedRuleRunners
                } else {
                    val unblockedRuleIds = unblockedRuleRunners.map { it.qualifiedRuleId }
                    this
                        .filter { it.qualifiedRuleId !in unblockedRuleIds }
                        .toSet()
                        .canRunWithRuleIds(loadedRuleIds.plus(unblockedRuleIds))
                        .plus(unblockedRuleRunners)
                }
            }
    }

    private fun RuleRunner.canRunWith(loadedRuleIds: List<String>): Boolean =
        this.runAfterRules.all { it.ruleId in loadedRuleIds }

    private data class RunAfterRuleRequiredButNotLoaded(val ruleId: String, val runAfterRuleId: String)

    private enum class RuleRunnerOrderModifier(val severity: Int) {
        /**
         *  This rule does not depend on any other rule which will be loaded, or it depends on rule which was already
         *  added to the new list of rule which will be loaded before the current rule.
         */
        ADD(0),

        /**
         * The runAfterRule depends on a rule which will be loaded but currently is not yet added to the list which is
         * loaded before the current rule.
         * */
        BLOCK_UNTIL_RUN_AFTER_RULE_IS_LOADED(1),

        /**
         * The rule depends on a rule which will not be loaded.
         */
        REQUIRED_RUN_AFTER_RULE_NOT_LOADED(2),
    }
}
