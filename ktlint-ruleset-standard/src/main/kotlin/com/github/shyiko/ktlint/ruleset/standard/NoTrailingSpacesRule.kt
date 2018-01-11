package com.github.shyiko.ktlint.ruleset.standard

import com.github.shyiko.ktlint.core.Rule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.com.intellij.psi.util.PsiTreeUtil

class NoTrailingSpacesRule : Rule("no-trailing-spaces") {

    override fun visit(node: ASTNode, autoCorrect: Boolean,
            emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit) {
        if (node is PsiWhiteSpace) {
            val lines = node.getText().split("\n")
            if (lines.size > 1) {
                checkForTrailingSpaces(lines.head(), node.startOffset, emit)
                if (autoCorrect) {
                    (node as LeafPsiElement).rawReplaceWithText("\n".repeat(lines.size - 1) + lines.last())
                }
            } else if (PsiTreeUtil.nextLeaf(node) == null /* eof */) {
                checkForTrailingSpaces(lines, node.startOffset, emit)
                if (autoCorrect) {
                    (node as LeafPsiElement).rawReplaceWithText("\n".repeat(lines.size - 1))
                }
            }
        }
    }

    private fun checkForTrailingSpaces(lines: List<String>, offset: Int,
            emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit) {
        var violationOffset = offset
        return lines.forEach { line ->
            if (!line.isEmpty()) {
                emit(violationOffset, "Trailing space(s)", true)
            }
            violationOffset += line.length + 1
        }
    }
}
