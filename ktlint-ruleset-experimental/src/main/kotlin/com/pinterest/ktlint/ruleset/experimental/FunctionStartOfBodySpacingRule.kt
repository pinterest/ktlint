package com.pinterest.ktlint.ruleset.experimental

import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.ast.ElementType
import com.pinterest.ktlint.core.ast.ElementType.FUN
import com.pinterest.ktlint.core.ast.nextLeaf
import com.pinterest.ktlint.core.ast.prevLeaf
import com.pinterest.ktlint.core.ast.upsertWhitespaceAfterMe
import com.pinterest.ktlint.core.ast.upsertWhitespaceBeforeMe
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafElement
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement

/**
 * Lints and formats the spacing after the fun keyword
 */
public class FunctionStartOfBodySpacingRule : Rule("$experimentalRulesetId:function-start-of-body-spacing") {
    override fun visit(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit
    ) {
        if (node.elementType == FUN) {
            node
                .findChildByType(ElementType.EQ)
                ?.let { visitFunctionFollowedByBodyExpression(node, emit, autoCorrect) }

            node
                .findChildByType(ElementType.BLOCK)
                ?.let { visitFunctionFollowedByBodyBlock(node, emit, autoCorrect) }
        }
    }

    private fun visitFunctionFollowedByBodyExpression(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
        autoCorrect: Boolean
    ) {
        fixWhiteSpaceBeforeAssignmentOfBodyExpression(node, emit, autoCorrect)
        fixWhiteSpaceBetweenAssignmentAndBodyExpression(node, emit, autoCorrect)
    }

    private fun fixWhiteSpaceBeforeAssignmentOfBodyExpression(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
        autoCorrect: Boolean
    ) {
        node
            .findChildByType(ElementType.EQ)
            ?.let { assignmentExpression ->
                assignmentExpression
                    .prevLeaf(includeEmpty = true)
                    ?.takeIf { it.elementType == ElementType.WHITE_SPACE }
                    .let { whiteSpaceBeforeAssignment ->
                        if (whiteSpaceBeforeAssignment == null) {
                            emit(assignmentExpression.startOffset, "Expected a single white space before assignment of expression body", true)
                            if (autoCorrect) {
                                (assignmentExpression as LeafPsiElement).upsertWhitespaceBeforeMe(" ")
                            }
                        } else if (whiteSpaceBeforeAssignment.text != " ") {
                            emit(whiteSpaceBeforeAssignment.startOffset, "Unexpected whitespace", true)
                            if (autoCorrect) {
                                (assignmentExpression as LeafPsiElement).upsertWhitespaceBeforeMe(" ")
                            }
                        }
                    }
            }
    }

    private fun fixWhiteSpaceBetweenAssignmentAndBodyExpression(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
        autoCorrect: Boolean
    ) {
        node
            .findChildByType(ElementType.EQ)
            ?.let { assignmentExpression ->
                assignmentExpression
                    .nextLeaf(includeEmpty = true)
                    ?.takeIf { it.elementType == ElementType.WHITE_SPACE }
                    .let { whiteSpaceAfterAssignment ->
                        if (whiteSpaceAfterAssignment == null) {
                            emit(
                                assignmentExpression.startOffset,
                                "Expected a single white space between assignment and expression body on same line",
                                true
                            )
                            if (autoCorrect) {
                                (assignmentExpression as LeafPsiElement).upsertWhitespaceAfterMe(" ")
                            }
                        } else if (whiteSpaceAfterAssignment.text != " " && !whiteSpaceAfterAssignment.textContains('\n')) {
                            emit(whiteSpaceAfterAssignment.startOffset, "Unexpected whitespace", true)
                            if (autoCorrect) {
                                (assignmentExpression as LeafPsiElement).upsertWhitespaceAfterMe(" ")
                            }
                        }
                    }
            }
    }

    private fun visitFunctionFollowedByBodyBlock(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
        autoCorrect: Boolean
    ) {
        node
            .findChildByType(ElementType.BLOCK)
            ?.let { block ->
                block
                    .prevLeaf(includeEmpty = true)
                    ?.takeIf { it.elementType == ElementType.WHITE_SPACE }
                    .let { whiteSpaceBeforeExpressionBlock ->
                        if (whiteSpaceBeforeExpressionBlock == null) {
                            emit(block.startOffset, "Expected a single white space before start of function body", true)
                            if (autoCorrect) {
                                if (whiteSpaceBeforeExpressionBlock == null) {
                                    (block.firstChildNode.prevLeaf(true) as LeafPsiElement).upsertWhitespaceAfterMe(" ")
                                } else {
                                    (whiteSpaceBeforeExpressionBlock as LeafElement).rawReplaceWithText(" ")
                                }
                            }
                        } else if (whiteSpaceBeforeExpressionBlock.text != " ") {
                            emit(whiteSpaceBeforeExpressionBlock.startOffset, "Unexpected whitespace", true)
                            if (autoCorrect) {
                                if (whiteSpaceBeforeExpressionBlock == null) {
                                    (block.firstChildNode as LeafPsiElement).upsertWhitespaceBeforeMe(" ")
                                } else {
                                    (whiteSpaceBeforeExpressionBlock as LeafElement).rawReplaceWithText(" ")
                                }
                            }
                        }
                    }
            }
    }
}
