package com.pinterest.ktlint.ruleset.experimental

import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.ast.ElementType
import com.pinterest.ktlint.core.ast.ElementType.FUN
import com.pinterest.ktlint.core.ast.isPartOf
import com.pinterest.ktlint.core.ast.prevLeaf
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement

class NoFirstLineBlankInMethodBlockRule : Rule("no-first-line-blank-in-method-block-rule") {

    override fun visit(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit
    ) {
        if (node is PsiWhiteSpace && node.textContains('\n') &&
            node.prevLeaf()?.elementType == ElementType.LBRACE && node.isPartOf(FUN)
        ) {
            val split = node.getText().split("\n")
            if (split.size > 2) {
                emit(
                    node.startOffset + 1,
                    "First line in a method block should not be empty", true
                )
                if (autoCorrect) {
                    (node as LeafPsiElement).rawReplaceWithText("${split.first()}\n${split.last()}")
                }
            }
        }
    }
}
