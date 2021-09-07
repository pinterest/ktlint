package com.pinterest.ktlint.core

import com.pinterest.ktlint.core.ast.visit
import org.jetbrains.kotlin.com.intellij.lang.ASTNode

public class VisitorProvider(
    ruleSets: Iterable<RuleSet>,
    debug: Boolean
) {
    private val ruleReferences: List<RuleReference> =
        ruleSets
            .flatMap { it.toRuleReferences() }
            .sortedWith(
                // The sort order below should guarantee a stable order of the rule between multiple invocations of
                // KtLint given the same set of input parameters. There should be no dependency on ordered data coming
                // from outside of this class.
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
            )
            .also { ruleReferences ->
                if (debug) {
                    ruleReferences
                        .joinToString(prefix = "[DEBUG] Rules will be executed in order below:") {
                            "\n           - ${it.ruleSetId}:${it.ruleId}"
                        }
                        .let { println(it) }
                }
            }

    private fun RuleSet.toRuleReferences() =
        rules.map { it.toRuleReference(id) }

    private fun Rule.toRuleReference(ruleSetId: String) =
        RuleReference(
            ruleId = id,
            ruleSetId = ruleSetId,
            runOnRootNodeOnly = toRunsOnRootNodeOnly(),
            runAsLateAsPossible = toRunsAsLateAsPossible()
        )

    private fun Rule.toRunsOnRootNodeOnly(): Boolean {
        return when (this) {
            is Rule.Modifier.RestrictToRootLast -> true
            is Rule.Modifier.RestrictToRoot -> true
            else -> false
        }
    }

    private fun Rule.toRunsAsLateAsPossible(): Boolean {
        return when (this) {
            is Rule.Modifier.Last -> true
            is Rule.Modifier.RestrictToRootLast -> true
            else -> false
        }
    }

    internal fun visitor(
        ruleSets: Iterable<RuleSet>,
        rootNode: ASTNode,
        concurrent: Boolean = true
    ): ((node: ASTNode, rule: Rule, fqRuleId: String) -> Unit) -> Unit {
        return if (concurrent) {
            concurrentVisitor(ruleSets, rootNode)
        } else {
            sequentialVisitor(ruleSets, rootNode)
        }
    }

    private fun concurrentVisitor(
        ruleSets: Iterable<RuleSet>,
        rootNode: ASTNode,
    ): ((node: ASTNode, rule: Rule, fqRuleId: String) -> Unit) -> Unit {
        val enabledRules = ruleSets
            .flatMap { ruleSet ->
                ruleSet
                    .rules
                    .filter { rule -> isNotDisabled(rootNode, ruleSet.id, rule.id) }
                    .map { rule -> "${ruleSet.id}:${rule.id}" to rule }
            }.toMap()
        return { visit ->
            rootNode.visit { node ->
                ruleReferences
                    .forEach { ruleReference ->
                        if (node == rootNode || !ruleReference.runOnRootNodeOnly) {
                            enabledRules["${ruleReference.ruleSetId}:${ruleReference.ruleId}"]
                                ?.let { rule ->
                                    visit(node, rule, ruleReference.toId())
                                }
                        }
                    }
            }
        }
    }

    private fun sequentialVisitor(
        ruleSets: Iterable<RuleSet>,
        rootNode: ASTNode,
    ): ((node: ASTNode, rule: Rule, fqRuleId: String) -> Unit) -> Unit {
        val enabledRules = ruleSets
            .flatMap { ruleSet ->
                ruleSet
                    .rules
                    .filter { rule -> isNotDisabled(rootNode, ruleSet.id, rule.id) }
                    .map { rule -> "${ruleSet.id}:${rule.id}" to rule }
            }.toMap()
        return { visit ->
            ruleReferences
                .forEach { ruleReference ->
                    enabledRules["${ruleReference.ruleSetId}:${ruleReference.ruleId}"]
                        ?.let { rule ->
                            if (ruleReference.runOnRootNodeOnly) {
                                visit(rootNode, rule, ruleReference.toId())
                            } else {
                                rootNode.visit { node -> visit(node, rule, ruleReference.toId()) }
                            }
                        }
                }
        }
    }

    private fun RuleReference.toId() =
        if (ruleSetId == "standard") {
            ruleId
        } else {
            "$ruleSetId:$ruleId"
        }

    private fun isNotDisabled(rootNode: ASTNode, ruleSetId: String, ruleId: String): Boolean {
        // The rule set id may be omitted in the disabled_rules setting
        val ruleIds = if (ruleSetId == "standard") {
            listOf(ruleId, "$ruleSetId:$ruleId")
        } else {
            listOf("$ruleSetId:$ruleId")
        }
        return rootNode
            .getUserData(KtLint.DISABLED_RULES)
            .orEmpty()
            .none { it in ruleIds }
    }
}

private data class RuleReference(
    val ruleId: String,
    val ruleSetId: String,
    val runOnRootNodeOnly: Boolean,
    val runAsLateAsPossible: Boolean
)
