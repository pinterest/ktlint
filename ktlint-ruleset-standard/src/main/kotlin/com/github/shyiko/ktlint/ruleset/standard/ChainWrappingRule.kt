package com.github.shyiko.ktlint.ruleset.standard

import com.github.shyiko.ktlint.core.Rule
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.PsiComment
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.PsiWhiteSpaceImpl
import org.jetbrains.kotlin.com.intellij.psi.tree.TokenSet
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.lexer.KtTokens.ANDAND
import org.jetbrains.kotlin.lexer.KtTokens.DIV
import org.jetbrains.kotlin.lexer.KtTokens.DOT
import org.jetbrains.kotlin.lexer.KtTokens.ELVIS
import org.jetbrains.kotlin.lexer.KtTokens.MINUS
import org.jetbrains.kotlin.lexer.KtTokens.MUL
import org.jetbrains.kotlin.lexer.KtTokens.OROR
import org.jetbrains.kotlin.lexer.KtTokens.PERC
import org.jetbrains.kotlin.lexer.KtTokens.PLUS
import org.jetbrains.kotlin.lexer.KtTokens.SAFE_ACCESS
import org.jetbrains.kotlin.psi.psiUtil.nextLeaf
import org.jetbrains.kotlin.psi.psiUtil.nextLeafs
import org.jetbrains.kotlin.psi.psiUtil.prevLeaf

class ChainWrappingRule : Rule("chain-wrapping") {

    private val sameLineTokens = TokenSet.create(MUL, DIV, PERC, ANDAND, OROR)
    private val prefixTokens = TokenSet.create(PLUS, MINUS)
    private val nextLineTokens = TokenSet.create(DOT, SAFE_ACCESS, ELVIS)
    private val noSpaceAroundTokens = TokenSet.create(DOT, SAFE_ACCESS)

    override fun visit(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit
    ) {
        /*
           org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement (DOT) | "."
           org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.PsiWhiteSpaceImpl (WHITE_SPACE) | "\n        "
           org.jetbrains.kotlin.psi.KtCallExpression (CALL_EXPRESSION)
         */
        val elementType = node.elementType
        if (nextLineTokens.contains(elementType)) {
            val nextLeaf = node.psi.nextLeaf(true)
            if (nextLeaf is PsiWhiteSpaceImpl && nextLeaf.textContains('\n')) {
                emit(node.startOffset, "Line must not end with \"${node.text}\"", true)
                if (autoCorrect) {
                    val prevLeaf = node.psi.prevLeaf(true)
                    if (prevLeaf is PsiWhiteSpaceImpl) {
                        prevLeaf.rawReplaceWithText(nextLeaf.text)
                    } else {
                        (node.psi as LeafPsiElement).rawInsertBeforeMe(PsiWhiteSpaceImpl(nextLeaf.text))
                    }
                    if (noSpaceAroundTokens.contains(elementType)) {
                        nextLeaf.node.treeParent.removeChild(nextLeaf.node)
                    } else {
                        nextLeaf.rawReplaceWithText(" ")
                    }
                }
            }
        } else if (sameLineTokens.contains(elementType) || prefixTokens.contains(elementType)) {
            val prevLeaf = node.psi.prevLeaf(true)
            if (
                prevLeaf is PsiWhiteSpaceImpl &&
                prevLeaf.textContains('\n') &&
                // fn(*typedArray<...>()) case
                (elementType != MUL || !prevLeaf.isPartOfSpread()) &&
                // unary +/-
                (!prefixTokens.contains(elementType) || !node.isInPrefixPosition()) &&
                // LeafPsiElement->KtOperationReferenceExpression->KtPrefixExpression->KtWhenConditionWithExpression
                !node.isPartOfWhenCondition()
            ) {
                emit(node.startOffset, "Line must not begin with \"${node.text}\"", true)
                if (autoCorrect) {
                    val leavesToMove =
                        listOf(node.psi) +
                            node.psi.nextLeafs.takeWhile {
                                it is PsiWhiteSpace
                            }.toList()
                    var textToMove = leavesToMove
                        .joinToString("") { it.text }
                    var insertionPoint = prevLeaf.prevLeafIgnoringWhitespaceAndComments()
                        ?: prevLeaf
                    val afterInsertionPoint = insertionPoint.nextLeaf(true)

                    if (
                        afterInsertionPoint is PsiWhiteSpace &&
                        !afterInsertionPoint.textContains('\n')
                    ) {
                        // If we have whitespace to insert after on the same line,
                        // go ahead and do it so we don't have to add our own.
                        insertionPoint = afterInsertionPoint
                    } else if (afterInsertionPoint is PsiWhiteSpace) {
                        // Or if there is whitespace and it has a newline,
                        // ensure we don't add trailing space.
                        textToMove = " ${textToMove.trimEnd()}"
                    } else {
                        // Otherwise, ensure there's at least a space between the
                        // left operand and the operator.
                        textToMove = " $textToMove"
                    }

                    for (leafToMove in leavesToMove) {
                        leafToMove.node.treeParent.removeChild(leafToMove.node)
                    }

                    (insertionPoint as LeafPsiElement).rawInsertAfterMe(PsiWhiteSpaceImpl(textToMove))
                }
            }
        }
    }

    private fun PsiElement.isPartOfSpread() =
        prevLeafIgnoringWhitespaceAndComments()?.let { leaf ->
            val type = leaf.node.elementType
            type == KtTokens.LPAR ||
            type == KtTokens.COMMA ||
            type == KtTokens.LBRACE ||
            type == KtTokens.ELSE_KEYWORD ||
            KtTokens.OPERATIONS.contains(type)
        } == true

    private fun ASTNode.isInPrefixPosition() =
        treeParent?.treeParent?.elementType == KtNodeTypes.PREFIX_EXPRESSION

    private fun ASTNode.isPartOfWhenCondition() =
        treeParent?.treeParent?.treeParent?.elementType == KtNodeTypes.WHEN_CONDITION_EXPRESSION

    private fun PsiElement.prevLeafIgnoringWhitespaceAndComments() =
        this.prevLeaf { it.node.elementType != KtTokens.WHITE_SPACE && !it.isPartOf(PsiComment::class) }
}
