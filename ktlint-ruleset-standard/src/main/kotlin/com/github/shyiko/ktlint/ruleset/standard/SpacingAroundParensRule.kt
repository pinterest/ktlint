package com.github.shyiko.ktlint.ruleset.standard

import com.github.shyiko.ktlint.core.Rule
import com.github.shyiko.ktlint.core.ast.ElementType.IDENTIFIER
import com.github.shyiko.ktlint.core.ast.ElementType.LPAR
import com.github.shyiko.ktlint.core.ast.ElementType.RPAR
import com.github.shyiko.ktlint.core.ast.ElementType.SUPER_KEYWORD
import com.github.shyiko.ktlint.core.ast.ElementType.VALUE_ARGUMENT_LIST
import com.github.shyiko.ktlint.core.ast.ElementType.VALUE_PARAMETER_LIST
import com.github.shyiko.ktlint.core.ast.nextLeaf
import com.github.shyiko.ktlint.core.ast.prevLeaf
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace

/**
 * Ensures there are no extra spaces around parentheses.
 *
 * See https://kotlinlang.org/docs/reference/coding-conventions.html, "Horizontal Whitespace"
 */
class SpacingAroundParensRule : Rule("paren-spacing") {

    override fun visit(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit
    ) {
        if (node.elementType == LPAR || node.elementType == RPAR) {
            val prevLeaf = node.prevLeaf()
            val nextLeaf = node.nextLeaf()
            val spacingBefore = if (node.elementType == LPAR) {
                prevLeaf is PsiWhiteSpace && !prevLeaf.textContains('\n') &&
                    (
                        prevLeaf.prevLeaf()?.elementType == IDENTIFIER ||
                            // Super keyword needs special-casing
                            prevLeaf.prevLeaf()?.elementType == SUPER_KEYWORD
                        ) && (
                    node.treeParent?.elementType == VALUE_PARAMETER_LIST ||
                        node.treeParent?.elementType == VALUE_ARGUMENT_LIST
                    )
            } else {
                prevLeaf is PsiWhiteSpace && !prevLeaf.textContains('\n') &&
                    prevLeaf.prevLeaf()?.elementType != LPAR
            }
            val spacingAfter = if (node.elementType == LPAR) {
                nextLeaf is PsiWhiteSpace && (
                    !nextLeaf.textContains('\n') ||
                        nextLeaf.nextLeaf()?.elementType == RPAR
                    )
            } else {
                nextLeaf is PsiWhiteSpace && !nextLeaf.textContains('\n') &&
                    nextLeaf.nextLeaf()?.elementType == RPAR
            }
            when {
                spacingBefore && spacingAfter -> {
                    emit(node.startOffset, "Unexpected spacing around \"${node.text}\"", true)
                    if (autoCorrect) {
                        prevLeaf!!.treeParent.removeChild(prevLeaf)
                        nextLeaf!!.treeParent.removeChild(nextLeaf)
                    }
                }
                spacingBefore -> {
                    emit(prevLeaf!!.startOffset, "Unexpected spacing before \"${node.text}\"", true)
                    if (autoCorrect) {
                        prevLeaf.treeParent.removeChild(prevLeaf)
                    }
                }
                spacingAfter -> {
                    emit(node.startOffset + 1, "Unexpected spacing after \"${node.text}\"", true)
                    if (autoCorrect) {
                        nextLeaf!!.treeParent.removeChild(nextLeaf)
                    }
                }
            }
        }
    }
}
