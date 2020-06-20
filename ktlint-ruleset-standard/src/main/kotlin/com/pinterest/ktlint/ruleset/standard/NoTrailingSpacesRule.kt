package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.ast.nextLeaf
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement

class NoTrailingSpacesRule : Rule("no-trailing-spaces") {

    override fun visit(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit
    ) {
        if (node is PsiWhiteSpace) {
            val lines = node.getText().split("\n")
            if (lines.size > 1) {
                val violated = checkForTrailingSpaces(lines.head(), node.startOffset, emit)
                if (violated && autoCorrect) {
                    (node as LeafPsiElement).rawReplaceWithText("\n".repeat(lines.size - 1) + lines.last())
                }
            } else if (node.nextLeaf() == null /* eof */) {
                val violated = checkForTrailingSpaces(lines, node.startOffset, emit)
                if (violated && autoCorrect) {
                    (node as LeafPsiElement).rawReplaceWithText("\n".repeat(lines.size - 1))
                }
            }
        }
    }

    private fun checkForTrailingSpaces(
        lines: List<String>,
        offset: Int,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit
    ): Boolean {
        var violated = false
        var violationOffset = offset
        lines.forEach { line ->
            if (!line.isEmpty()) {
                emit(violationOffset, "Trailing space(s)", true)
                violated = true
            }
            violationOffset += line.length + 1
        }
        return violated
    }
}
