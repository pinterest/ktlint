package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.ast.ElementType.KDOC_MARKDOWN_LINK
import com.pinterest.ktlint.core.ast.ElementType.KDOC_TAG
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement

public class NoMultipleSpacesRule : Rule("no-multi-spaces") {
    override fun beforeVisitChildNodes(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        node
            .takeIf { node is PsiWhiteSpace }
            .takeUnless { node.isIndentation() }
            .takeIf { node.textLength > 1 }
            .takeUnless { node.isPossibleAlignmentOfKdocTag() }
            ?.let {
                emit(node.startOffset + 1, "Unnecessary long whitespace", true)
                if (autoCorrect) {
                    (node as LeafPsiElement).rawReplaceWithText(" ")
                }
            }
    }

    private fun ASTNode.isIndentation() = this.text.startsWith("\n")

    // allow multiple spaces in KDoc in case of KDOC_TAG for alignment, e.g.
    // @param foo      stuff
    // @param foobar   stuff2
    private fun ASTNode.isPossibleAlignmentOfKdocTag() =
        treePrev?.elementType == KDOC_MARKDOWN_LINK && treeParent?.elementType == KDOC_TAG
}
