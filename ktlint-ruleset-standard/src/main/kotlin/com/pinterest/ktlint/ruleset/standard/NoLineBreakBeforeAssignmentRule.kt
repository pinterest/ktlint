package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.ast.ElementType.EQ
import com.pinterest.ktlint.core.ast.ElementType.REGULAR_STRING_PART
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement

class NoLineBreakBeforeAssignmentRule : Rule("no-line-break-before-assignment") {

    override fun visit(node: ASTNode, autoCorrect: Boolean, emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit) {
        if (node.elementType == EQ) {
            val prevElement = node.treePrev?.psi
            if (prevElement is PsiWhiteSpace && prevElement.text.contains("\n")) {
                emit(node.startOffset, "Line break before assignment is not allowed", true)
                if (autoCorrect) {
                    val leaf = node.treeNext?.psi as? LeafPsiElement
                    if (leaf != null) {
                        leaf.rawReplaceWithText(prevElement.text)
                    } else {
                        (node.psi as LeafPsiElement).rawInsertAfterMe(
                            LeafPsiElement(REGULAR_STRING_PART, prevElement.text)
                        )
                    }
                    (prevElement as LeafPsiElement).rawReplaceWithText(" ")
                }
            }
        }
    }
}
