package com.pinterest.ktlint.ruleset.experimental

import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.ast.ElementType
import com.pinterest.ktlint.core.ast.ElementType.FUN
import com.pinterest.ktlint.core.ast.nextLeaf
import com.pinterest.ktlint.core.ast.prevLeaf
import com.pinterest.ktlint.core.ast.upsertWhitespaceAfterMe
import com.pinterest.ktlint.core.ast.upsertWhitespaceBeforeMe
import org.jetbrains.kotlin.com.intellij.lang.ASTNode

/**
 * Lints and formats the spacing after the fun keyword
 */
public class FunctionStartOfBodySpacingRule : Rule("$EXPERIMENTAL_RULE_SET_ID:function-start-of-body-spacing") {
    override fun beforeVisitChildNodes(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
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
        autoCorrect: Boolean,
    ) {
        fixWhiteSpaceBeforeAssignmentOfBodyExpression(node, emit, autoCorrect)
        fixWhiteSpaceBetweenAssignmentAndBodyExpression(node, emit, autoCorrect)
    }

    private fun fixWhiteSpaceBeforeAssignmentOfBodyExpression(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
        autoCorrect: Boolean,
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
                                assignmentExpression.upsertWhitespaceBeforeMe(" ")
                            }
                        } else if (whiteSpaceBeforeAssignment.text != " ") {
                            emit(whiteSpaceBeforeAssignment.startOffset, "Unexpected whitespace", true)
                            if (autoCorrect) {
                                assignmentExpression.upsertWhitespaceBeforeMe(" ")
                            }
                        }
                    }
            }
    }

    private fun fixWhiteSpaceBetweenAssignmentAndBodyExpression(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
        autoCorrect: Boolean,
    ) {
        node
            .findChildByType(ElementType.EQ)
            ?.let { assignmentExpression ->
                assignmentExpression
                    .nextLeaf(includeEmpty = true)
                    ?.takeIf { it.elementType == ElementType.WHITE_SPACE }
                    .let { whiteSpaceAfterAssignment ->
                        if (!(whiteSpaceAfterAssignment?.text == " " || whiteSpaceAfterAssignment?.textContains('\n') == true)) {
                            emit(
                                assignmentExpression.startOffset,
                                "Expected a single white space between assignment and expression body on same line",
                                true,
                            )
                            if (autoCorrect) {
                                assignmentExpression.upsertWhitespaceAfterMe(" ")
                            }
                        }
                    }
            }
    }

    private fun visitFunctionFollowedByBodyBlock(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
        autoCorrect: Boolean,
    ) {
        node
            .findChildByType(ElementType.BLOCK)
            ?.let { block ->
                block
                    .prevLeaf(includeEmpty = true)
                    ?.takeIf { it.elementType == ElementType.WHITE_SPACE }
                    .let { whiteSpaceBeforeExpressionBlock ->
                        if (whiteSpaceBeforeExpressionBlock?.text != " ") {
                            emit(block.startOffset, "Expected a single white space before start of function body", true)
                            if (autoCorrect) {
                                block
                                    .firstChildNode
                                    .prevLeaf(true)
                                    ?.upsertWhitespaceAfterMe(" ")
                            }
                        }
                    }
            }
    }
}
