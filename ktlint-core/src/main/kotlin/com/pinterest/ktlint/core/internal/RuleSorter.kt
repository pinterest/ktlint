package com.pinterest.ktlint.core.internal

import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.RuleSet
import kotlin.reflect.KClass

/**
 * Normally, the [RuleSorter] should be used as a singleton as it logs the order in which the rules are executed. Only
 * in specific unit tests scenario's it might be needed to recreate the class when running unit tests for the same set
 * of rule ids but having different [Rule.VisitorModifier]s.
 */
internal class RuleSorter {
    private val ruleSetsToRuleReferencesMap = mutableMapOf<Int, List<RuleReference>>()

    @Synchronized
    fun getSortedRules(
        ruleSets: Iterable<RuleSet>,
        debug: Boolean
    ): List<RuleReference> {
        val id = ruleSets
            .asSequence()
            .map { ruleSet ->
                ruleSet
                    .rules
                    .map { rule ->
                        toQualifiedRuleId(ruleSet.id, rule.id)
                    }
            }.flatten()
            .sorted()
            .joinToString(prefix = "rule-ids=[", separator = ",", postfix = "]")
            .plus(",debug=$debug")
        val ruleSetsHashCode = id.hashCode()
        return ruleSetsToRuleReferencesMap
            // If the VisitorProvider has been created before for the same list of RuleSets than reuse the list of
            // ruleReferences as computation is relative expensive but more important because it generates as lot of
            // logging. Only during unit tests it is normal behavior to have a frequent changing list of rule sets.
            .getOrPut(ruleSetsHashCode) {
                ruleSets
                    .flatMap { it.toRuleReferences() }
                    .sortedWith(defaultRuleExecutionOrderComparator())
                    .applyRunAfterRuleToRuleExecutionOrder(debug)
                    .also { ruleReferences ->
                        if (debug) {
                            ruleReferences
                                .map { toQualifiedRuleId(it.ruleSetId, it.ruleId) }
                                .joinToString(prefix = "[DEBUG] Rules will be executed in order below (unless disabled):") {
                                    "\n           - $it"
                                }
                                .let { println(it) }
                        }
                    }
            }
    }

    private fun RuleSet.toRuleReferences() =
        rules.map { it.toRuleReference(id) }

    private fun Rule.toRuleReference(ruleSetId: String) =
        RuleReference(
            ruleId = id,
            ruleSetId = ruleSetId,
            runOnRootNodeOnly = toRunsOnRootNodeOnly(ruleSetId),
            runAsLateAsPossible = toRunsAsLateAsPossible(ruleSetId),
            runAfterRule = toRunAfter(ruleSetId)
        )

    private fun Rule.toRunsOnRootNodeOnly(ruleSetId: String): Boolean {
        if (visitorModifiers.contains(Rule.VisitorModifier.RunOnRootNodeOnly)) {
            return true
        }

        return when (this) {
            is Rule.Modifier.RestrictToRootLast -> {
                printWarningDeprecatedInterface(ruleSetId, Rule.Modifier.RestrictToRootLast::class)
                true
            }
            is Rule.Modifier.RestrictToRoot -> {
                printWarningDeprecatedInterface(ruleSetId, Rule.Modifier.RestrictToRoot::class)
                true
            }
            else -> false
        }
    }

    private fun Rule.toRunsAsLateAsPossible(ruleSetId: String): Boolean {
        if (visitorModifiers.contains(Rule.VisitorModifier.RunAsLateAsPossible)) {
            return true
        }

        return when (this) {
            is Rule.Modifier.Last -> {
                printWarningDeprecatedInterface(ruleSetId, Rule.Modifier.Last::class)
                true
            }
            is Rule.Modifier.RestrictToRootLast -> {
                printWarningDeprecatedInterface(ruleSetId, Rule.Modifier.RestrictToRootLast::class)
                true
            }
            else -> false
        }
    }

    private fun Rule.toRunAfter(ruleSetId: String): Rule.VisitorModifier.RunAfterRule? =
        this
            .visitorModifiers
            .find { it is Rule.VisitorModifier.RunAfterRule }
            ?.let {
                val runAfterRuleVisitorModifier = it as Rule.VisitorModifier.RunAfterRule
                val qualifiedRuleId = toQualifiedRuleId(ruleSetId, this.id)
                val qualifiedAfterRuleId = runAfterRuleVisitorModifier.ruleId.toQualifiedRuleId()
                check(qualifiedRuleId != qualifiedAfterRuleId) {
                    "Rule with id '$qualifiedRuleId' has a visitor modifier of type " +
                        "'${Rule.VisitorModifier.RunAfterRule::class.simpleName}' but it is not referring to another " +
                        "rule but to the rule itself. A rule can not run after itself. This should be fixed by the " +
                        "maintainer of the rule."
                }
                runAfterRuleVisitorModifier.copy(
                    ruleId = qualifiedAfterRuleId
                )
            }

