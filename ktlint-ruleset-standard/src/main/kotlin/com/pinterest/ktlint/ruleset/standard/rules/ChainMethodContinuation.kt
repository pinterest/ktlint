package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.rule.engine.core.api.ElementType
import com.pinterest.ktlint.rule.engine.core.api.Rule
import com.pinterest.ktlint.rule.engine.core.api.firstChildLeafOrSelf
import com.pinterest.ktlint.rule.engine.core.api.hasNewLineInClosedRange
import com.pinterest.ktlint.rule.engine.core.api.indent
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpace
import com.pinterest.ktlint.rule.engine.core.api.lastChildLeafOrSelf
import com.pinterest.ktlint.rule.engine.core.api.leavesIncludingSelf
import com.pinterest.ktlint.rule.engine.core.api.nextCodeSibling
import com.pinterest.ktlint.rule.engine.core.api.noNewLineInOpenRange
import com.pinterest.ktlint.rule.engine.core.api.prevCodeSibling
import com.pinterest.ktlint.rule.engine.core.api.prevLeaf
import com.pinterest.ktlint.rule.engine.core.api.upsertWhitespaceAfterMe
import com.pinterest.ktlint.rule.engine.core.api.upsertWhitespaceBeforeMe
import com.pinterest.ktlint.ruleset.standard.StandardRule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.tree.TokenSet

/**
 * When present, align chain operators('.' or '?.') at previous line with next method call
 */
public class ChainMethodContinuation :
    StandardRule("chain-method-continuation"),
    Rule.Experimental {
    private val chainOperators =
        TokenSet.create(
            ElementType.DOT_QUALIFIED_EXPRESSION,
            ElementType.SAFE_ACCESS_EXPRESSION,
        )

    override fun beforeVisitChildNodes(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        node.takeIf {
            it.elementType in chainOperators
        }?.let {
            visitChains(node, autoCorrect, emit)
        }
    }

    private fun visitChains(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        val chainOperator = node.findChildByType(ElementType.SAFE_ACCESS) ?: node.findChildByType(ElementType.DOT)
        chainOperator ?: return
        val rightExpression = chainOperator.nextCodeSibling()?.firstChildLeafOrSelf() ?: return
        val leftExpressionEndBrace =
            chainOperator.prevLeaf { !it.isWhiteSpace() && it.elementType != ElementType.EXCLEXCL }
                ?.lastChildLeafOrSelf() ?: return
        val leftExpressionBeforeEndBrace = leftExpressionEndBrace.prevCodeSibling()?.lastChildLeafOrSelf() ?: return
        val isLeftExpressionEndBraceInSeparateLine =
            leftExpressionEndBrace.elementType == ElementType.RBRACE &&
                hasNewLineInClosedRange(leftExpressionBeforeEndBrace, leftExpressionEndBrace)
        if (!noNewLineInOpenRange(chainOperator, rightExpression)) {
            emit(
                chainOperator.textRange.endOffset,
                "${
                    if (isLeftExpressionEndBraceInSeparateLine) {
                        leftExpressionEndBrace.leavesIncludingSelf()
                            .takeWhile { it != chainOperator && !it.isWhiteSpace() }.map { it.text }
                            .joinToString(separator = "")
                    } else {
                        ""
                    }
                }${chainOperator.text} must merge at the start of next call",
                true,
            )
            if (autoCorrect) {
                if (isLeftExpressionEndBraceInSeparateLine) {
                    /*
                        Detects code like below
                        bar {
                            ...
                        }.
                        foo() // this should align with previous line }
                     */
                    chainOperator.upsertWhitespaceAfterMe("")
                } else {
                    chainOperator.upsertWhitespaceBeforeMe(rightExpression.indent())
                    rightExpression.upsertWhitespaceBeforeMe("")
                }
            }
        } else if (leftExpressionEndBrace.elementType == ElementType.RBRACE &&
            isLeftExpressionEndBraceInSeparateLine &&
            !noNewLineInOpenRange(
                leftExpressionEndBrace,
                chainOperator,
            )
        ) {
            /*
                Detects code like below
                bar {
                    ...
                }
                .foo() // this should align with previous line }
             */
            emit(
                chainOperator.textRange.endOffset,
                "${chainOperator.text} must must merge at the end of previous call",
                true,
            )
            if (autoCorrect) {
                chainOperator.upsertWhitespaceBeforeMe("")
            }
        }
    }
}
