package com.github.shyiko.ktlint.rule

import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.PsiWhiteSpaceImpl
import org.jetbrains.kotlin.com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType

class SpacingAroundCurlyRule : Rule {

    override fun visit(node: ASTNode, correct: Boolean, emit: (e: RuleViolation) -> Unit) {
        if (node is LeafPsiElement && !node.isPartOfString()) {
            val prevLeaf = PsiTreeUtil.prevLeaf(node, true)
            val nextLeaf = PsiTreeUtil.nextLeaf(node, true)
            if (node.textMatches("{")) {
                val spacingBefore = prevLeaf is PsiWhiteSpace
                val spacingAfter = nextLeaf is PsiWhiteSpace
                val lambdaExpression = node.getNonStrictParentOfType(KtLambdaExpression::class.java) != null
                when {
                    !spacingBefore && !spacingAfter && !lambdaExpression -> {
                        emit(RuleViolation(node.startOffset,
                            "Missing spacing around \"{\"", correct))
                        if (correct) {
                            node.rawInsertBeforeMe(PsiWhiteSpaceImpl(" "))
                            node.rawInsertAfterMe(PsiWhiteSpaceImpl(" "))
                        }
                    }
                    !spacingBefore && !lambdaExpression -> {
                        emit(RuleViolation(node.startOffset,
                            "Missing spacing before \"{\"", correct))
                        if (correct) {
                            node.rawInsertBeforeMe(PsiWhiteSpaceImpl(" "))
                        }
                    }
                    !spacingAfter && nextLeaf != null && !nextLeaf.textMatches("}") /*&& block.children.size > 3*/ -> {
                        emit(RuleViolation(node.startOffset + 1,
                            "Missing spacing after \"{\"", correct))
                        if (correct) {
                            node.rawInsertAfterMe(PsiWhiteSpaceImpl(" "))
                        }
                    }
                }
            } else
            if (node.textMatches("}") && node.prevSibling !is PsiWhiteSpace &&
                    !(node.prevSibling /* KtBlockExpression */).children.isEmpty()) {
                emit(RuleViolation(node.startOffset, "Missing spacing before \"}\"", correct))
                if (correct) {
                    node.rawInsertBeforeMe(PsiWhiteSpaceImpl(" "))
                }
            }
        }
    }

}
