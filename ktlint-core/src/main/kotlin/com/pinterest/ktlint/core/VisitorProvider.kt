package com.pinterest.ktlint.core

import com.pinterest.ktlint.core.ast.visit
import kotlin.reflect.KClass
import org.jetbrains.kotlin.com.intellij.lang.ASTNode

public class VisitorProvider(
    ruleSets: Iterable<RuleSet>,
    private val debug: Boolean
) {
    private val ruleReferences: List<RuleReference> =
        VisitorProviderInitializer(ruleSets, debug).getRulePreferences()

    internal fun visitor(
        ruleSets: Iterable<RuleSet>,
        rootNode: ASTNode,
        concurrent: Boolean = true
    ): ((node: ASTNode, rule: Rule, fqRuleId: String) -> Unit) -> Unit {
        val enabledRuleReferences =
            ruleReferences
                .filter { ruleReference -> isNotDisabled(rootNode, ruleReference.toQualifiedRuleId()) }
        val enabledQualifiedRuleIds = enabledRuleReferences.map { it.toQualifiedRuleId() }
        val enabledRules = ruleSets
            .flatMap { ruleSet ->
                ruleSet
                    .rules
                    .filter { rule -> toQualifiedRuleId(ruleSet.id, rule.id) in enabledQualifiedRuleIds }
                    .filter { rule -> isNotDisabled(rootNode, toQualifiedRuleId(ruleSet.id, rule.id)) }
                    .map { rule -> toQualifiedRuleId(ruleSet.id, rule.id) to rule }
            }.toMap()
        if (debug && enabledRules.isEmpty()) {
            println(
                "[DEBUG] Skipping file as no enabled rules are found to be executed"
            )
            return { _ -> }
        }
        val ruleReferencesToBeSkipped =
            ruleReferences
                .filter { ruleReference ->
                    ruleReference.runAfterRule != null &&
                        ruleReference.runAfterRule.runOnlyWhenOtherRuleIsEnabled &&
                        enabledRules[ruleReference.runAfterRule.ruleId.toQualifiedRuleId()] == null
                }
        if (debug && ruleReferencesToBeSkipped.isNotEmpty()) {
            ruleReferencesToBeSkipped
                .forEach {
                    println(
                        "[DEBUG] Skipping rule with id '${it.toQualifiedRuleId()}'. This rule has to run after rule with " +
                            "id '${it.runAfterRule?.ruleId?.toQualifiedRuleId()}' and will not run in case that rule is " +
                            "disabled."
                    )
                }
        }
        val ruleReferenceWithoutEntriesToBeSkipped = enabledRuleReferences - ruleReferencesToBeSkipped.toSet()
        if (debug && ruleReferenceWithoutEntriesToBeSkipped.isEmpty()) {
            println(
                "[DEBUG] Skipping file as no enabled rules are found to be executed"
            )
            return { _ -> }
        }
        return if (concurrent) {
            concurrentVisitor(enabledRules, ruleReferenceWithoutEntriesToBeSkipped, rootNode)
        } else {
            sequentialVisitor(enabledRules, ruleReferenceWithoutEntriesToBeSkipped, rootNode)
        }
    }

    private fun concurrentVisitor(
        enabledRules: Map<String, Rule>,
        ruleReferences: List<RuleReference>,
        rootNode: ASTNode
    ): ((node: ASTNode, rule: Rule, fqRuleId: String) -> Unit) -> Unit {
        return { visit ->
            rootNode.visit { node ->
                ruleReferences
                    .forEach { ruleReference ->
                        if (node == rootNode || !ruleReference.runOnRootNodeOnly) {
                            enabledRules[ruleReference.toQualifiedRuleId()]
                                ?.let { rule ->
                                    visit(node, rule, ruleReference.toShortenedQualifiedRuleId())
                                }
                        }
                    }
            }
        }
    }

    private fun sequentialVisitor(
        enabledRules: Map<String, Rule>,
        ruleReferences: List<RuleReference>,
        rootNode: ASTNode
    ): ((node: ASTNode, rule: Rule, fqRuleId: String) -> Unit) -> Unit {
        return { visit ->
            ruleReferences
                .forEach { ruleReference ->
                    enabledRules[ruleReference.toQualifiedRuleId()]
                        ?.let { rule ->
                            if (ruleReference.runOnRootNodeOnly) {
                                visit(rootNode, rule, ruleReference.toShortenedQualifiedRuleId())
                            } else {
                                rootNode.visit { node -> visit(node, rule, ruleReference.toShortenedQualifiedRuleId()) }
                            }
                        }
                }
        }
    }

    private fun RuleReference.toShortenedQualifiedRuleId() =
        if (ruleSetId == "standard") {
            ruleId
        } else {
            toQualifiedRuleId()
        }

    private fun isNotDisabled(rootNode: ASTNode, qualifiedRuleId: String): Boolean =
        rootNode
            .getUserData(KtLint.DISABLED_RULES)
            .orEmpty()
            .none {
                // The rule set id in the disabled_rules setting may be omitted for rules in the standard rule set
                it.toQualifiedRuleId() == qualifiedRuleId
            }
}

private fun RuleReference.toQualifiedRuleId() =
    toQualifiedRuleId(ruleSetId, ruleId)

private fun toQualifiedRuleId(
    ruleSetId: String,
    ruleId: String
) =
    if (ruleId.startsWith("$ruleSetId:")) {
        ruleId
    } else {
        "$ruleSetId:$ruleId"
    }

private fun String.toQualifiedRuleId() =
    if (contains(":")) {
        this
    } else {
        "standard:$this"
    }

private class VisitorProviderInitializer(
    val ruleSets: Iterable<RuleSet>,
    val debug: Boolean
) {
    fun getRulePreferences(): List<RuleReference> {
        return ruleSets
            .flatMap { it.toRuleReferences() }
            .sortedWith(defaultRuleExecutionOrderComparator())
            .applyRunAfterRuleToRuleExecutionOrder()
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

    private fun List<RuleReference>.applyRunAfterRuleToRuleExecutionOrder(): List<RuleReference> {
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
                blockedRuleReferences.removeAll(ruleReferencesToUnblock.toSet())
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

private data class RuleReference(
    val ruleId: String,
    val ruleSetId: String,
    val runOnRootNodeOnly: Boolean,
    val runAsLateAsPossible: Boolean,
    val runAfterRule: Rule.VisitorModifier.RunAfterRule?
)

private data class RunAfter(
    val ruleId: String,
    val loadOnlyWhenOtherRuleIsLoaded: Boolean,
    val runOnlyWhenOtherRuleIsEnabled: Boolean
)
