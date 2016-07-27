package com.github.shyiko.ktlint.rule

import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.PsiWhiteSpaceImpl
import org.jetbrains.kotlin.psi.KtAnnotation
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtClassOrObject

class SpacingAroundColonRule : Rule {

    override fun visit(node: ASTNode, correct: Boolean, emit: (e: RuleViolation) -> Unit) {
        if (node is LeafPsiElement && node.textMatches(":") && !node.isPartOfString()) {
            if (node.isPartOf(KtAnnotation::class) || node.isPartOf(KtAnnotationEntry::class)) {
                // todo: enfore "no spacing"
                return
            }
            val missingSpacingBefore = node.prevSibling !is PsiWhiteSpace && node.parent is KtClassOrObject
            val missingSpacingAfter = node.nextSibling !is PsiWhiteSpace
            when {
                missingSpacingBefore && missingSpacingAfter -> {
                    emit(RuleViolation(node.startOffset,
                        "Missing spacing around \":\"", correct))
                    if (correct) {
                        node.rawInsertBeforeMe(PsiWhiteSpaceImpl(" "))
                        node.rawInsertAfterMe(PsiWhiteSpaceImpl(" "))
                    }
                }
                missingSpacingBefore -> {
                    emit(RuleViolation(node.startOffset,
                        "Missing spacing before \":\"", correct))
                    if (correct) {
                        node.rawInsertBeforeMe(PsiWhiteSpaceImpl(" "))
                    }
                }
                missingSpacingAfter -> {
                    emit(RuleViolation(node.startOffset + 1,
                        "Missing spacing after \":\"", correct))
                    if (correct) {
                        node.rawInsertAfterMe(PsiWhiteSpaceImpl(" "))
                    }
                }
            }
        }
    }
}
