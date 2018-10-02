package com.github.shyiko.ktlint.ruleset.standard

import com.github.shyiko.ktlint.core.Rule
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.PsiComment
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
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
            if (node.psi.isPartOf(PsiComment::class)) {
                return
            }
            val nextLeaf = node.psi.nextLeafIgnoringWhitespaceAndComments()?.prevLeaf(true)
            if (nextLeaf is PsiWhiteSpaceImpl && nextLeaf.textContains('\n')) {
                emit(node.startOffset, "Line must not end with \"${node.text}\"", true)
                if (autoCorrect) {
                    // rewriting
                    // <prevLeaf><node="."><nextLeaf="\n"> to
                    // <prevLeaf><delete space if any><nextLeaf="\n"><node="."><space if needed>
                    // (or)
                    // <prevLeaf><node="."><spaceBeforeComment><comment><nextLeaf="\n"> to
                    // <prevLeaf><delete space if any><spaceBeforeComment><comment><nextLeaf="\n"><node="."><space if needed>
                    val prevLeaf = node.psi.prevLeaf(true)
                    if (prevLeaf is PsiWhiteSpaceImpl) {
                        prevLeaf.node.treeParent.removeChild(prevLeaf.node)
                    }
                    if (!noSpaceAroundTokens.contains(elementType)) {
                        nextLeaf.rawInsertAfterMe(PsiWhiteSpaceImpl(" "))
                    }
                    node.treeParent.removeChild(node)
                    nextLeaf.rawInsertAfterMe(node.psi as LeafPsiElement)
                }
            }
        } else if (sameLineTokens.contains(elementType) || prefixTokens.contains(elementType)) {
            if (node.psi.isPartOf(PsiComment::class)) {
                return
            }
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
                    // rewriting
                    // <insertionPoint><prevLeaf="\n"><node="&&"><nextLeaf=" "> to
                    // <insertionPoint><prevLeaf=" "><node="&&"><nextLeaf="\n"><delete node="&&"><delete nextLeaf=" ">
                    // (or)
                    // <insertionPoint><spaceBeforeComment><comment><prevLeaf="\n"><node="&&"><nextLeaf=" "> to
                    // <insertionPoint><space if needed><node="&&"><spaceBeforeComment><comment><prevLeaf="\n"><delete node="&&"><delete nextLeaf=" ">
                    val nextLeaf = node.psi.nextLeaf(true)
                    if (nextLeaf is PsiWhiteSpaceImpl) {
                        nextLeaf.node.treeParent.removeChild(nextLeaf.node)
                    }
                    val insertionPoint = prevLeaf.prevLeafIgnoringWhitespaceAndComments() as LeafPsiElement
                    node.treeParent.removeChild(node)
                    insertionPoint.rawInsertAfterMe(node.psi as LeafPsiElement)
                    if (!noSpaceAroundTokens.contains(elementType)) {
                        insertionPoint.rawInsertAfterMe(PsiWhiteSpaceImpl(" "))
                    }
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
}
