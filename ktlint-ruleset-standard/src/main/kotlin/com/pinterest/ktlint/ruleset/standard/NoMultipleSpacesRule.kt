package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.ast.ElementType.KDOC_MARKDOWN_LINK
import com.pinterest.ktlint.core.ast.ElementType.KDOC_TAG
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement

class NoMultipleSpacesRule : Rule("no-multi-spaces") {

    override fun visit(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit
    ) {
        if (node is PsiWhiteSpace && !node.textContains('\n') && node.getTextLength() > 1 &&
            // allow multiple spaces in KDoc in case of KDOC_TAG for alignment, e.g.
            // @param foo      stuff
            // @param foobar   stuff2
            !(node.treePrev?.elementType == KDOC_MARKDOWN_LINK && node.treeParent?.elementType == KDOC_TAG)
        ) {
            emit(node.startOffset + 1, "Unnecessary space(s)", true)
            if (autoCorrect) {
                (node as LeafPsiElement).rawReplaceWithText(" ")
            }
        }
    }
}
