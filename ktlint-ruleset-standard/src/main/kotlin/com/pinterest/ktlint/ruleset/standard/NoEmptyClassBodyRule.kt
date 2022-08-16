package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.ast.ElementType.CLASS_BODY
import com.pinterest.ktlint.core.ast.ElementType.LBRACE
import com.pinterest.ktlint.core.ast.ElementType.RBRACE
import com.pinterest.ktlint.core.ast.ElementType.WHITE_SPACE
import com.pinterest.ktlint.core.ast.children
import com.pinterest.ktlint.core.ast.isPartOf
import com.pinterest.ktlint.core.ast.nextLeaf
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.psi.KtObjectLiteralExpression

public class NoEmptyClassBodyRule : Rule("no-empty-class-body") {

    override fun beforeVisitChildNodes(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit
    ) {
        if (node.elementType == CLASS_BODY &&
            node.firstChildNode?.let { n ->
                n.elementType == LBRACE &&
                    n.nextLeaf { it.elementType != WHITE_SPACE }?.elementType == RBRACE
            } == true &&
            !node.isPartOf(KtObjectLiteralExpression::class) &&
            node.treeParent.firstChildNode.children().none { it.text == "companion" }
        ) {
            emit(node.startOffset, "Unnecessary block (\"{}\")", true)
            if (autoCorrect) {
                val prevNode = node.treePrev
                if (prevNode.elementType == WHITE_SPACE) {
                    // remove space between declaration and block
                    prevNode.treeParent.removeChild(prevNode)
                }
                // remove block
                node.treeParent.removeChild(node)
            }
        }
    }
}
