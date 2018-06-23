package com.github.shyiko.ktlint.ruleset.standard

import com.github.shyiko.ktlint.core.Rule
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.psiUtil.startOffset

class SpacingAroundParensRule : Rule("paren-spacing") {

    override fun visit(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit
    ) {
        if (node.elementType == KtTokens.LPAR || node.elementType == KtTokens.RPAR) {
            val prevLeaf = PsiTreeUtil.prevLeaf(node.psi, true)
            val nextLeaf = PsiTreeUtil.nextLeaf(node.psi, true)
            val spacingBefore = if (node.elementType == KtTokens.LPAR) {
                prevLeaf is PsiWhiteSpace && !prevLeaf.textContains('\n') &&
                PsiTreeUtil.prevLeaf(prevLeaf, true)?.node?.elementType == KtTokens.IDENTIFIER && (
                    node.treeParent?.elementType == KtNodeTypes.VALUE_PARAMETER_LIST ||
                    node.treeParent?.elementType == KtNodeTypes.VALUE_ARGUMENT_LIST
                )
            } else {
                prevLeaf is PsiWhiteSpace && !prevLeaf.textContains('\n') &&
                PsiTreeUtil.prevLeaf(prevLeaf, true)?.node?.elementType != KtTokens.LPAR
            }
            val spacingAfter = if (node.elementType == KtTokens.LPAR) {
                nextLeaf is PsiWhiteSpace && (
                    !nextLeaf.textContains('\n') ||
                    PsiTreeUtil.nextLeaf(nextLeaf, true)?.node?.elementType == KtTokens.RPAR
                )
            } else {
                nextLeaf is PsiWhiteSpace && !nextLeaf.textContains('\n') &&
                PsiTreeUtil.nextLeaf(nextLeaf, true)?.node?.elementType == KtTokens.RPAR
            }
            when {
                spacingBefore && spacingAfter -> {
                    emit(node.startOffset, "Unexpected spacing around \"${node.text}\"", true)
                    if (autoCorrect) {
                        prevLeaf!!.node.treeParent.removeChild(prevLeaf.node)
                        nextLeaf!!.node.treeParent.removeChild(nextLeaf.node)
                    }
                }
                spacingBefore -> {
                    emit(prevLeaf!!.startOffset, "Unexpected spacing before \"${node.text}\"", true)
                    if (autoCorrect) {
                        prevLeaf.node.treeParent.removeChild(prevLeaf.node)
                    }
                }
                spacingAfter -> {
                    emit(node.startOffset + 1, "Unexpected spacing after \"${node.text}\"", true)
                    if (autoCorrect) {
                        nextLeaf!!.node.treeParent.removeChild(nextLeaf.node)
                    }
                }
            }
        }
    }
}
