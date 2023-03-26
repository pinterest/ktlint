package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.rule.engine.core.api.ElementType.ELSE
import com.pinterest.ktlint.rule.engine.core.api.ElementType.ELSE_KEYWORD
import com.pinterest.ktlint.rule.engine.core.api.ElementType.RPAR
import com.pinterest.ktlint.rule.engine.core.api.ElementType.THEN
import com.pinterest.ktlint.rule.engine.core.api.ElementType.TYPE_PARAMETER_LIST
import com.pinterest.ktlint.rule.engine.core.api.Rule
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.isPartOfComment
import com.pinterest.ktlint.rule.engine.core.api.nextCodeSibling
import com.pinterest.ktlint.rule.engine.core.api.prevCodeSibling
import com.pinterest.ktlint.ruleset.standard.StandardRule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.tree.IElementType

/**
 * The AST allows comments to be placed anywhere. This however can lead to code which is unnecessarily hard to read.
 * Disallowing comments at certain positions in the AST makes development of a rule easier as they have not to be taken
 * into account in that rule.
 *
 * In examples below, a comment is placed between a type parameter list and the function name. Such comments are badly
 * handled by default IntelliJ IDEA code formatter. We should put no effort in making and keeping ktlint in sync with
 * such bad code formatting.
 *
 * ```kotlin
 *     fun <T>
 *     // some comment
 *         foo(t: T) = "some-result"
 *
 *     fun <T>
 *         /* some comment
 *      *
 *      */
 *         foo(t: T) = "some-result"
 * ```
 */
public class DiscouragedCommentLocationRule :
    StandardRule("discouraged-comment-location"),
    Rule.Experimental {
    override fun beforeVisitChildNodes(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        if (node.isPartOfComment()) {
            // Be restrictive when adding new locations at which comments are discouraged. Always run against major
            // open source projects first to verify whether valid cases are found to comment at this location.
            if (node.afterNodeOfElementType(TYPE_PARAMETER_LIST) ||
                node.betweenNodesOfElementType(RPAR, THEN) ||
                node.betweenNodesOfElementType(THEN, ELSE_KEYWORD) ||
                node.betweenNodesOfElementType(ELSE_KEYWORD, ELSE)
            ) {
                emit(node.startOffset, "No comment expected at this location", false)
            }
        }
    }

    private fun ASTNode.afterNodeOfElementType(afterElementType: IElementType) = prevCodeSibling()?.elementType == afterElementType

    private fun ASTNode.beforeNodeOfElementType(beforeElementType: IElementType) = nextCodeSibling()?.elementType == beforeElementType

    private fun ASTNode.betweenNodesOfElementType(
        afterElementType: IElementType,
        beforeElementType: IElementType,
    ) = afterNodeOfElementType(afterElementType) && beforeNodeOfElementType(beforeElementType)
}

public val DISCOURAGED_COMMENT_LOCATION_RULE_ID: RuleId = DiscouragedCommentLocationRule().ruleId
