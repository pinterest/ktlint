package com.github.shyiko.ktlint.rule

import com.github.shyiko.ktlint.head
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.com.intellij.psi.util.PsiTreeUtil

class NoTrailingSpacesRule : Rule {

    override fun visit(node: ASTNode, correct: Boolean, emit: (e: RuleViolation) -> Unit) {
        if (node is PsiWhiteSpace) {
            val split = node.getText().split("\n")
            if (split.size > 1) {
                checkForTrailingSpaces(split.head(), node.startOffset, correct, emit)
                if (correct) {
                    (node as LeafPsiElement).replaceWithText("\n".repeat(split.size - 1) + split.last())
                }
            } else
            if (PsiTreeUtil.nextLeaf(node) == null /* eof */) {
                checkForTrailingSpaces(split, node.startOffset, correct, emit)
                if (correct) {
                    (node as LeafPsiElement).replaceWithText("\n".repeat(split.size - 1))
                }
            }
        }
    }

    private fun checkForTrailingSpaces(split: List<String>, offset: Int, correct: Boolean,
        emit: (e: RuleViolation) -> Unit) {
        var violationOffset = offset
        return split.forEach {
            if (!it.isEmpty()) {
                emit(RuleViolation(violationOffset, "Trailing space(s)", correct))
            }
            violationOffset += it.length + 1
        }
    }

}
