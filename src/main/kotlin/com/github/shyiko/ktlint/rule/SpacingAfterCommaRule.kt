package com.github.shyiko.ktlint.rule

import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.PsiWhiteSpaceImpl
import org.jetbrains.kotlin.com.intellij.psi.util.PsiTreeUtil

class SpacingAfterCommaRule : Rule {

    override fun visit(node: ASTNode, correct: Boolean, emit: (e: RuleViolation) -> Unit) {
        if (node is LeafPsiElement && (node.textMatches(",") || node.textMatches(";")) && !node.isPartOfString() &&
            PsiTreeUtil.nextLeaf(node) !is PsiWhiteSpace) {
            emit(RuleViolation(node.startOffset + 1, "Missing spacing after \"${node.text}\"", correct))
            if (correct) {
                node.rawInsertAfterMe(PsiWhiteSpaceImpl(" "))
            }
        }
    }

}
