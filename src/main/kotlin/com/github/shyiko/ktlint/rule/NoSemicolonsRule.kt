package com.github.shyiko.ktlint.rule

import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.PsiWhiteSpaceImpl
import org.jetbrains.kotlin.com.intellij.psi.util.PsiTreeUtil

class NoSemicolonsRule : Rule {

    override fun visit(node: ASTNode, correct: Boolean, emit: (e: RuleViolation) -> Unit) {
        if (node is LeafPsiElement && node.textMatches(";") && !node.isPartOfString()) {
            val nextLeaf = PsiTreeUtil.nextLeaf(node)
            if (nextLeaf == null /* eof */ ||
                (nextLeaf is PsiWhiteSpace && (nextLeaf.text.contains("\n") ||
                    PsiTreeUtil.nextLeaf(nextLeaf) == null /* \s+ and then eof */))
                ) {
                emit(RuleViolation(node.startOffset, "Unnecessary semicolon", correct))
                if (correct) {
                    node.delete()
                }
            } else if (nextLeaf !is PsiWhiteSpace) {
                // todo: move to a separate rule
                emit(RuleViolation(node.startOffset + 1, "Missing spacing after \";\"", correct))
                if (correct) {
                    node.rawInsertAfterMe(PsiWhiteSpaceImpl(" "))
                }
            }
        }
    }

}
