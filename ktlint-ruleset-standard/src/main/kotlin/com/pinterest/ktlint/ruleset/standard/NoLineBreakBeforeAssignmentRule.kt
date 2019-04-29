package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.core.Issue
import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.ast.ElementType.EQ
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement

class NoLineBreakBeforeAssignmentRule : Rule("no-line-break-before-assignment") {

    override fun visit(node: ASTNode, autoCorrect: Boolean, emit: (issue: Issue) -> Unit) {
        if (node.elementType == EQ) {
            val prevElement = node.treePrev?.psi
            if (prevElement is PsiWhiteSpace && prevElement.text.contains("\n")) {
                emit(Issue(node.startOffset, "Line break before assignment is not allowed", true))
                if (autoCorrect) {
                    (node.treeNext?.psi as LeafPsiElement).rawReplaceWithText(prevElement.text)
                    (prevElement as LeafPsiElement).rawReplaceWithText(" ")
                }
            }
        }
    }
}
