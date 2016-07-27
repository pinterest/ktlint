package com.github.shyiko.ktlint.rule

import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement

class NoConsecutiveBlankLinesRule : Rule {

    override fun visit(node: ASTNode, correct: Boolean, emit: (e: RuleViolation) -> Unit) {
        if (node is PsiWhiteSpace) {
            val split = node.getText().split("\n")
            if (split.size > 3) {
                emit(RuleViolation(node.startOffset + split[0].length + split[1].length + 2,
                    "Needless blank line(s)", correct))
                if (correct) {
                    (node as LeafPsiElement).replaceWithText("${split.first()}\n\n${split.last()}")
                }
            }
        }
    }

}
