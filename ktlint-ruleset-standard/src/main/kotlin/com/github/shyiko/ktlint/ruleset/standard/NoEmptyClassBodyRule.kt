package com.github.shyiko.ktlint.ruleset.standard

import com.github.shyiko.ktlint.core.Rule
import org.jetbrains.kotlin.KtNodeTypes.CLASS_BODY
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.PsiWhiteSpaceImpl
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.TreeElement
import org.jetbrains.kotlin.com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtObjectLiteralExpression
import org.jetbrains.kotlin.psi.psiUtil.getNextSiblingIgnoringWhitespace

class NoEmptyClassBodyRule : Rule("no-empty-class-body") {

    override fun visit(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit
    ) {
        if (node.elementType == CLASS_BODY && node.psi.firstChild != null &&
            node.psi.firstChild.node.elementType == KtTokens.LBRACE &&
            node.psi.firstChild.getNextSiblingIgnoringWhitespace(false)!!.node.elementType == KtTokens.RBRACE &&
            !node.psi.isPartOf(KtObjectLiteralExpression::class)) {
            emit(node.startOffset, "Unnecessary block (\"{}\")", true)
            if (autoCorrect) {
                val prevNode = node.psi.prevSibling.node
                val nextNode = PsiTreeUtil.nextLeaf(node.psi, true)?.node
                if (prevNode.elementType == KtTokens.WHITE_SPACE && nextNode?.elementType == KtTokens.WHITE_SPACE) {
                    // remove space between declaration and block
                    prevNode.treeParent.removeChild(prevNode)
                }
                if (nextNode != null && nextNode.elementType != KtTokens.WHITE_SPACE) {
                    // make sure there is a whitespace between declaration and whatever follows next
                    (node as TreeElement).rawInsertBeforeMe(PsiWhiteSpaceImpl(" "))
                }
                node.treeParent.removeChild(node)
            }
        }
    }
}
