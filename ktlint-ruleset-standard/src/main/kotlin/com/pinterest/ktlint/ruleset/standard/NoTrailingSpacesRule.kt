package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.ast.nextLeaf
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.PsiComment
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement

class NoTrailingSpacesRule : Rule("no-trailing-spaces") {

    override fun visit(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit
    ) {
        if (node is PsiWhiteSpace || node is PsiComment) {
            val lines = node.text.split("\n")
            var violated = false
            var violationOffset = node.startOffset
            lines
                .head()
                .forEach { line ->
                    if (line.hasTrailingSpace()) {
                        emit(violationOffset, "Trailing space(s)", true)
                        violated = true
                    }
                    violationOffset += line.length + 1
                }
            when {
                node is PsiWhiteSpace && node.nextLeaf() != null ->
                    // Ignore the last line as it contains the indentation of the next element
                    Unit
                lines.last().hasTrailingSpace() -> {
                    emit(violationOffset, "Trailing space(s)", true)
                    violated = true
                }
            }
            if (violated && autoCorrect) {
                val modifiedLines = lines.joinToString(separator = "\n") { it.trimEnd() }
                (node as LeafPsiElement).rawReplaceWithText(modifiedLines)
            }
        }
    }

    private fun String.hasTrailingSpace() =
        takeLast(1) == " "
}
