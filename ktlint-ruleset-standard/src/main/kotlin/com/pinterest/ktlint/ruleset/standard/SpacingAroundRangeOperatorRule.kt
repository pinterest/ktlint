package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.core.Issue
import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.ast.ElementType.RANGE
import com.pinterest.ktlint.core.ast.nextLeaf
import com.pinterest.ktlint.core.ast.prevLeaf
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace

class SpacingAroundRangeOperatorRule : Rule("range-spacing") {

    override fun visit(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (issue: Issue) -> Unit
    ) {
        if (node.elementType == RANGE) {
            val prevLeaf = node.prevLeaf()
            val nextLeaf = node.nextLeaf()
            when {
                prevLeaf is PsiWhiteSpace && nextLeaf is PsiWhiteSpace -> {
                    emit(Issue(node.startOffset, "Unexpected spacing around \"..\"", true))
                    if (autoCorrect) {
                        prevLeaf.node.treeParent.removeChild(prevLeaf.node)
                        nextLeaf.node.treeParent.removeChild(nextLeaf.node)
                    }
                }
                prevLeaf is PsiWhiteSpace -> {
                    emit(Issue(prevLeaf.node.startOffset, "Unexpected spacing before \"..\"", true))
                    if (autoCorrect) {
                        prevLeaf.node.treeParent.removeChild(prevLeaf.node)
                    }
                }
                nextLeaf is PsiWhiteSpace -> {
                    emit(Issue(nextLeaf.node.startOffset, "Unexpected spacing after \"..\"", true))
                    if (autoCorrect) {
                        nextLeaf.node.treeParent.removeChild(nextLeaf.node)
                    }
                }
            }
        }
    }
}
