package com.pinterest.ktlint.core

import com.pinterest.ktlint.core.internal.IdNamingPolicy
import org.jetbrains.kotlin.com.intellij.lang.ASTNode

/**
 * A rule contract.
 *
 * Implementation **doesn't** have to be thread-safe or stateless
 * (provided [RuleSetProvider] creates a new instance on each `get()` call).
 *
 * @param id: For non-standard rules, it is expected that this id consist of the ruleSetId and ruleId, e.g. "some-rule-set-id:some-rule-id"
 * @param visitorModifiers: set of modifiers of the visitor. Preferably a rule has no modifiers at all, meaning that it
 * is completely independent of all other rules.
 *
 * @see RuleSet
 */
abstract class Rule(
    val id: String,
    public val visitorModifiers: Set<VisitorModifier> = emptySet()
) {
    init {
        IdNamingPolicy.enforceRuleIdNaming(id)
    }

    /**
     * This method is going to be executed for each node in AST (in DFS fashion).
     *
     * @param node AST node
     * @param autoCorrect indicates whether rule should attempt auto-correction
     * @param emit a way for rule to notify about a violation (lint error)
     */
    abstract fun visit(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit
    )

    sealed class VisitorModifier {

        data class RunAfterRule(
            /**
             * Qualified ruleId in format "ruleSetId:ruleId". For a rule in the standard rule set it suffices to specify
             * the ruleId only.
             */
            val ruleId: String,
            /**
             * The annotated rule will only be loaded in case the other rule is loaded as well.
             */
            val loadOnlyWhenOtherRuleIsLoaded: Boolean = false,
            /**
             * The annotated rule will only be run in case the other rule is enabled.
             */
            val runOnlyWhenOtherRuleIsEnabled: Boolean = false
        ) : VisitorModifier()

        object RunAsLateAsPossible : VisitorModifier()

        object RunOnRootNodeOnly : VisitorModifier()
    }
}
