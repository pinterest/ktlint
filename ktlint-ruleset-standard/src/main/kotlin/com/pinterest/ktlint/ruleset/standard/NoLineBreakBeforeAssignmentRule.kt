package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.ast.ElementType.EQ
import com.pinterest.ktlint.core.ast.isPartOfComment
import com.pinterest.ktlint.core.ast.isWhiteSpace
import com.pinterest.ktlint.core.ast.isWhiteSpaceWithNewline
import com.pinterest.ktlint.core.ast.prevCodeSibling
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.psiUtil.siblings

public class NoLineBreakBeforeAssignmentRule : Rule("no-line-break-before-assignment") {

    override fun beforeVisitChildNodes(node: ASTNode, autoCorrect: Boolean, emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit) {
        if (node.elementType == EQ) {
            val prevCodeSibling = node.prevCodeSibling()
            val hasLineBreakBeforeAssignment = prevCodeSibling
                ?.siblings()
                ?.takeWhile { it.isWhiteSpace() || it.isPartOfComment() }
                ?.any { it.isWhiteSpaceWithNewline() }
            if (hasLineBreakBeforeAssignment == true) {
                emit(node.startOffset, "Line break before assignment is not allowed", true)
                if (autoCorrect) {
                    val prevPsi = prevCodeSibling.psi
                    val parentPsi = prevPsi.parent
                    val psiFactory = KtPsiFactory(prevPsi)
                    if (prevPsi.nextSibling !is PsiWhiteSpace) {
                        parentPsi.addAfter(psiFactory.createWhiteSpace(), prevPsi)
                    }
                    parentPsi.addAfter(psiFactory.createEQ(), prevPsi)
                    parentPsi.addAfter(psiFactory.createWhiteSpace(), prevPsi)
                    (node as? LeafPsiElement)?.delete()
                }
            }
        }
    }
}
