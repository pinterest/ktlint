package com.github.shyiko.ktlint.ruleset.standard

import com.github.shyiko.ktlint.core.Rule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement

class NoBlankLineBeforeRbraceRule : Rule("no-blank-line-before-rbrace") {
    override fun visit(node: ASTNode, autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit) {
        if (node is PsiWhiteSpace) {
            val split = node.getText().split("\n")

            if (split.size > 2 && isNextElementRbrace(node)) {
                emit(node.startOffset + split[0].length + split[1].length + 1,
                    "Needless blank line(s)", true)
                if (autoCorrect) {
                    (node as LeafPsiElement).replaceWithText("${split.first()}\n${split.last()}")
                }
            }
        }
    }

    private fun isNextElementRbrace(node: ASTNode) =
        node.treeNext?.elementType?.index ?: 0 == 147.toShort()
}
