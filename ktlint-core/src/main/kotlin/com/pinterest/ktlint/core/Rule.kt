package com.pinterest.ktlint.core

import com.pinterest.ktlint.core.api.EditorConfigProperties
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
public open class Rule(
    val id: String,
    public val visitorModifiers: Set<VisitorModifier> = emptySet()
) {
    init {
        IdNamingPolicy.enforceRuleIdNaming(id)
    }

    /**
     * This method is called once before the first node is visited. It can be used to initialize the state of the rule
     * before processing of nodes starts.
     */
    @Suppress("UNUSED_PARAMETER")
    public open fun beforeFirstNode(editorConfigProperties: EditorConfigProperties) {}

    /**
     * This method is called on a node in AST before visiting the child nodes. This is repeated recursively for the
     * child nodes resulting in a depth first traversal of the AST.
     *
     * @param node AST node
     * @param autoCorrect indicates whether rule should attempt auto-correction
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public open fun beforeVisitChildNodes(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit
    ) =
        /**
         * For backwards compatibility with ktlint 0.46.x or before, call [visit] when not implemented on node.
         * Add abstract function modifier and remove function body after removal of deprecated [visit] method to enforce
         * explicit implementation by rule developer.
         */
        visit(node, autoCorrect, emit)

    /**
     * Rules that override method [visit] should rename that method to [beforeVisitChildNodes]. For backwards
     * compatibility reasons (in KtLint 0.47 only), this method is called via the default implementation of
     * [beforeVisitChildNodes]. Whenever [beforeVisitChildNodes] is overridden with a custom implementation, this method
     * will no longer be called.
     *
     * @param node AST node
     * @param autoCorrect indicates whether rule should attempt auto-correction
     * @param emit a way for rule to notify about a violation (lint error)
     */
    @Deprecated(
        message = "Marked for deletion in ktlint 0.48.0",
        replaceWith = ReplaceWith("beforeVisitChildNodes(node, autocorrect, emit")
    )
    @Suppress("UNUSED_PARAMETER")
    public open fun visit(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit
    ) {}

    /**
     * This method is called on a node in AST after all its child nodes have been visited.
     */
    @Suppress("unused", "UNUSED_PARAMETER")
    public open fun afterVisitChildNodes(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit
    ) {}

    /**
     * This method is called once after the last node in the AST is visited. It can be used for teardown of the state
     * of the rule.
     */
    public open fun afterLastNode() {}

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

        @Deprecated(
            """
                Marked for removal in Ktlint 0.48. This modifier blocks the ability to suppress ktlint rules. See
                changelog Ktlint 0.47 for details on how to modify a rule using this modifier.
                """
        )
        object RunOnRootNodeOnly : VisitorModifier()
    }
}
