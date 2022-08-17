package com.pinterest.ktlint.core.internal

import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.RuleRunner

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
    fun getSortedRuleRunners(
        ruleRunners: Set<RuleRunner>,
        debug: Boolean,
    ): List<RuleRunner> {
        val debugSorter =
            debug &&
                debugLogCache
                    .putIfAbsent(createHashCode(ruleRunners, debug), false)
                    .let { previousValue -> previousValue == null }
        return ruleRunners
            .sortedWith(defaultRuleExecutionOrderComparator())
            .applyRunAfterRuleToRuleExecutionOrder(debugSorter)
            .also { ruleReferences ->
                if (debugSorter) {
                    ruleReferences
                        .map { toQualifiedRuleId(it.ruleSetId, it.ruleId) }
                        .joinToString(prefix = "[DEBUG] Rules will be executed in order below (unless disabled):") {
                            "\n           - $it"
                        }
                        .let { println(it) }
                }
            }
    }

    private fun createHashCode(
        ruleRunners: Set<RuleRunner>,
        debug: Boolean,
    ): Int {
        val cacheKey = ruleRunners
            .map { it.qualifiedRuleId }
            .sorted()
            .joinToString(prefix = "rule-ids=[", separator = ",", postfix = "]")
            .plus(",debug=$debug")
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
            if (it.runOnRootNodeOnly) {
                0
            } else {
                1
            }
        }.thenBy {
            when (it.ruleSetId) {
                "standard" -> 0
                "experimental" -> 1
                else -> 2
            }
        }.thenBy { it.qualifiedRuleId }

    private fun List<RuleRunner>.applyRunAfterRuleToRuleExecutionOrder(debug: Boolean): List<RuleRunner> {
        // The new list of rule runners retains the order of the original list of rule runners as much as possible. Rule
        // runners will only be deferred till later in the list when needed.
        val newRuleRunners = mutableListOf<RuleRunner>()
        // Blocked rule runners can not be processed because the rule should run after another rule which is not yet
        // added to the new list of rule runners. As long a no cycle of rule runners exists which refer to each other,
        // this list will be empty at completion of this function.
        val blockedRuleRunners = mutableListOf<RuleRunner>()

        val ruleRunnersIterator = this.iterator()
        while (ruleRunnersIterator.hasNext()) {
            val currentRuleRunner = ruleRunnersIterator.next()

            val runAfterRule = currentRuleRunner.runAfterRule
            if (runAfterRule != null && newRuleRunners.none { ruleRunner -> ruleRunner.runsAfter(currentRuleRunner) }) {
                // The RunAfterRule refers to a rule which is not yet added to the new list of rule references.
                if (this.none { it.runsAfter(currentRuleRunner) }) {
                    // The RunAfterRule refers to a rule which is not loaded at all.
                    if (runAfterRule.loadOnlyWhenOtherRuleIsLoaded) {
                        println(
                            "[WARN] Skipping rule with id '${currentRuleRunner.qualifiedRuleId}' as it requires " +
                                "that the rule with id '${runAfterRule.ruleId}' is loaded. However, " +
                                "no rule with this id is loaded.",
                        )
                        continue
                    } else {
                        if (debug) {
                            println(
                                "[DEBUG] Rule with id '${currentRuleRunner.qualifiedRuleId}' should run after the " +
                                    "rule with id '${runAfterRule.ruleId}'. However, the latter " +
                                    "rule is not loaded and is allowed to be ignored. For best results, it is " +
                                    "advised load the rule.",
                            )
                        }
                        // As it is not required that the rule is loaded, the runAfter condition is ignored.
                        currentRuleRunner.clearRunAfterRule()
                        newRuleRunners.add(currentRuleRunner)
                    }
                } else {
                    // This rule can not yet be processed as it should run after another rule which is not yet added to
                    // the new list of rule references.
                    blockedRuleRunners.add(currentRuleRunner)
                    continue
                }
            } else {
                // This rule does not depend on any other rule, or it depends on rule which was already added to the new
                // list of rule references before.
                newRuleRunners.add(currentRuleRunner)
            }

            // All rule runners which were (recursively) blocked because they need to be run after the newly added rule
            // runner can now be added to the new list of rule runners as well.
            val ruleReferencesToUnblock = blockedRuleRunners.findRuleRunnersBlockedBy(currentRuleRunner.qualifiedRuleId)
            if (ruleReferencesToUnblock.isNotEmpty()) {
                newRuleRunners.addAll(ruleReferencesToUnblock)
                blockedRuleRunners.removeAll(ruleReferencesToUnblock.toSet())
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
                    "Found cyclic dependencies between rules that should run after another rule:"
                } else {
                    "Found cyclic dependencies between rules that should run after another rule. Please contact " +
                        "the maintainer(s) of the custom rule set(s) [${customRuleSetIds.joinToString()}] before " +
                        "creating an issue in the KtLint project. Dependencies:"
                }
            val separator = "\n  - "
            blockedRuleRunners.joinToString(prefix = prefix + separator, separator = separator) {
                "Rule with id '${it.qualifiedRuleId}' should run after rule with id '${it.runAfterRule?.ruleId}'"
            }
        }
        check(newRuleRunners.isNotEmpty()) {
            "No runnable rules found. Please ensure that at least one is enabled."
        }
        return newRuleRunners
    }

    private fun List<RuleRunner>.findRuleRunnersBlockedBy(qualifiedRuleId: String): List<RuleRunner> {
        return this
            .filter { it.runAfterRule?.ruleId == qualifiedRuleId }
            .map { listOf(it) + this.findRuleRunnersBlockedBy(it.qualifiedRuleId) }
            .flatten()
    }

    private fun RuleRunner.runsAfter(ruleRunner: RuleRunner) =
        ruleRunner.runAfterRule?.ruleId == qualifiedRuleId
}
