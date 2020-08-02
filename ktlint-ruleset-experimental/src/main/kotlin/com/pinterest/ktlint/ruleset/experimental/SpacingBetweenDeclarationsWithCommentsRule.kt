package com.pinterest.ktlint.ruleset.experimental

import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.ast.ElementType.FILE
import com.pinterest.ktlint.core.ast.ElementType.WHITE_SPACE
import com.pinterest.ktlint.core.ast.prevSibling
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.PsiComment
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement

/**
 * @see https://youtrack.jetbrains.com/issue/KT-35088
 */
class SpacingBetweenDeclarationsWithCommentsRule : Rule("spacing-between-declarations-with-comments") {

    override fun visit(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit
    ) {
        if (node is PsiComment) {
            val prevSibling = node.parent.node.prevSibling { it.elementType != WHITE_SPACE }
            if (prevSibling != null &&
                prevSibling.elementType != FILE &&
                prevSibling !is PsiComment
            ) {
                if (node.parent.prevSibling is PsiWhiteSpace && node.parent.prevSibling.text == "\n") {
                    emit(
                        node.startOffset,
                        "Declarations and declarations with comments should have an empty space between.",
                        true
                    )
                    if (autoCorrect) {
                        (node.parent.prevSibling.node as LeafPsiElement).rawReplaceWithText("\n\n")
                    }
                }
            }
        }
    }
}
