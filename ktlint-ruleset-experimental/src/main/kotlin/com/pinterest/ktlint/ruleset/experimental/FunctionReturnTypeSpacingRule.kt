package com.pinterest.ktlint.ruleset.experimental

import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.ast.ElementType.COLON
import com.pinterest.ktlint.core.ast.ElementType.FUN
import com.pinterest.ktlint.core.ast.ElementType.WHITE_SPACE
import com.pinterest.ktlint.core.ast.nextLeaf
import com.pinterest.ktlint.core.ast.prevLeaf
import com.pinterest.ktlint.core.ast.upsertWhitespaceAfterMe
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafElement

public class FunctionReturnTypeSpacingRule : Rule("function-return-type-spacing") {
    override fun visit(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit
    ) {
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
        autoCorrect: Boolean
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
        autoCorrect: Boolean
    ) {
        require(node.elementType == COLON)
        node
            .nextLeaf()
            ?.takeIf { it.elementType == WHITE_SPACE }
            .let { whiteSpaceAfterColon ->
                if (whiteSpaceAfterColon == null) {
                    emit(node.startOffset, "Single space expected between colon and return type", true)
                    if (autoCorrect) {
                        (node as LeafElement).upsertWhitespaceAfterMe(" ")
                    }
                } else if (whiteSpaceAfterColon.text != " ") {
                    emit(node.startOffset, "Unexpected whitespace", true)
                    if (autoCorrect) {
                        (whiteSpaceAfterColon as LeafElement).rawReplaceWithText(" ")
                    }
                }
            }
    }
}
