package com.github.shyiko.ktlint.rule

import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement

class NoMultipleSpacesRule : Rule {

    override fun visit(node: ASTNode, correct: Boolean, emit: (e: RuleViolation) -> Unit) {
        if (node is PsiWhiteSpace && !node.textContains('\n') && node.getTextLength() > 1) {
            emit(RuleViolation(node.startOffset + 1, "Unnecessary space(s)", correct))
            if (correct) {
                (node as LeafPsiElement).replaceWithText(" ")
            }
        }
    }

}
