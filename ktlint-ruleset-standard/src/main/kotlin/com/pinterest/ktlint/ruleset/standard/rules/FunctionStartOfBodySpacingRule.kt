package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.ElementType
import com.pinterest.ktlint.rule.engine.core.api.ElementType.FUN
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint.Status.EXPERIMENTAL
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint.Status.STABLE
import com.pinterest.ktlint.rule.engine.core.api.ifAutocorrectAllowed
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpaceWithNewline20
import com.pinterest.ktlint.rule.engine.core.api.nextLeaf
import com.pinterest.ktlint.rule.engine.core.api.prevLeaf
import com.pinterest.ktlint.rule.engine.core.api.upsertWhitespaceAfterMe
import com.pinterest.ktlint.rule.engine.core.api.upsertWhitespaceBeforeMe
import com.pinterest.ktlint.ruleset.standard.StandardRule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode

/**
 * Lints and formats the spacing after the fun keyword
 */
@SinceKtlint("0.46", EXPERIMENTAL)
@SinceKtlint("0.49", STABLE)
public class FunctionStartOfBodySpacingRule : StandardRule("function-start-of-body-spacing") {
    override fun beforeVisitChildNodes(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        if (node.elementType == FUN) {
            node
                .findChildByType(ElementType.EQ)
                ?.let { visitFunctionFollowedByBodyExpression(node, emit) }

            node
                .findChildByType(ElementType.BLOCK)
                ?.let { visitFunctionFollowedByBodyBlock(node, emit) }
        }
    }

    private fun visitFunctionFollowedByBodyExpression(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        fixWhiteSpaceBeforeAssignmentOfBodyExpression(node, emit)
        fixWhiteSpaceBetweenAssignmentAndBodyExpression(node, emit)
    }

    private fun fixWhiteSpaceBeforeAssignmentOfBodyExpression(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        node
            .findChildByType(ElementType.EQ)
            ?.let { assignmentExpression ->
                assignmentExpression
                    .prevLeaf
                    ?.takeIf { it.elementType == ElementType.WHITE_SPACE }
                    .let { whiteSpaceBeforeAssignment ->
                        if (whiteSpaceBeforeAssignment == null) {
                            emit(
                                assignmentExpression.startOffset,
                                "Expected a single white space before assignment of expression body",
                                true,
                            ).ifAutocorrectAllowed {
                                assignmentExpression.upsertWhitespaceBeforeMe(" ")
                            }
                            Unit
                        } else if (whiteSpaceBeforeAssignment.text != " ") {
                            emit(whiteSpaceBeforeAssignment.startOffset, "Unexpected whitespace", true)
                                .ifAutocorrectAllowed {
                                    assignmentExpression.upsertWhitespaceBeforeMe(" ")
                                }
                        }
                    }
            }
    }

    private fun fixWhiteSpaceBetweenAssignmentAndBodyExpression(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        node
            .findChildByType(ElementType.EQ)
            ?.let { assignmentExpression ->
                assignmentExpression
                    .nextLeaf
                    ?.takeIf { it.elementType == ElementType.WHITE_SPACE }
                    .let { whiteSpaceAfterAssignment ->
                        if (whiteSpaceAfterAssignment?.text != " " && !whiteSpaceAfterAssignment.isWhiteSpaceWithNewline20) {
                            emit(
                                assignmentExpression.startOffset,
                                "Expected a single white space between assignment and expression body on same line",
                                true,
                            ).ifAutocorrectAllowed {
                                assignmentExpression.upsertWhitespaceAfterMe(" ")
                            }
                        }
                    }
            }
    }

    private fun visitFunctionFollowedByBodyBlock(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        node
            .findChildByType(ElementType.BLOCK)
            ?.let { block ->
                block
                    .prevLeaf
                    ?.takeIf { it.elementType == ElementType.WHITE_SPACE }
                    .let { whiteSpaceBeforeExpressionBlock ->
                        if (whiteSpaceBeforeExpressionBlock?.text != " ") {
                            emit(block.startOffset, "Expected a single white space before start of function body", true)
                                .ifAutocorrectAllowed {
                                    block
                                        .firstChildNode
                                        .prevLeaf
                                        ?.upsertWhitespaceAfterMe(" ")
                                }
                        }
                    }
            }
    }
}

public val FUNCTION_START_OF_BODY_SPACING_RULE_ID: RuleId = FunctionStartOfBodySpacingRule().ruleId
