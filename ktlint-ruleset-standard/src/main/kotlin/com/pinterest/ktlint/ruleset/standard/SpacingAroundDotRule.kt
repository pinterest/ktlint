package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.ast.isPartOfComment
import com.pinterest.ktlint.core.ast.isPartOfString
import com.pinterest.ktlint.core.ast.nextLeaf
import com.pinterest.ktlint.core.ast.prevLeaf
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement

public class SpacingAroundDotRule : Rule("dot-spacing") {

    override fun beforeVisitChildNodes(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        if (node is LeafPsiElement && node.textMatches(".") && !node.isPartOfString() && !node.isPartOfComment()) {
            val prevLeaf = node.prevLeaf()
            if (prevLeaf is PsiWhiteSpace && !prevLeaf.textContains('\n')) {
                emit(prevLeaf.startOffset, "Unexpected spacing before \"${node.text}\"", true)
                if (autoCorrect) {
                    prevLeaf.node.treeParent.removeChild(prevLeaf.node)
                }
            }
            val nextLeaf = node.nextLeaf()
            if (nextLeaf is PsiWhiteSpace) {
                emit(nextLeaf.startOffset, "Unexpected spacing after \"${node.text}\"", true)
                if (autoCorrect) {
                    nextLeaf.node.treeParent.removeChild(nextLeaf.node)
                }
            }
        }
    }
}
