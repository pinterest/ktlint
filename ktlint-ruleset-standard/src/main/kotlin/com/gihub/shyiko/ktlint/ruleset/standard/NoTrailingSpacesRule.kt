package com.gihub.shyiko.ktlint.ruleset.standard

import com.github.shyiko.ktlint.core.Rule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.com.intellij.psi.util.PsiTreeUtil

class NoTrailingSpacesRule : Rule("no-trailing-spaces") {

    override fun visit(node: ASTNode, autoCorrect: Boolean,
            emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit) {
        if (node is PsiWhiteSpace) {
            val split = node.getText().split("\n")
            if (split.size > 1) {
                checkForTrailingSpaces(split.head(), node.startOffset, emit)
                if (autoCorrect) {
                    (node as LeafPsiElement).replaceWithText("\n".repeat(split.size - 1) + split.last())
                }
            } else
            if (PsiTreeUtil.nextLeaf(node) == null /* eof */) {
                checkForTrailingSpaces(split, node.startOffset, emit)
                if (autoCorrect) {
                    (node as LeafPsiElement).replaceWithText("\n".repeat(split.size - 1))
                }
            }
        }
    }

    private fun checkForTrailingSpaces(split: List<String>, offset: Int,
            emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit) {
        var violationOffset = offset
        return split.forEach {
            if (!it.isEmpty()) {
                emit(violationOffset, "Trailing space(s)", true)
            }
            violationOffset += it.length + 1
        }
    }

}
