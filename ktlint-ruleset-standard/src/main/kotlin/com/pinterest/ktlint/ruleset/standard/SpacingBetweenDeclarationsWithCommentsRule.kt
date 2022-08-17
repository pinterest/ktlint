package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.ast.ElementType.FILE
import com.pinterest.ktlint.core.ast.ElementType.WHITE_SPACE
import com.pinterest.ktlint.core.ast.prevLeaf
import com.pinterest.ktlint.core.ast.prevSibling
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.PsiComment
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.psiUtil.getPrevSiblingIgnoringWhitespaceAndComments
import org.jetbrains.kotlin.psi.psiUtil.startOffset

/**
 * @see https://youtrack.jetbrains.com/issue/KT-35088
 */
public class SpacingBetweenDeclarationsWithCommentsRule : Rule("spacing-between-declarations-with-comments") {
    override fun beforeVisitChildNodes(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        if (node is PsiComment) {
            val declaration = node.parent as? KtDeclaration ?: return
            val isTailComment = node.startOffset > declaration.startOffset
            if (isTailComment || declaration.getPrevSiblingIgnoringWhitespaceAndComments() !is KtDeclaration) return

            val prevSibling = declaration.node.prevSibling { it.elementType != WHITE_SPACE }
            if (prevSibling != null &&
                prevSibling.elementType != FILE &&
                prevSibling !is PsiComment
            ) {
                if (declaration.prevSibling is PsiWhiteSpace && declaration.prevSibling.text.count { it == '\n' } < 2) {
                    emit(
                        node.startOffset,
                        "Declarations and declarations with comments should have an empty space between.",
                        true,
                    )
                    if (autoCorrect) {
                        val indent = node.prevLeaf()?.text?.trim('\n') ?: ""
                        (declaration.prevSibling.node as LeafPsiElement).rawReplaceWithText("\n\n$indent")
                    }
                }
            }
        }
    }
}
