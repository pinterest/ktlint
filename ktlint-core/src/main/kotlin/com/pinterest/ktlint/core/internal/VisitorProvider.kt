package com.pinterest.ktlint.core.internal

import com.pinterest.ktlint.core.KtLint
import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.RuleSet
import com.pinterest.ktlint.core.ast.visit
import org.jetbrains.kotlin.com.intellij.lang.ASTNode

private val visitorProviderFactory = VisitorProviderFactory()

internal class VisitorProvider(
    ruleSets: Iterable<RuleSet>,
    private val debug: Boolean,
    /** Creates a new VisitorProviderFactory to instantiate the VisitorProvider. */
    forceNewVisitorProviderFactory: Boolean = false
) {
    private val ruleReferences: List<RuleReference> =
        if (forceNewVisitorProviderFactory) {
            VisitorProviderFactory()
        } else {
            visitorProviderFactory
        }.getRuleReferences(ruleSets, debug)

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
        val ruleReferenceWithoutEntriesToBeSkipped = enabledRuleReferences - ruleReferencesToBeSkipped
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

    private fun isNotDisabled(rootNode: ASTNode, qualifiedRuleId: String): Boolean =
        rootNode
            .getUserData(KtLint.DISABLED_RULES)
            .orEmpty()
            .none {
                // The rule set id in the disabled_rules setting may be omitted for rules in the standard rule set
                it.toQualifiedRuleId() == qualifiedRuleId
            }
}
