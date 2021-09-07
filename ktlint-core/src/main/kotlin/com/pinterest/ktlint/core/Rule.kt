package com.pinterest.ktlint.core

import com.pinterest.ktlint.core.internal.IdNamingPolicy
import org.jetbrains.kotlin.com.intellij.lang.ASTNode

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
        @Deprecated(
            message = "Marked for deletion in a future version. Annotate the rule with @RunOnRootNodeOnly.",
            level = DeprecationLevel.WARNING
        )
        interface RestrictToRoot
        @Deprecated(
            message = "Marked for deletion in a future version. Annotate the rule with @RunOnRootNodeOnly and @RunAsLateAsPossible.",
            level = DeprecationLevel.WARNING
        )
        interface RestrictToRootLast : RestrictToRoot
        @Deprecated(
            message = "Marked for deletion in a future version. Annotate the rule with @RunAsLateAsPossible.",
            level = DeprecationLevel.WARNING
        )
        interface Last
    }
}
