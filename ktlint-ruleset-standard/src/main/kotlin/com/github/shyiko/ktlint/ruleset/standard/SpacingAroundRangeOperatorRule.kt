package com.github.shyiko.ktlint.ruleset.standard

import com.github.shyiko.ktlint.core.Rule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.lexer.KtTokens

class SpacingAroundRangeOperatorRule : Rule("range-spacing") {

    override fun visit(node: ASTNode, autoCorrect: Boolean,
            emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit) {
        if (node.elementType == KtTokens.RANGE) {
            val prevLeaf = PsiTreeUtil.prevLeaf(node.psi, true)
            val nextLeaf = PsiTreeUtil.nextLeaf(node.psi, true)
            when {
                prevLeaf is PsiWhiteSpace && nextLeaf is PsiWhiteSpace -> {
                    emit(node.startOffset, "Unexpected spacing around \"..\"", true)
                    if (autoCorrect) {
                        prevLeaf.node.treeParent.removeChild(prevLeaf.node)
                        nextLeaf.node.treeParent.removeChild(nextLeaf.node)
                    }
                }
                prevLeaf is PsiWhiteSpace -> {
                    emit(prevLeaf.node.startOffset, "Unexpected spacing before \"..\"", true)
                    if (autoCorrect) {
                        prevLeaf.node.treeParent.removeChild(prevLeaf.node)
                    }
                }
                nextLeaf is PsiWhiteSpace -> {
                    emit(nextLeaf.node.startOffset, "Unexpected spacing after \"..\"", true)
                    if (autoCorrect) {
                        nextLeaf.node.treeParent.removeChild(nextLeaf.node)
                    }
                }
            }
        }
    }
}
