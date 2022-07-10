package com.pinterest.ktlint.core.internal

import com.pinterest.ktlint.core.KtLint
import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.api.DefaultEditorConfigProperties.disabledRulesProperty
import com.pinterest.ktlint.core.api.UsesEditorConfigProperties
import com.pinterest.ktlint.core.api.UsesEditorConfigProperties.EditorConfigProperty
import com.pinterest.ktlint.core.ast.visit
import com.pinterest.ktlint.core.initKtLintKLogger
import mu.KotlinLogging
import org.jetbrains.kotlin.com.intellij.lang.ASTNode

/**
 * The VisitorProvider is created for each file being scanned. As the [RuleSorter] logs the order in which the rules are
 * executed, a singleton instance of the class is used to prevent that the logs are flooded with duplicate log lines.
 */
private val ruleSorter = RuleSorter()

private val logger = KotlinLogging.logger {}.initKtLintKLogger()

internal class VisitorProvider(
    private val params: KtLint.ExperimentalParams,
    /**
     * Creates a new [RuleSorter]. Only to be used in unit tests where the same set of rules are used with distinct [Rule.VisitorModifier]s.
     */
    recreateRuleSorter: Boolean = false
) : UsesEditorConfigProperties {
    override val editorConfigProperties: List<EditorConfigProperty<*>> = listOf(disabledRulesProperty)

    /**
     * The [ruleReferences] is a sorted list of rules based on the [Rule.VisitorModifier] of the rules included in the
     * list.
     */
    private val ruleReferences: List<RuleReference> =
        if (recreateRuleSorter) {
            RuleSorter()
        } else {
            ruleSorter
        }.getSortedRules(params.ruleSets, params.debug)

    internal fun visitor(rootNode: ASTNode): ((node: ASTNode, rule: Rule, fqRuleId: String) -> Unit) -> Unit {
        val enabledRuleReferences =
            ruleReferences
                .filter { ruleReference -> isNotDisabled(rootNode, ruleReference.toQualifiedRuleId()) }
        val enabledQualifiedRuleIds = enabledRuleReferences.map { it.toQualifiedRuleId() }
        val enabledRules = params.ruleSets
            .flatMap { ruleSet ->
                ruleSet
                    .rules
                    .filter { rule -> toQualifiedRuleId(ruleSet.id, rule.id) in enabledQualifiedRuleIds }
                    .filter { rule -> isNotDisabled(rootNode, toQualifiedRuleId(ruleSet.id, rule.id)) }
                    .map { rule -> toQualifiedRuleId(ruleSet.id, rule.id) to rule }
            }.toMap()
        if (enabledRules.isEmpty()) {
            if (params.debug && enabledRules.isEmpty()) {
                println(
                    "[DEBUG] Skipping file as no enabled rules are found to be executed"
                )
            }
            return { _ -> }
        }
        val ruleReferencesToBeSkipped =
            ruleReferences
                .filter { ruleReference ->
                    ruleReference.runAfterRule != null &&
                        ruleReference.runAfterRule.runOnlyWhenOtherRuleIsEnabled &&
                        enabledRules[ruleReference.runAfterRule.ruleId.toQualifiedRuleId()] == null
                }
        if (params.debug && ruleReferencesToBeSkipped.isNotEmpty()) {
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
        if (ruleReferenceWithoutEntriesToBeSkipped.isEmpty()) {
            if (params.debug) {
                println(
                    "[DEBUG] Skipping file as no enabled rules are found to be executed"
                )
            }
            return { _ -> }
        }
        val rules = ruleReferences
            .mapNotNull { ruleReference ->
                enabledRules[ruleReference.toQualifiedRuleId()]
                    ?.let {
                        ShortenedQualifiedRule(ruleReference.toShortenedQualifiedRuleId(), it)
                    }
            }
        return sequentialVisitor(rules, rootNode)
    }

    private fun sequentialVisitor(
        rules: List<ShortenedQualifiedRule>,
        rootNode: ASTNode
    ): ((node: ASTNode, rule: Rule, fqRuleId: String) -> Unit) -> Unit = { visit ->
        rules.forEach {
            if (it.rule.runsOnRootNodeOnly()) {
                visit(rootNode, it.rule, it.shortenedQualifiedRuleId)
            } else {
                rootNode.visit { node -> visit(node, it.rule, it.shortenedQualifiedRuleId) }
            }
        }
    }

    private fun isNotDisabled(rootNode: ASTNode, qualifiedRuleId: String): Boolean =
        rootNode
            .getEditorConfigValue(disabledRulesProperty)
            .split(",")
            .none {
                // The rule set id in the disabled_rules setting may be omitted for rules in the standard rule set
                it.toQualifiedRuleId() == qualifiedRuleId
            }

    private fun Rule.runsOnRootNodeOnly() =
        visitorModifiers.contains(Rule.VisitorModifier.RunOnRootNodeOnly)

    private data class ShortenedQualifiedRule(
        val shortenedQualifiedRuleId: String,
        val rule: Rule
    )
}
