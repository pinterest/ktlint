package com.pinterest.ktlint.rule.engine.internal

import com.pinterest.ktlint.rule.engine.core.api.FeatureInAlphaState
import com.pinterest.ktlint.rule.engine.core.api.Rule
import com.pinterest.ktlint.rule.engine.core.api.Rule.VisitorModifier.RunAfterRule
import com.pinterest.ktlint.rule.engine.core.api.Rule.VisitorModifier.RunAsLateAsPossible
import com.pinterest.ktlint.rule.engine.core.api.RuleProvider

// TODO: Rename to RuleInstanceProvider. Also rename RuleRunnerSorter.
internal class RuleRunner(private val provider: RuleProvider) {
    private var rule = provider.createNewRuleInstance()

    val ruleId = rule.ruleId

    // TODO: refactor
    val ruleSetId = rule.ruleId.ruleSetId.value

    val runAsLateAsPossible = rule.visitorModifiers.contains(RunAsLateAsPossible)
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

    @OptIn(FeatureInAlphaState::class)
    private fun setRunAfterRules(): List<RunAfterRule> =
        rule
            .visitorModifiers
            .filterIsInstance<RunAfterRule>()
            .map { runAfterRuleVisitorModifier ->
                check(ruleId != runAfterRuleVisitorModifier.ruleId) {
                    // Do not print the fully qualified rule id in the error message as it might not appear in the code
                    // in case it is a rule from the 'standard' rule set.
                    "Rule with id '${ruleId.value}' has a visitor modifier of type '${RunAfterRule::class.simpleName}' " +
                        "but it is not referring to another rule but to the rule itself. A rule can not run after " +
                        "itself. This should be fixed by the maintainer of the rule."
                }
                runAfterRuleVisitorModifier.copy(
                    ruleId = runAfterRuleVisitorModifier.ruleId,
                )
            }

    fun removeRunAfterRules(runAfterRules: Set<RunAfterRule>) {
        require(!rule.isUsedForTraversalOfAST()) {
            "RunAfterRules can not be removed when RuleRunner has already been used for traversal of the AST"
        }
        this.runAfterRules = this.runAfterRules - runAfterRules
    }
}
