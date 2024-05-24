package com.pinterest.ktlint.rule.engine.core.api

import org.jetbrains.kotlin.com.intellij.lang.ASTNode

/**
 * In Ktlint 2.0 the methods from this interface will be merged into the [Rule] class. Consider to implement this interface on your Ktlint
 * 1.x compatible rules in order to make your rules suitable for API Consumers, like the ktlint-intellij-plugin, that allow their users to
 * fix violations that can be autocorrected on an interactive 1-by-1 basis.
 *
 * Whenever a rule implements this interface, the [beforeVisitChildNodes] and [afterVisitChildNodes] methods of this interface take
 * precedence above the methods with same name in the [Rule] class. As of that the rule should not only implement the interface, but also
 * change the implementation by replacing the implementation of [Rule.beforeVisitChildNodes] and/or [Rule.afterVisitChildNodes] in the rule
 * class with the methods of this class.
 */
public interface RuleAutocorrectApproveHandler {
    /**
     * This method is called on a node in AST before visiting the child nodes. This is repeated recursively for the
     * child nodes resulting in a depth first traversal of the AST.
     *
     * When a rule overrides this method, the API Consumer can decide per violation whether the violation needs to be autocorrected. For
     * this the [emitAndApprove] function is called, and its result can be used to determine whether the violation is to be corrected. In
     * lint mode the [emitAndApprove] should always return false.
     *
     * @param node AST node
     * @param emitAndApprove a way for rule to notify about a violation (lint error) and get approval to actually autocorrect the violation
     * if that is supported by the rule
     */
    public fun beforeVisitChildNodes(
        node: ASTNode,
        emitAndApprove: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
    }

    /**
     * This method is called on a node in AST after all its child nodes have been visited.
     *
     * When a rule overrides this method, the API Consumer can decide per violation whether the violation needs to be autocorrected. For
     * this the [emitAndApprove] function is called, and its result can be used to determine whether the violation is to be corrected. In
     * lint mode the [emitAndApprove] should always return false.
     *
     * @param node AST node
     * @param emitAndApprove a way for rule to notify about a violation (lint error) and get approval to actually autocorrect the violation
     * if that is supported by the rule
     */
    @Suppress("unused")
    public open fun afterVisitChildNodes(
        node: ASTNode,
        emitAndApprove: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
    }
}
