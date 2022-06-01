package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.ast.ElementType.CALLABLE_REFERENCE_EXPRESSION
import com.pinterest.ktlint.core.ast.ElementType.CLASS_LITERAL_EXPRESSION
import com.pinterest.ktlint.core.ast.ElementType.COLONCOLON
import com.pinterest.ktlint.core.ast.isPartOf
import com.pinterest.ktlint.core.ast.nextLeaf
import com.pinterest.ktlint.core.ast.prevLeaf
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement

class SpacingAroundDoubleColonRule : Rule("double-colon-spacing") {
    override fun visit(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit
    ) {
        if (node.elementType == COLONCOLON) {
            val prevLeaf = node.prevLeaf()
            val nextLeaf = node.nextLeaf()

            var removeSingleWhiteSpace = false
            val spacingBefore = when {
                node.isPartOf(CLASS_LITERAL_EXPRESSION) && prevLeaf is PsiWhiteSpace -> true // Clazz::class
                node.isPartOf(CALLABLE_REFERENCE_EXPRESSION) && prevLeaf is PsiWhiteSpace -> // String::length, ::isOdd
                    if (node.treePrev == null) { // compose(length, ::isOdd), val predicate = ::isOdd
                        removeSingleWhiteSpace = true
                        !prevLeaf.textContains('\n') && prevLeaf.psi.textLength > 1
                    } else { // String::length, List<String>::isEmpty
                        !prevLeaf.textContains('\n')
                    }
                else -> false
            }

            val spacingAfter = nextLeaf is PsiWhiteSpace
            when {
                spacingBefore && spacingAfter -> {
                    emit(node.startOffset, "Unexpected spacing around \"${node.text}\"", true)
                    if (autoCorrect) {
                        prevLeaf!!.removeSelf(removeSingleWhiteSpace)
                        nextLeaf!!.treeParent.removeChild(nextLeaf)
                    }
                }
                spacingBefore -> {
                    emit(prevLeaf!!.startOffset, "Unexpected spacing before \"${node.text}\"", true)
                    if (autoCorrect) {
                        prevLeaf.removeSelf(removeSingleWhiteSpace)
                    }
                }
                spacingAfter -> {
                    emit(nextLeaf!!.startOffset, "Unexpected spacing after \"${node.text}\"", true)
                    if (autoCorrect) {
                        nextLeaf.treeParent.removeChild(nextLeaf)
                    }
                }
            }
        }
    }

    private fun ASTNode.removeSelf(removeSingleWhiteSpace: Boolean) {
        if (removeSingleWhiteSpace) {
            (this as LeafPsiElement).rawReplaceWithText(text.substring(0, textLength - 1))
        } else {
            treeParent.removeChild(this)
        }
    }
}
