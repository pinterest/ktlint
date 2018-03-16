package com.github.shyiko.ktlint.ruleset.standard

import com.github.shyiko.ktlint.core.Rule
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.PsiComment
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.PsiWhiteSpaceImpl
import org.jetbrains.kotlin.com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.com.intellij.psi.tree.TokenSet
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.lexer.KtTokens.ANDAND
import org.jetbrains.kotlin.lexer.KtTokens.COMMA
import org.jetbrains.kotlin.lexer.KtTokens.DIV
import org.jetbrains.kotlin.lexer.KtTokens.DOT
import org.jetbrains.kotlin.lexer.KtTokens.ELSE_KEYWORD
import org.jetbrains.kotlin.lexer.KtTokens.ELVIS
import org.jetbrains.kotlin.lexer.KtTokens.LBRACE
import org.jetbrains.kotlin.lexer.KtTokens.LPAR
import org.jetbrains.kotlin.lexer.KtTokens.MINUS
import org.jetbrains.kotlin.lexer.KtTokens.MUL
import org.jetbrains.kotlin.lexer.KtTokens.OPERATIONS
import org.jetbrains.kotlin.lexer.KtTokens.OROR
import org.jetbrains.kotlin.lexer.KtTokens.PERC
import org.jetbrains.kotlin.lexer.KtTokens.PLUS
import org.jetbrains.kotlin.lexer.KtTokens.RPAR
import org.jetbrains.kotlin.lexer.KtTokens.SAFE_ACCESS
import org.jetbrains.kotlin.psi.psiUtil.nextLeaf
import org.jetbrains.kotlin.psi.psiUtil.prevLeaf

class ChainWrappingRule : Rule("chain-wrapping") {

    private val alwaysSameLineTokens = TokenSet.create(ANDAND, OROR)
    private val sometimesSameLineTokens = TokenSet.create(MUL, PLUS, MINUS, DIV, PERC)
    private val nextLineTokens = TokenSet.create(DOT, SAFE_ACCESS, ELVIS)
    private val noSpaceAroundTokens = TokenSet.create(DOT, SAFE_ACCESS)
    private val sameLineRuleOverridingTokens = TokenSet.orSet(OPERATIONS, TokenSet.create(LPAR, RPAR, COMMA, LBRACE, ELSE_KEYWORD))

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
        if (elementType.mustNotEndALineOfCode()) {
            val nextLeaf = node.psi.nextLeaf(true)
            if (nextLeaf is PsiWhiteSpaceImpl && nextLeaf.isLineBreak()) {
                emit(node.startOffset, "Line must not end with \"${node.text}\"", true)
                if (autoCorrect) {
                    val prevLeaf = node.psi.prevLeaf(true)
                    if (prevLeaf is PsiWhiteSpaceImpl) {
                        prevLeaf.rawReplaceWithText(nextLeaf.text)
                    } else {
                        (node.psi as LeafPsiElement).rawInsertBeforeMe(PsiWhiteSpaceImpl(nextLeaf.text))
                    }
                    if (elementType.mustNotBeSurroundBySpaces()) {
                        nextLeaf.node.treeParent.removeChild(nextLeaf.node)
                    } else {
                        nextLeaf.rawReplaceWithText(" ")
                    }
                }
            }
        } else if (elementType.mightNotBeAllowedToStartALineOfCode()) {
            val prevLeaf = node.psi.prevLeaf(true)
            if (
                prevLeaf is PsiWhiteSpaceImpl &&
                prevLeaf.isLineBreak() &&
                (elementType.mustNotStartALineOfCode() ||
                    prevLeaf.prevLeafIgnoringWhitespaceAndComments().isNotRuleOverridingToken()) &&
                // LeafPsiElement->KtOperationReferenceExpression->KtPrefixExpression->KtWhenConditionWithExpression
                node.treeParent?.treeParent?.treeParent?.elementType != KtNodeTypes.WHEN_CONDITION_EXPRESSION
            ) {
                emit(node.startOffset, "Line must not begin with \"${node.text}\"", true)
                if (autoCorrect) {
                    val nextLeaf = node.psi.nextLeaf(true)
                    if (nextLeaf is PsiWhiteSpaceImpl) {
                        nextLeaf.rawReplaceWithText(prevLeaf.text)
                    } else {
                        (node.psi as LeafPsiElement).rawInsertAfterMe(PsiWhiteSpaceImpl(prevLeaf.text))
                    }
                    if (elementType.mustNotBeSurroundBySpaces()) {
                        prevLeaf.node.treeParent.removeChild(prevLeaf.node)
                    } else {
                        prevLeaf.rawReplaceWithText(" ")
                    }
                }
            }
        }
    }

    private fun IElementType.mustNotEndALineOfCode() =
        nextLineTokens.contains(this)

    private fun IElementType.mustNotBeSurroundBySpaces() =
        noSpaceAroundTokens.contains(this)

    private fun IElementType.mightNotBeAllowedToStartALineOfCode() =
        this.mustNotStartALineOfCode() || sometimesSameLineTokens.contains(this)

    private fun IElementType.mustNotStartALineOfCode() =
        alwaysSameLineTokens.contains(this)

    private fun PsiElement.isLineBreak() =
        this.textContains('\n')

    private fun PsiElement.prevLeafIgnoringWhitespaceAndComments() =
        this.prevLeaf { it.node.elementType != KtTokens.WHITE_SPACE && !it.isPartOf(PsiComment::class) }

    private fun PsiElement?.isNotRuleOverridingToken(): Boolean {
        val type = this?.node?.elementType
        return type != null && !sameLineRuleOverridingTokens.contains(type)
    }
}
