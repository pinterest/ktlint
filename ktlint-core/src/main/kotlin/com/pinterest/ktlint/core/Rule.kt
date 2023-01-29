package com.pinterest.ktlint.core

import com.pinterest.ktlint.core.api.EditorConfigProperties
import com.pinterest.ktlint.core.api.FeatureInAlphaState
import com.pinterest.ktlint.core.internal.IdNamingPolicyLegacy
import org.jetbrains.kotlin.com.intellij.lang.ASTNode

/**
 * The [Rule] contains the life cycle hooks which are needed for the KtLint rule engine.
 *
 * Implementation **doesn't** have to be thread-safe or stateless, provided that [RuleSetProviderV2] creates a new
 * instance of the [Rule] on each call to [RuleProvider.createNewRuleInstance]. The KtLint engine never re-uses a [Rule]
 * instance once is has been used for traversal of the AST of a file.
 */
@Deprecated("Deprecated since ktlint 0.49.0. Custom rulesets have to be migrated to RuleSetProviderV3. See changelog 0.49.")
public open class Rule(
    /**
     * Identification of the rule. Except for rules in the "standard" rule set, this id needs to be prefixed with the
     * identifier of the rule set (e.g. "some-rule-set-id:some-rule-id") to avoid naming conflicts with referring to the
     * rule (e.g. in [Rule.VisitorModifier.RunAfterRule] and in enable/disable rule suppression directives).
     *
     * In a future version of Ktlint this field is likely to be split into separate fields for the "rule-set-id" and the "rule-id". For now,
     * the following fields are supported:
     *   - id: the id as given during construction of the rule. It may (preferred) or may not contain a rule set id.
     *   - ruleId: the id of the rule without the rule set id prefix.
     *   - ruleSetId: the rule set if of the rule. Defaults to "standard" when not specified during construction of the rule.
     *   - qualifiedRuleId: the guaranteed fully qualified rule id containing the rule set id as prefix.
     */
    public val id: String,

    /**
     * Set of modifiers of the visitor. Preferably a rule has no modifiers at all, meaning that it is completely
     * independent of all other rules.
     */
    public val visitorModifiers: Set<VisitorModifier> = emptySet(),
) {
    private var traversalState = TraversalState.NOT_STARTED

    init {
        IdNamingPolicyLegacy.enforceRuleIdNaming(id)
    }

    /**
     * The id of the rule without the rule set id as prefix.
     */
    @OptIn(FeatureInAlphaState::class)
    public val ruleId: String = id.ruleId()

    /**
     * The rule set id of the rule. Defaults to "standard" when not specified during construction of the rule.
     */
    @OptIn(FeatureInAlphaState::class)
    public val ruleSetId: String = id.ruleSetId()

    /**
     * The guaranteed fully qualified rule id containing the rule set id as prefix. The rule set id defaults to "standard" when not
     * specified during construction of the rule.
     */
    public val qualifiedRuleId: String = qualifiedRuleId(ruleSetId, ruleId)

    /**
     * This method is called once before the first node is visited. It can be used to initialize the state of the rule
     * before processing of nodes starts.
     */
    public open fun beforeFirstNode(editorConfigProperties: EditorConfigProperties) {}

    /**
     * This method is called on a node in AST before visiting the child nodes. This is repeated recursively for the
     * child nodes resulting in a depth first traversal of the AST.
     *
     * @param node AST node
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public open fun beforeVisitChildNodes(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {}

    /**
     * This method is called on a node in AST after all its child nodes have been visited.
     */
    @Suppress("unused")
    public open fun afterVisitChildNodes(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {}

    /**
     * This method is called once after the last node in the AST is visited. It can be used for teardown of the state
     * of the rule.
     */
    public open fun afterLastNode() {}

    /**
     * Checks whether the [Rule] instance is used for traversal of the AST and as of that potentially has changed the state of the [Rule]
     * provided that it has state.
     */
    public fun isUsedForTraversalOfAST(): Boolean = traversalState != TraversalState.NOT_STARTED

    /**
     * Marks the [Rule] instance as being used for traversal of an AST. From this moment on, this instance of the [Rule]
     * can not be used to start a new traversal of the same or another AST as the instance might contain state.
     */
    public fun startTraversalOfAST() {
        require(traversalState == TraversalState.NOT_STARTED)
        traversalState = TraversalState.CONTINUE
    }

    /**
     * Checks whether the next node in the AST is to be traversed. By default, the entire AST is traversed.
     */
    public fun shouldContinueTraversalOfAST(): Boolean = traversalState == TraversalState.CONTINUE

    /**
     * Stops traversal of the AST. Intended usage it to prevent parsing of the remainder of the AST once the goal of the
     * rule is achieved. For example, if the ".editorconfig" property indent_size is set to 0 or -1 then the indent rule
     * should be disabled.
     *
     * When called in [beforeFirstNode], no AST nodes will be visited. [afterLastNode] is still called.
     *
     * When called in [beforeVisitChildNodes], the child nodes of that node will not be visited. [afterVisitChildNodes]
     * is still called for the node and each of its parent nodes. Other nodes in the AST will not be visited. Finally
     * [afterLastNode] is called.
     *
     * When called in [afterVisitChildNodes] the child nodes of that node are already visited. [afterVisitChildNodes] is
     * still called for each of its parent nodes. Other nodes in the AST will not be visited. Finally [afterLastNode] is
     * called.
     *
     * Calling in [afterLastNode] has no effect as traversal of the AST has already been completed.
     */
    public fun stopTraversalOfAST() {
        traversalState = TraversalState.STOP
    }

    private enum class TraversalState {
        /**
         * Traversal of the AST is not started. As no life cycle hooks of the [Rule] have been executed, the [Rule]
         * instance can not contain state specific for the AST.
         */
        NOT_STARTED,

        /**
         * Traversal of the AST is started and should be continued with next node.
         */
        CONTINUE,

        /**
         * Stops traversal of yet unvisited nodes in the AST. See [stopTraversalOfAST] for more details.
         */
        STOP,
    }

    public sealed class VisitorModifier {
        public data class RunAfterRule(
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
            val runOnlyWhenOtherRuleIsEnabled: Boolean = false,
        ) : VisitorModifier() {
            init {
                IdNamingPolicyLegacy.enforceRuleIdNaming(ruleId)
            }

            /**
             * The guaranteed fully qualified rule id containing the rule set id as prefix. The rule set id defaults to "standard" when not
             * specified during construction of the RunAfterRule.
             */
            public val qualifiedRuleId: String = ruleId.qualifiedRuleId()
        }

        public object RunAsLateAsPossible : VisitorModifier()
    }

    /**
     * This interface marks a rule as an 'experimental' rule. A rule marked with this interface will only be executed by ktlint in case the
     * '.editorconfig' allows this rule specifically or all experimental rules. This interface is used by Ktlint internally but is also
     * explicitly meant to be used by custom rule providers.
     */
    public interface Experimental

    /**
     * This interface marks a rule as an Official rule. A rule marked with this interface will only be executed when by ktlint in case the
     * '.editorconfig' contains property "code_style = ktlint_official" or when enabled explicitly. This interface is intended to be used
     * in Ktlint internally only. It may be subject to change at any time without providing any backward compatibility.
     */
    public interface OfficialCodeStyle
}

private const val STANDARD_RULE_SET_ID = "standard"
private const val DELIMITER = ":"

private fun String.ruleId(): String = this.substringAfter(DELIMITER, this)

private fun String.ruleSetId(): String = substringBefore(DELIMITER, STANDARD_RULE_SET_ID)

private fun String.qualifiedRuleId(): String = "${ruleSetId()}$DELIMITER${ruleId()}"

private fun qualifiedRuleId(
    ruleSetId: String,
    ruleId: String,
): String {
    return "$ruleSetId$DELIMITER$ruleId"
}
