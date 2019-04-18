package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.core.Rule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement

class NoMultipleSpacesRule : Rule("no-multi-spaces") {

    override fun visit(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit
    ) {
        if (node is PsiWhiteSpace && !node.textContains('\n') && node.getTextLength() > 1) {
            emit(node.startOffset + 1, "Unnecessary space(s)", true)
            if (autoCorrect) {
                (node as LeafPsiElement).rawReplaceWithText(" ")
            }
        }
    }
}
