package com.github.shyiko.ktlint.ruleset.standard

import com.github.shyiko.ktlint.core.Rule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.lexer.KtTokens

class NoLineBreakBeforeAssignmentRule : Rule("no-line-break-before-assignment") {

    override fun visit(node: ASTNode, autoCorrect: Boolean, emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit) {
        if (node.elementType == KtTokens.EQ) {
            val prevElement = node.treePrev?.psi
            if (prevElement is PsiWhiteSpace && prevElement.text.contains("\n")) {
                emit(node.startOffset, "Line break before assignment is not allowed", true)
                if (autoCorrect) {
                    (node.treeNext?.psi as LeafPsiElement).rawReplaceWithText(prevElement.text)
                    (prevElement as LeafPsiElement).rawReplaceWithText(" ")
                }
            }
        }
    }
}
