package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.rule.engine.core.api.ElementType
import com.pinterest.ktlint.rule.engine.core.api.ElementType.BLOCK_COMMENT
import com.pinterest.ktlint.rule.engine.core.api.ElementType.DOT
import com.pinterest.ktlint.rule.engine.core.api.ElementType.EOL_COMMENT
import com.pinterest.ktlint.rule.engine.core.api.ElementType.EXCLEXCL
import com.pinterest.ktlint.rule.engine.core.api.ElementType.RBRACE
import com.pinterest.ktlint.rule.engine.core.api.ElementType.SAFE_ACCESS
import com.pinterest.ktlint.rule.engine.core.api.ElementType.WHITE_SPACE
import com.pinterest.ktlint.rule.engine.core.api.Rule
import com.pinterest.ktlint.rule.engine.core.api.firstChildLeafOrSelf
import com.pinterest.ktlint.rule.engine.core.api.hasNewLineInClosedRange
import com.pinterest.ktlint.rule.engine.core.api.indent
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpace
import com.pinterest.ktlint.rule.engine.core.api.lastChildLeafOrSelf
import com.pinterest.ktlint.rule.engine.core.api.leavesIncludingSelf
import com.pinterest.ktlint.rule.engine.core.api.nextCodeSibling
import com.pinterest.ktlint.rule.engine.core.api.nextLeaf
import com.pinterest.ktlint.rule.engine.core.api.noNewLineInOpenRange
import com.pinterest.ktlint.rule.engine.core.api.parent
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
            chainOperator
                .prevLeaf { !it.isWhiteSpace() && it.elementType != EXCLEXCL && it.elementType != EOL_COMMENT && it.elementType != BLOCK_COMMENT }
                ?.lastChildLeafOrSelf() ?: return
        val previousExpressionBeforeBrace = previousEndBrace.prevCodeSibling()?.lastChildLeafOrSelf() ?: return
        val isPreviousChainElementMultiline =
            previousEndBrace.elementType == RBRACE &&
                hasNewLineInClosedRange(previousExpressionBeforeBrace, previousEndBrace)
        if (hasNewLineInClosedRange(chainOperator, rightExpression)) {
            emit(
                chainOperator.textRange.endOffset,
                "${
                    getLeftAlighingTextAfterAligningBrace(
                        isPreviousChainElementMultiline,
                        previousEndBrace,
                        chainOperator
                    )
                }${chainOperator.text} must merge at the start of next call",
                true,
            )
            if (autoCorrect) {
                if (isPreviousChainElementMultiline) {
                    /*
                        Detects code like below
                        bar {
                            ...
                        }.
                        foo() // this should align with previous line }
                     */
                    chainOperator.treeParent.addChild(
                        rightExpression.parent(ElementType.CALL_EXPRESSION)!!,
                        chainOperator.nextLeaf()
                    )
                    rightExpression.parent(ElementType.CALL_EXPRESSION)!!.upsertWhitespaceAfterMe("")
                } else {
                    val indent = rightExpression.indent()
//                    chainOperator.upsertWhitespaceBeforeMe(rightExpression.indent())
                    val chainParent = rightExpression.prevLeaf(includeEmpty = true)!!.treeParent
                    chainParent.replaceChild(rightExpression.prevLeaf(includeEmpty = true)!!, chainOperator)
                    chainOperator.upsertWhitespaceBeforeMe(indent)
                }
            }
        } else if (previousEndBrace.elementType == RBRACE &&
            isPreviousChainElementMultiline &&
            !noNewLineInOpenRange(
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
            emit(
                chainOperator.textRange.endOffset,
                "${chainOperator.text} must must merge at the end of previous call",
                true,
            )
            if (autoCorrect) {
                chainOperator.upsertWhitespaceBeforeMe("")
                var t = false
                val list =
                    chainOperator.leavesIncludingSelf().takeWhile {
                        it.also {
                            if (it.elementType !in listOf(EOL_COMMENT, BLOCK_COMMENT) && it != chainOperator) {
                                t = true
                            }
                        }.elementType !in listOf(WHITE_SPACE, EOL_COMMENT, BLOCK_COMMENT) || !t
                    }.toList()
                val rightExpCode = list.joinToString("") { it.text }
                list.forEach { it.treeParent.removeChild(it) }
                val text = previousEndBrace.nextLeaf().takeIf { it?.elementType == WHITE_SPACE }?.text.orEmpty().split("\n")
                    .joinToString("\n")
                previousEndBrace.upsertWhitespaceAfterMe(rightExpCode + text)
            }
        }
    }

    private fun getLeftAlighingTextAfterAligningBrace(
        isLeftExpressionEndBraceInSeparateLine: Boolean,
        leftExpressionEndBrace: ASTNode,
        chainOperator: ASTNode
    ) = if (isLeftExpressionEndBraceInSeparateLine) {
        leftExpressionEndBrace
            .leavesIncludingSelf()
            .takeWhile { it != chainOperator && !it.isWhiteSpace() }
            .map { it.text }
            .joinToString(separator = "")
    } else {
        ""
    }
}
