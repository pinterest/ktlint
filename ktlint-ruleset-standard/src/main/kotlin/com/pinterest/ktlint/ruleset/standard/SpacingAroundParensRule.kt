package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.ast.ElementType.BLOCK_COMMENT
import com.pinterest.ktlint.core.ast.ElementType.EOL_COMMENT
import com.pinterest.ktlint.core.ast.ElementType.FUNCTION_TYPE
import com.pinterest.ktlint.core.ast.ElementType.IDENTIFIER
import com.pinterest.ktlint.core.ast.ElementType.KDOC_START
import com.pinterest.ktlint.core.ast.ElementType.LPAR
import com.pinterest.ktlint.core.ast.ElementType.PRIMARY_CONSTRUCTOR
import com.pinterest.ktlint.core.ast.ElementType.RPAR
import com.pinterest.ktlint.core.ast.ElementType.SUPER_KEYWORD
import com.pinterest.ktlint.core.ast.ElementType.VALUE_ARGUMENT_LIST
import com.pinterest.ktlint.core.ast.ElementType.VALUE_PARAMETER_LIST
import com.pinterest.ktlint.core.ast.nextLeaf
import com.pinterest.ktlint.core.ast.prevLeaf
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace

/**
 * Ensures there are no extra spaces around parentheses.
 *
 * See https://kotlinlang.org/docs/reference/coding-conventions.html#horizontal-whitespace
 */
class SpacingAroundParensRule : Rule("paren-spacing") {

    override fun beforeVisitChildNodes(
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
                        prevLeaf.prevLeaf()?.elementType == IDENTIFIER &&
                            // val foo: @Composable () -> Unit
                            node.treeParent?.treeParent?.elementType != FUNCTION_TYPE ||
                            // Super keyword needs special-casing
                            prevLeaf.prevLeaf()?.elementType == SUPER_KEYWORD ||
                            prevLeaf.prevLeaf()?.treeParent?.elementType == PRIMARY_CONSTRUCTOR
                        ) &&
                    (
                        node.treeParent?.elementType == VALUE_PARAMETER_LIST ||
                            node.treeParent?.elementType == VALUE_ARGUMENT_LIST
                        )
            } else {
                prevLeaf is PsiWhiteSpace && !prevLeaf.textContains('\n') &&
                    prevLeaf.prevLeaf()?.elementType != LPAR
            }
            val spacingAfter = if (node.elementType == LPAR) {
                nextLeaf is PsiWhiteSpace &&
                    (!nextLeaf.textContains('\n') || nextLeaf.nextLeaf()?.elementType == RPAR) &&
                    !nextLeaf.isNextLeafAComment()
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

    private fun ASTNode.isNextLeafAComment(): Boolean {
        val commentTypes = setOf(EOL_COMMENT, BLOCK_COMMENT, KDOC_START)
        return nextLeaf()?.elementType in commentTypes
    }
}
