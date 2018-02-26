package com.github.shyiko.ktlint.ruleset.standard

import com.github.shyiko.ktlint.core.Rule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes

class NoUnitReturnRule : Rule("no-unit-return") {

    override fun visit(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit
    ) {
        if (node.elementType == KtStubElementTypes.TYPE_REFERENCE &&
            node.treeParent.elementType == KtStubElementTypes.FUNCTION &&
            node.text.contentEquals("Unit") &&
            PsiTreeUtil.nextVisibleLeaf(node.psi)?.node?.elementType == KtTokens.LBRACE) {
            emit(node.startOffset, "Unnecessary \"Unit\" return type", true)
            if (autoCorrect) {
                var prevNode = node
                while (prevNode.treePrev.elementType != KtStubElementTypes.VALUE_PARAMETER_LIST) {
                    prevNode = prevNode.treePrev
                }
                node.treeParent.removeRange(prevNode, node.treeNext)
            }
        }
    }
}
