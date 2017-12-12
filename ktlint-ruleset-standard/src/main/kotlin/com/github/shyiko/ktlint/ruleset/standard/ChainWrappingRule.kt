package com.github.shyiko.ktlint.ruleset.standard

import com.github.shyiko.ktlint.core.Rule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.PsiWhiteSpaceImpl
import org.jetbrains.kotlin.com.intellij.psi.tree.TokenSet
import org.jetbrains.kotlin.lexer.KtTokens.DOT
import org.jetbrains.kotlin.lexer.KtTokens.ELVIS
import org.jetbrains.kotlin.lexer.KtTokens.SAFE_ACCESS
import org.jetbrains.kotlin.psi.psiUtil.nextLeaf
import org.jetbrains.kotlin.psi.psiUtil.prevLeaf

class ChainWrappingRule : Rule("chain-wrapping") {

    // *,+,-,/,% position differ in
    // https://github.com/yole/kotlin-style-guide/issues/9
    // https://android.github.io/kotlin-guides/style.html#where-to-break
    private val sameLineTokens = TokenSet.EMPTY // TokenSet.create(MUL, PLUS, MINUS, DIV, PERC)
    private val nextLineTokens = TokenSet.create(DOT, SAFE_ACCESS, ELVIS)
    private val noSpaceAroundTokens = TokenSet.create(DOT, SAFE_ACCESS)

    override fun visit(node: ASTNode, autoCorrect: Boolean,
            emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit) {
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
        } else if (sameLineTokens.contains(elementType)) {
            val prevLeaf = node.psi.prevLeaf(true)
            if (prevLeaf is PsiWhiteSpaceImpl && prevLeaf.textContains('\n')) {
                emit(node.startOffset, "Line must not begin with \"${node.text}\"", true)
                if (autoCorrect) {
                    val nextLeaf = node.psi.nextLeaf(true)
                    if (nextLeaf is PsiWhiteSpaceImpl) {
                        nextLeaf.rawReplaceWithText(prevLeaf.text)
                    } else {
                        (node.psi as LeafPsiElement).rawInsertAfterMe(PsiWhiteSpaceImpl(prevLeaf.text))
                    }
                    if (noSpaceAroundTokens.contains(elementType)) {
                        prevLeaf.node.treeParent.removeChild(prevLeaf.node)
                    } else {
                        prevLeaf.rawReplaceWithText(" ")
                    }
                }
            }
        }
    }
}
