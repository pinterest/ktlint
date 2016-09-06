package com.gihub.shyiko.ktlint.ruleset.standard

import com.github.shyiko.ktlint.core.Rule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.PsiWhiteSpaceImpl
import org.jetbrains.kotlin.com.intellij.psi.util.PsiTreeUtil

class SpacingAfterCommaRule : Rule("comma-spacing") {

    override fun visit(node: ASTNode, autoCorrect: Boolean,
            emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit) {
        if (node is LeafPsiElement && (node.textMatches(",") || node.textMatches(";")) && !node.isPartOfString() &&
            PsiTreeUtil.nextLeaf(node) !is PsiWhiteSpace) {
            emit(node.startOffset + 1, "Missing spacing after \"${node.text}\"", true)
            if (autoCorrect) {
                node.rawInsertAfterMe(PsiWhiteSpaceImpl(" "))
            }
        }
    }

}
