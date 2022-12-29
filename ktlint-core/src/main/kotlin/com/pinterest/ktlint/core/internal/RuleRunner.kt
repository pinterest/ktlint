package com.pinterest.ktlint.core.internal

import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.Rule.VisitorModifier.RunAfterRule
import com.pinterest.ktlint.core.RuleProvider

// TODO: Rename to RuleInstanceProvider. Also rename RuleRunnerSorter.
internal class RuleRunner(private val provider: RuleProvider) {
    private var rule = provider.createNewRuleInstance()

    val qualifiedRuleId = rule.toQualifiedRuleId()
    val shortenedQualifiedRuleId = qualifiedRuleId.removePrefix("standard:")

    val ruleId = rule.id
    val ruleSetId = qualifiedRuleId.substringBefore(':')

    val runAsLateAsPossible = rule.visitorModifiers.contains(Rule.VisitorModifier.RunAsLateAsPossible)
    var runAfterRules = setRunAfterRules()

    /**
     * Gets the [Rule]. If the [Rule] has already been used for traversal of the AST, a new instance of the [Rule] is
     * provided. This prevents leakage of the state of the Rule between executions.
     */
    fun getRule(): Rule {
        if (rule.isUsedForTraversalOfAST()) {
            rule = provider.createNewRuleInstance()
        }
        return rule
    }

    private fun setRunAfterRules(): List<RunAfterRule> =
        rule
            .visitorModifiers
            .filterIsInstance<RunAfterRule>()
            .map {
                val runAfterRuleVisitorModifier = it as RunAfterRule
                val qualifiedAfterRuleId = runAfterRuleVisitorModifier.ruleId.toQualifiedRuleId()
                check(qualifiedRuleId != qualifiedAfterRuleId) {
                    // Do not print the fully qualified rule id in the error message as it might not appear in the code
                    // in case it is a rule from the 'standard' rule set.
                    "Rule with id '${rule.id}' has a visitor modifier of type '${RunAfterRule::class.simpleName}' " +
                        "but it is not referring to another rule but to the rule itself. A rule can not run after " +
                        "itself. This should be fixed by the maintainer of the rule."
                }
                runAfterRuleVisitorModifier.copy(
                    ruleId = qualifiedAfterRuleId,
                )
            }

    fun removeRunAfterRules(runAfterRules: Set<RunAfterRule>) {
        require(!rule.isUsedForTraversalOfAST()) {
            "RunAfterRules can not be removed when RuleRunner has already been used for traversal of the AST"
        }
        this.runAfterRules = this.runAfterRules - runAfterRules
    }
}
