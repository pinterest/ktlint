package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.ast.ElementType
import com.pinterest.ktlint.core.ast.ElementType.CLASS_BODY
import com.pinterest.ktlint.core.ast.ElementType.FUN
import com.pinterest.ktlint.core.ast.isPartOf
import com.pinterest.ktlint.core.ast.prevLeaf
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement

class NoEmptyFirstLineInMethodBlockRule : Rule("no-empty-first-line-in-method-block") {
    override fun visit(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit
    ) {
        if (node is PsiWhiteSpace && node.textContains('\n') &&
            node.prevLeaf()?.elementType == ElementType.LBRACE && node.isPartOf(FUN) &&
            node.treeParent.elementType != CLASS_BODY // fun fn() = object : Builder {\n\n fun stuff() = Unit }
        ) {
            val split = node.getText().split("\n")
            if (split.size > 2) {
                emit(
                    node.startOffset + 1,
                    "First line in a method block should not be empty",
                    true
                )
                if (autoCorrect) {
                    (node as LeafPsiElement).rawReplaceWithText("${split.first()}\n${split.last()}")
                }
            }
        }
    }
}