    private fun Rule.printWarningDeprecatedInterface(
        ruleSetId: String,
        kClass: KClass<*>
    ) {
        println(
            "[WARN] Rule with id '${toQualifiedRuleId(ruleSetId, id)}' is marked with interface '${kClass.qualifiedName}'. This interface " +
                "is deprecated and marked for deletion in a future version of ktlint. Contact the maintainer of the " +
                "ruleset '$ruleSetId' to fix this rule."
        )
    }

    private fun defaultRuleExecutionOrderComparator() =
        // The sort order below should guarantee a stable order of the rule between multiple invocations of KtLint given
        // the same set of input parameters. There should be no dependency on data ordering outside this class.
        compareBy<RuleReference> {
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
        }.thenBy { it.ruleId }

    private fun List<RuleReference>.applyRunAfterRuleToRuleExecutionOrder(debug: Boolean): List<RuleReference> {
        // The new list of rule references retains the order of the original list of rule references as much as
        // possible. Rule references will only be deferred till later in the list when needed.
        val newRuleReferences = mutableListOf<RuleReference>()
        // Blocked rule references can not be processed because the rule should run after another rule which is not yet
        // added to the new list of rule references. As long a no cycle of rules exists which refer to each other, this
        // list will be empty at completion of this function.
        val blockedRuleReferences = mutableListOf<RuleReference>()

        val ruleReferencesIterator = this.iterator()
        while (ruleReferencesIterator.hasNext()) {
            val ruleReference = ruleReferencesIterator.next()

            if (ruleReference.runAfterRule != null && newRuleReferences.none { rule -> rule.runsAfter(ruleReference) }) {
                // The RunAfterRule refers to a rule which is not yet added to the new list of rule references.
                if (this.none { it.runsAfter(ruleReference) }) {
                    // The RunAfterRule refers to a rule which is not loaded at all.
                    if (ruleReference.runAfterRule.loadOnlyWhenOtherRuleIsLoaded) {
                        println(
                            "[WARN] Skipping rule with id '${ruleReference.toQualifiedRuleId()}' as it requires " +
                                "that the rule with id '${ruleReference.runAfterRule.ruleId}' is loaded. However, no " +
                                "rule with this id is loaded."
                        )
                        continue
                    } else {
                        if (debug) {
                            println(
                                "[DEBUG] Rule with id '${ruleReference.toQualifiedRuleId()}' should run after the " +
                                    "rule with id '${ruleReference.runAfterRule.ruleId}'. However, the latter rule is " +
                                    "not loaded and is allowed to be ignored. For best results, it is advised load " +
                                    "the rule."
                            )
                        }
                        // As it is not required that the rule is loaded, the runAfter condition is ignored.
                        newRuleReferences.add(ruleReference.copy(runAfterRule = null))
                    }
                } else {
                    // This rule can not yet be processed as it should run after another rule which is not yet added to
                    // the new list of rule references.
                    blockedRuleReferences.add(ruleReference)
                    continue
                }
            } else {
                // This rule does not depend on any other rule, or it depends on rule which was already added to the new
                // list of rule references before.
                newRuleReferences.add(ruleReference)
            }

            // All rules which were (recursively) blocked because they need to be run after the newly added rule can now
            // be added to the new list of rule references as well.
            val ruleReferencesToUnblock = blockedRuleReferences.findRulesBlockedBy(ruleReference.toQualifiedRuleId())
            if (ruleReferencesToUnblock.isNotEmpty()) {
                newRuleReferences.addAll(ruleReferencesToUnblock)
                blockedRuleReferences.removeAll(ruleReferencesToUnblock)
            }
        }
        check(blockedRuleReferences.isEmpty()) {
            val customRuleSetIds =
                blockedRuleReferences
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
            blockedRuleReferences.joinToString(prefix = prefix + separator, separator = separator) {
                "Rule with id '${it.toQualifiedRuleId()}' should run after rule with id '${it.runAfterRule?.ruleId}'"
            }
        }
        check(newRuleReferences.isNotEmpty()) {
            "No runnable rules found. Please ensure that at least one is enabled."
        }
        return newRuleReferences
    }

    private fun List<RuleReference>.findRulesBlockedBy(qualifiedRuleId: String): List<RuleReference> {
        return this
            .filter { it.runAfterRule?.ruleId == qualifiedRuleId }
            .map { listOf(it) + this.findRulesBlockedBy(it.toQualifiedRuleId()) }
            .flatten()
    }

    private fun RuleReference.runsAfter(ruleReference: RuleReference) =
        ruleReference.runAfterRule?.ruleId == toQualifiedRuleId()
}
