package com.pinterest.ktlint.ruleset.experimental

import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.ast.ElementType.MODIFIER_LIST
import com.pinterest.ktlint.core.ast.children
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.PsiComment
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.psiUtil.getPrevSiblingIgnoringWhitespaceAndComments
import org.jetbrains.kotlin.psi.psiUtil.prevLeaf
import org.jetbrains.kotlin.psi.psiUtil.prevLeafs

/**
 * @see https://youtrack.jetbrains.com/issue/KT-35106
 */
class SpacingBetweenDeclarationsWithAnnotationsRule : Rule("$experimentalRulesetId:spacing-between-declarations-with-annotations") {
    override fun visit(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit
    ) {
        if (node.elementType == MODIFIER_LIST && node.hasAnnotationsAsChildren()) {
            val declaration = node.psi.parent as? KtDeclaration
            val prevDeclaration =
                declaration?.getPrevSiblingIgnoringWhitespaceAndComments(withItself = false) as? KtDeclaration
            val whiteSpaceAfterPreviousDeclaration = prevDeclaration?.nextSibling as? PsiWhiteSpace
            val startOfDeclarationIncludingLeadingComment = node.psi.parent.getPrevLeafIgnoringCommentAndWhitespaceExceptBlankLines()
            if (whiteSpaceAfterPreviousDeclaration?.text != null &&
                startOfDeclarationIncludingLeadingComment?.text?.count { it == '\n' } == 1
            ) {
                emit(
                    node.startOffset,
                    "Declarations and declarations with annotations should have an empty space between.",
                    true
                )
                if (autoCorrect) {
                    val indent = whiteSpaceAfterPreviousDeclaration.text.substringAfter('\n')
                    (whiteSpaceAfterPreviousDeclaration.node as LeafPsiElement).rawReplaceWithText("\n\n$indent")
                }
            }
        }
    }

    /**
     * Gets the previous element but ignores white whitespaces (excluding blank lines) and comments. Note the difference
     * with method [PsiElement.getPrevSiblingIgnoringWhitespaceAndComments] which excludes blank lines as well.
     */
    private fun PsiElement.getPrevLeafIgnoringCommentAndWhitespaceExceptBlankLines(): PsiElement? {
        var prevLeaf: PsiElement? = this.prevLeaf()
        val iterator = prevLeafs.iterator()
        while (iterator.hasNext()) {
            val psiElement = iterator.next()
            if (psiElement is PsiComment || (psiElement is PsiWhiteSpace && psiElement.text?.count { it == '\n' } == 1)) {
                prevLeaf = psiElement
            } else {
                break
            }
        }
        return prevLeaf
    }

    private fun ASTNode.hasAnnotationsAsChildren(): Boolean = children().find { it.psi is KtAnnotationEntry } != null
}
