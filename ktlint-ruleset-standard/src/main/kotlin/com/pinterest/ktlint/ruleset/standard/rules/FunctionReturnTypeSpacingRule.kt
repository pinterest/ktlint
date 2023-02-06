package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.ruleset.core.api.ElementType.COLON
import com.pinterest.ktlint.ruleset.core.api.ElementType.FUN
import com.pinterest.ktlint.ruleset.core.api.ElementType.WHITE_SPACE
import com.pinterest.ktlint.ruleset.core.api.Rule
import com.pinterest.ktlint.ruleset.core.api.RuleId
import com.pinterest.ktlint.ruleset.core.api.nextLeaf
import com.pinterest.ktlint.ruleset.core.api.prevLeaf
import com.pinterest.ktlint.ruleset.core.api.upsertWhitespaceAfterMe
import com.pinterest.ktlint.ruleset.standard.StandardRule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode

public class FunctionReturnTypeSpacingRule :
    StandardRule("function-return-type-spacing"),
    Rule.Experimental {
    override fun beforeVisitChildNodes(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        node.firstChildNode
        node
            .takeIf { node.elementType == FUN }
            ?.let { node.findChildByType(COLON) }
            ?.let { colonNode ->
                removeWhiteSpaceBetweenClosingParenthesisAndColon(colonNode, emit, autoCorrect)
                fixWhiteSpaceBetweenColonAndReturnType(colonNode, emit, autoCorrect)
            }
    }

    private fun removeWhiteSpaceBetweenClosingParenthesisAndColon(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
        autoCorrect: Boolean,
    ) {
        require(node.elementType == COLON)
        node
            .prevLeaf()
            ?.takeIf { it.elementType == WHITE_SPACE }
            ?.let { whitespaceBeforeColonNode ->
                emit(whitespaceBeforeColonNode.startOffset, "Unexpected whitespace", true)
                if (autoCorrect) {
                    whitespaceBeforeColonNode.treeParent?.removeChild(whitespaceBeforeColonNode)
                }
            }
    }

    private fun fixWhiteSpaceBetweenColonAndReturnType(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
        autoCorrect: Boolean,
    ) {
        require(node.elementType == COLON)
        node
            .nextLeaf()
            ?.takeIf { it.elementType == WHITE_SPACE }
            .let { whiteSpaceAfterColon ->
                if (whiteSpaceAfterColon?.text != " ") {
                    emit(node.startOffset, "Single space expected between colon and return type", true)
                    if (autoCorrect) {
                        node.upsertWhitespaceAfterMe(" ")
                    }
                }
            }
    }
}

public val FUNCTION_RETURN_TYPE_SPACING_RULE_ID: RuleId = FunctionReturnTypeSpacingRule().ruleId
