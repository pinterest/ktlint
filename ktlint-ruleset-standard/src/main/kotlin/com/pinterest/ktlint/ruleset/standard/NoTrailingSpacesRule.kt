package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.ast.ElementType.WHITE_SPACE
import com.pinterest.ktlint.core.ast.isPartOfComment
import com.pinterest.ktlint.core.ast.nextLeaf
import com.pinterest.ktlint.core.ast.parent
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.kdoc.psi.api.KDoc

class NoTrailingSpacesRule : Rule("no-trailing-spaces") {

    override fun visit(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit
    ) {
        if (node.isPartOfKDoc()) {
            if (node.elementType == WHITE_SPACE && node.hasTrailingSpacesBeforeNewline()) {
                emit(node.startOffset, "Trailing space(s)", true)
                if (autoCorrect) {
                    node.removeTrailingSpacesBeforeNewline()
                }
            }
        } else if (node.elementType == WHITE_SPACE || node.isPartOfComment()) {
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

    private fun ASTNode.isPartOfKDoc() = parent({ it.psi is KDoc }, strict = false) != null

    private fun ASTNode.hasTrailingSpacesBeforeNewline() =
        text.contains(
            regex = Regex("\\s+\\n")
        )

    private fun ASTNode.removeTrailingSpacesBeforeNewline() {
        val newText = text.replace(
            regex = Regex("\\s+\\n"),
            replacement = "\n"
        )
        (this as LeafPsiElement).replaceWithText(newText)
    }

    private fun String.hasTrailingSpace() =
        takeLast(1) == " "
}
