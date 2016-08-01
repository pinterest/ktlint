package com.github.shyiko.ktlint.rule

import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.PsiWhiteSpaceImpl
import org.jetbrains.kotlin.com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtLambdaExpression

class SpacingAroundCurlyRule : Rule {

    override fun visit(node: ASTNode, correct: Boolean, emit: (e: RuleViolation) -> Unit) {
        if (node is LeafPsiElement && !node.isPartOfString()) {
            val prevLeaf = PsiTreeUtil.prevLeaf(node, true)
            val nextLeaf = PsiTreeUtil.nextLeaf(node, true)
            val spacingBefore: Boolean
            val spacingAfter: Boolean
            if (node.textMatches("{")) {
                spacingBefore = prevLeaf is PsiWhiteSpace || (prevLeaf?.node?.elementType == KtTokens.LPAR &&
                    (node.parent is KtLambdaExpression || node.parent.parent is KtLambdaExpression))
                spacingAfter = nextLeaf is PsiWhiteSpace || nextLeaf?.node?.elementType == KtTokens.RBRACE
            } else
            if (node.textMatches("}")) {
                spacingBefore = prevLeaf is PsiWhiteSpace || prevLeaf?.node?.elementType == KtTokens.LBRACE
                val nextElementType = nextLeaf?.node?.elementType
                spacingAfter = nextLeaf is PsiWhiteSpace || nextLeaf == null || nextElementType == KtTokens.DOT ||
                    nextElementType == KtTokens.COMMA || nextElementType == KtTokens.RPAR
            } else {
                return
            }
            when {
                !spacingBefore && !spacingAfter -> {
                    emit(RuleViolation(node.startOffset,
                        "Missing spacing around \"${node.text}\"", correct))
                    if (correct) {
                        node.rawInsertBeforeMe(PsiWhiteSpaceImpl(" "))
                        node.rawInsertAfterMe(PsiWhiteSpaceImpl(" "))
                    }
                }
                !spacingBefore -> {
                    emit(RuleViolation(node.startOffset,
                        "Missing spacing before \"${node.text}\"", correct))
                    if (correct) {
                        node.rawInsertBeforeMe(PsiWhiteSpaceImpl(" "))
                    }
                }
                !spacingAfter -> {
                    emit(RuleViolation(node.startOffset + 1,
                        "Missing spacing after \"${node.text}\"", correct))
                    if (correct) {
                        node.rawInsertAfterMe(PsiWhiteSpaceImpl(" "))
                    }
                }
            }
        }
    }

}
