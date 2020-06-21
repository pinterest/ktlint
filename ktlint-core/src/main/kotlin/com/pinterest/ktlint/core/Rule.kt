package com.pinterest.ktlint.core

import com.pinterest.ktlint.core.internal.IdNamingPolicy
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.lang.FileASTNode

/**
 * A rule contract.
 *
 * Implementation **doesn't** have to be thread-safe or stateless
 * (provided [RuleSetProvider] creates a new instance on each `get()` call).
 *
 * @param id must be unique within the ruleset
 *
 * @see RuleSet
 */
abstract class Rule(val id: String) {

    init {
        IdNamingPolicy.enforceNaming(id)
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

    object Modifier {
        /**
         * Any rule implementing this interface will be given root ([FileASTNode]) node only
         * (in other words, [visit] will be called on [FileASTNode] but not on [FileASTNode] children).
         */
        interface RestrictToRoot
        /**
         * Marker interface to indicate that rule must be executed after all other rules (order among multiple
         * [RestrictToRootLast] rules is not defined and should be assumed to be random).
         *
         * Note that [RestrictToRootLast] implements [RestrictToRoot].
         */
        interface RestrictToRootLast : RestrictToRoot
        interface Last
    }
}
