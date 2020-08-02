package com.pinterest.ktlint.ruleset.experimental

import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.ast.ElementType.FILE
import com.pinterest.ktlint.core.ast.ElementType.MODIFIER_LIST
import com.pinterest.ktlint.core.ast.ElementType.WHITE_SPACE
import com.pinterest.ktlint.core.ast.children
import com.pinterest.ktlint.core.ast.prevSibling
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.PsiComment
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.psi.KtAnnotationEntry

/**
 * @see https://youtrack.jetbrains.com/issue/KT-35106
 */
class SpacingBetweenDeclarationsWithAnnotationsRule : Rule("spacing-between-declarations-with-annotations") {

    override fun visit(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit
    ) {
        if (node.elementType == MODIFIER_LIST && node.hasAnnotationsAsChildren()) {
            val prevSibling = node.psi.parent.node.prevSibling { it.elementType != WHITE_SPACE }
            if (prevSibling != null &&
                prevSibling.elementType != FILE &&
                prevSibling !is PsiComment
            ) {
                if (node.psi.parent.prevSibling is PsiWhiteSpace && node.psi.parent.prevSibling.text == "\n") {
                    emit(
                        node.startOffset,
                        "Declarations and declarations with annotations should have an empty space between.",
                        true
                    )
                    if (autoCorrect) {
                        (node.psi.parent.prevSibling.node as LeafPsiElement).rawReplaceWithText("\n\n")
                    }
                }
            }
        }
    }

    private fun ASTNode.hasAnnotationsAsChildren(): Boolean = children().find { it.psi is KtAnnotationEntry } != null
}
