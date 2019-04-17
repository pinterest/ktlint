package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.ast.isPartOfString
import com.pinterest.ktlint.core.ast.nextLeaf
import com.pinterest.ktlint.core.ast.prevLeaf
import com.pinterest.ktlint.core.ast.upsertWhitespaceAfterMe
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement

class SpacingAroundCommaRule : Rule("comma-spacing") {

    override fun visit(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit
    ) {
        if (node is LeafPsiElement && node.textMatches(",") && !node.isPartOfString()) {
            val prevLeaf = node.prevLeaf()
            if (prevLeaf is PsiWhiteSpace) {
                emit(prevLeaf.startOffset, "Unexpected spacing before \"${node.text}\"", true)
                if (autoCorrect) {
                    prevLeaf.node.treeParent.removeChild(prevLeaf.node)
                }
            }
            if (node.nextLeaf() !is PsiWhiteSpace) {
                emit(node.startOffset + 1, "Missing spacing after \"${node.text}\"", true)
                if (autoCorrect) {
                    node.upsertWhitespaceAfterMe(" ")
                }
            }
        }
    }
}
