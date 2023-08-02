package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.rule.engine.core.api.ElementType.BLOCK_COMMENT
import com.pinterest.ktlint.rule.engine.core.api.ElementType.DOT
import com.pinterest.ktlint.rule.engine.core.api.ElementType.EOL_COMMENT
import com.pinterest.ktlint.rule.engine.core.api.ElementType.EXCLEXCL
import com.pinterest.ktlint.rule.engine.core.api.ElementType.RBRACE
import com.pinterest.ktlint.rule.engine.core.api.ElementType.SAFE_ACCESS
import com.pinterest.ktlint.rule.engine.core.api.Rule
import com.pinterest.ktlint.rule.engine.core.api.firstChildLeafOrSelf
import com.pinterest.ktlint.rule.engine.core.api.hasNewLineInClosedRange
import com.pinterest.ktlint.rule.engine.core.api.indent
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpace
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpaceWithNewline
import com.pinterest.ktlint.rule.engine.core.api.lastChildLeafOrSelf
import com.pinterest.ktlint.rule.engine.core.api.leavesIncludingSelf
import com.pinterest.ktlint.rule.engine.core.api.nextCodeSibling
import com.pinterest.ktlint.rule.engine.core.api.prevCodeSibling
import com.pinterest.ktlint.rule.engine.core.api.prevLeaf
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
    private val chainOperators = TokenSet.create(DOT, SAFE_ACCESS)

    override fun beforeVisitChildNodes(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        node
            .takeIf { it.elementType in chainOperators }
            ?.let { visitChainOperator(node, autoCorrect, emit) }
    }

    private fun visitChainOperator(
        chainOperator: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        val rightExpression = chainOperator.nextCodeSibling()?.firstChildLeafOrSelf() ?: return
        val previousEndBrace =
            chainOperator.getPrevChainEndBrace() ?: return
        val previousExpressionBeforeBrace = previousEndBrace.prevCodeSibling()?.lastChildLeafOrSelf() ?: return
        val isPreviousChainElementMultiline =
            previousEndBrace.elementType == RBRACE &&
                hasNewLineInClosedRange(previousExpressionBeforeBrace, previousEndBrace)
        if (hasNewLineInClosedRange(chainOperator, rightExpression)) {
            /*
                Detects code like below
                bar {
                    ...
                }.
                foo() // unexpected new line after .

                or
                bar(). // expected newline before .
                foo()
             */
            fixMisalignedChainOperator(
                emit,
                chainOperator,
                isPreviousChainElementMultiline,
                previousEndBrace,
                autoCorrect,
                rightExpression,
            )
        } else if (previousEndBrace.elementType == RBRACE &&
            isPreviousChainElementMultiline &&
            hasNewLineInClosedRange(
                previousEndBrace,
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
            fixMisalignedChain(emit, chainOperator, autoCorrect)
        }
    }

    private fun fixMisalignedChain(
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
        chainOperator: ASTNode,
        autoCorrect: Boolean,
    ) {
        emit(
            chainOperator.textRange.endOffset,
            getMisAlignedChainErrorMessage(chainOperator),
            true,
        )
        if (autoCorrect) {
            val prevMultilineWhiteSpace =
                chainOperator.leafWithMultilineWhiteSpace(forward = false)
            prevMultilineWhiteSpace.treeParent.removeChild(prevMultilineWhiteSpace)
        }
    }

    private fun fixMisalignedChainOperator(
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
        chainOperator: ASTNode,
        isPreviousChainElementMultiline: Boolean,
        previousEndBrace: ASTNode,
        autoCorrect: Boolean,
        rightExpression: ASTNode,
    ) {
        emit(
            chainOperator.textRange.endOffset,
            getWhenMisAlignedChainOperatorErrorMessage(
                isPreviousChainElementMultiline,
                previousEndBrace,
                chainOperator,
            ),
            true,
        )
        if (autoCorrect) {
            if (!isPreviousChainElementMultiline) {
                val indent = rightExpression.indent()
                chainOperator.upsertWhitespaceBeforeMe(indent)
            }
            val nextMultilineWhiteSpace =
                chainOperator.leafWithMultilineWhiteSpace(forward = true)
            nextMultilineWhiteSpace.treeParent.removeChild(nextMultilineWhiteSpace)
        }
    }

    private fun getWhenMisAlignedChainOperatorErrorMessage(
        isPreviousChainElementMultiline: Boolean,
        leftExpressionEndBrace: ASTNode,
        chainOperator: ASTNode,
    ): String {
        val expressionText =
            if (isPreviousChainElementMultiline) {
                leftExpressionEndBrace
                    .leavesTillMultilineWhiteSpace(forward = true)
                    .toList()
                    .dropLast(1)
                    .joinToString(separator = "") { it.text }
            } else {
                chainOperator.text
            }

        return if (isPreviousChainElementMultiline) {
            "Unexpected newline after '$expressionText'"
        } else {
            "Expected newline before '$expressionText'"
        }
    }

    private fun getMisAlignedChainErrorMessage(chainOperator: ASTNode): String {
        val expressionText =
            chainOperator
                .leavesTillMultilineWhiteSpace(forward = false)
                .toList()
                .dropLast(1)
                .reversed()
                .joinToString(separator = "") { it.text }

        return "Unexpected newline before '$expressionText'"
    }

    private fun ASTNode.leafWithMultilineWhiteSpace(forward: Boolean): ASTNode = leavesTillMultilineWhiteSpace(forward).last()

    private fun ASTNode.leavesTillMultilineWhiteSpace(forward: Boolean) =
        this.leavesIncludingSelf(forward)
            .takeTill { it.isWhiteSpaceWithNewline() }

    private fun ASTNode.getPrevChainEndBrace() =
        prevLeaf { !it.isWhiteSpace() && it.elementType != EXCLEXCL && it.elementType != EOL_COMMENT && it.elementType != BLOCK_COMMENT }
            ?.lastChildLeafOrSelf()

    private fun <T> Sequence<T>.takeTill(predicate: (T) -> Boolean): Sequence<T> {
        var conditionMet = false
        return this
            .takeWhile { conditionMet.not() }
            .map {
                if (predicate(it)) {
                    conditionMet = true
                }
                it
            }
    }
}
