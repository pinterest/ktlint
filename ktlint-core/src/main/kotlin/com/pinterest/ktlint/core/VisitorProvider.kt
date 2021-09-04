package com.pinterest.ktlint.core

import com.pinterest.ktlint.core.RuleReferenceGroup.LAST
import com.pinterest.ktlint.core.RuleReferenceGroup.NORMAL
import com.pinterest.ktlint.core.RuleReferenceGroup.RESTRICT_TO_ROOT
import com.pinterest.ktlint.core.RuleReferenceGroup.RESTRICT_TO_ROOT_LAST
import com.pinterest.ktlint.core.ast.visit
import java.lang.UnsupportedOperationException
import org.jetbrains.kotlin.com.intellij.lang.ASTNode

public class VisitorProvider(
    ruleSets: Iterable<RuleSet>,
    debug: Boolean
) {
    private val ruleReferences: List<RuleReference> =
        ruleSets
            .flatMap { ruleSet ->
                ruleSet
                    .rules
                    .map { rule ->
                        RuleReference(
                            ruleId = rule.id,
                            ruleSetId = ruleSet.id,
                            ruleReferenceGroup = rule.toRuleReferenceGroup()
                        )
                    }
            }.also {
                if (debug) {
                    println("[DEBUG]Rules will be executed in order below:")
                    println("         - Rules with Rule.Modifier.RestrictToRoot: ${it.print(RESTRICT_TO_ROOT)}")
                    println("         - Rules without Rule.Modifier: ${it.print(NORMAL)}")
                    println("         - Rules with Rule.Modifier.RestrictToRootLast: ${it.print(RESTRICT_TO_ROOT_LAST)}")
                    println("         - Rules with Rule.Modifier.Last: ${it.print(LAST)}")
                }
            }

    private fun List<RuleReference>.print(ruleReferenceGroup: RuleReferenceGroup): String =
        filter { it.ruleReferenceGroup == ruleReferenceGroup }
            .joinToString { "\n             - ${it.ruleSetId}:${it.ruleId}" }

    private fun Rule.toRuleReferenceGroup() = when (this) {
        is Rule.Modifier.Last -> LAST
        is Rule.Modifier.RestrictToRootLast -> RESTRICT_TO_ROOT_LAST
        is Rule.Modifier.RestrictToRoot -> RESTRICT_TO_ROOT
        else -> NORMAL
    }

    internal fun visitor(
        ruleSets: Iterable<RuleSet>,
        rootNode: ASTNode,
        concurrent: Boolean = true
    ): ((node: ASTNode, rule: Rule, fqRuleId: String) -> Unit) -> Unit {
        val rules = ruleSets
            .flatMap { ruleSet ->
                ruleSet
                    .rules
                    .filter { rule -> isNotDisabled(rootNode, ruleSet.id, rule.id) }
                    .map { rule -> "${ruleSet.id}:${rule.id}" to rule }
            }.toMap()
        return { visit ->
            rules.processSequential(rootNode, RESTRICT_TO_ROOT, visit)
            if (concurrent) {
                rules.processConcurrent(rootNode, NORMAL, visit)
            } else {
                rules.processSequential(rootNode, NORMAL, visit)
            }
            rules.processSequential(rootNode, RESTRICT_TO_ROOT_LAST, visit)
            if (concurrent) {
                rules.processConcurrent(rootNode, LAST, visit)
            } else {
                rules.processSequential(rootNode, LAST, visit)
            }
        }
    }

    private fun Map<String, Rule>.processSequential(
        rootNode: ASTNode,
        ruleReferenceGroup: RuleReferenceGroup,
        visit: (node: ASTNode, rule: Rule, fqRuleId: String) -> Unit
    ) =
        when (ruleReferenceGroup) {
            RESTRICT_TO_ROOT, RESTRICT_TO_ROOT_LAST ->
                filterBy(ruleReferenceGroup)
                    .forEach { (ruleId, rule) -> visit(rootNode, rule, ruleId) }
            NORMAL, LAST ->
                filterBy(ruleReferenceGroup)
                    .forEach { (ruleId, rule) ->
                        rootNode.visit { node -> visit(node, rule, ruleId) }
                    }
        }

    private fun Map<String, Rule>.processConcurrent(
        rootNode: ASTNode,
        ruleReferenceGroup: RuleReferenceGroup,
        visit: (node: ASTNode, rule: Rule, fqRuleId: String) -> Unit
    ) =
        when (ruleReferenceGroup) {
            RESTRICT_TO_ROOT, RESTRICT_TO_ROOT_LAST ->
                throw UnsupportedOperationException(
                    "Concurrent processing for reference group '$ruleReferenceGroup' is not supported"
                )
            NORMAL, LAST ->
                rootNode.visit { node ->
                    this
                        .filterBy(ruleReferenceGroup)
                        .forEach { (ruleId, rule) -> visit(node, rule, ruleId) }
                }
        }

    private fun Map<String, Rule>.filterBy(
        ruleReferenceGroup: RuleReferenceGroup
    ): List<Pair<String, Rule>> =
        ruleReferences
            .filter { it.ruleReferenceGroup == ruleReferenceGroup }
            .map { Pair(it.toId(), findByReference(it)) }
            .filter { it.second != null }
            .map { it.first to it.second!! }

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

    private fun Map<String, Rule>.findByReference(ruleReference: RuleReference): Rule? =
        this["${ruleReference.ruleSetId}:${ruleReference.ruleId}"]
}

private data class RuleReference(
    val ruleId: String,
    val ruleSetId: String,
    val ruleReferenceGroup: RuleReferenceGroup
)

private enum class RuleReferenceGroup {
    LAST,
    RESTRICT_TO_ROOT_LAST,
    RESTRICT_TO_ROOT,
    NORMAL
}
