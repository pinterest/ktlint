package com.pinterest.ktlint.ruleset.experimental

import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.ast.ElementType.MODIFIER_LIST
import com.pinterest.ktlint.core.ast.children
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.psiUtil.getPrevSiblingIgnoringWhitespaceAndComments

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
            val declaration = node.psi.parent as? KtDeclaration
            val prevDeclaration =
                declaration?.getPrevSiblingIgnoringWhitespaceAndComments(withItself = false) as? KtDeclaration
            val prevWhiteSpace = prevDeclaration?.nextSibling as? PsiWhiteSpace
            if (declaration != null && prevDeclaration != null && prevWhiteSpace?.text?.count { it == '\n' } == 1) {
                emit(
                    node.startOffset,
                    "Declarations and declarations with annotations should have an empty space between.",
                    true
                )
                if (autoCorrect) {
                    val indent = prevWhiteSpace.text.substringAfter('\n')
                    (prevWhiteSpace.node as LeafPsiElement).rawReplaceWithText("\n\n$indent")
                }
            }
        }
    }

    private fun ASTNode.hasAnnotationsAsChildren(): Boolean = children().find { it.psi is KtAnnotationEntry } != null
}
