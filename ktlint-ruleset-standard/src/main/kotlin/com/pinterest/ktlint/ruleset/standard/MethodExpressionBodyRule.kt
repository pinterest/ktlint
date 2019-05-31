package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.ast.ElementType
import com.pinterest.ktlint.core.ast.children
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.CompositeElement

/**
 * Prefer using an expression body for functions with the body consisting of a single expression
 */
class MethodExpressionBodyRule : Rule("method-expression-body") {

    override fun visit(node: ASTNode, autoCorrect: Boolean, emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit) {
        if (node.elementType == KtNodeTypes.FUN) {
            node.findChildByType(KtNodeTypes.BLOCK)?.let {
                it as CompositeElement
                if (it.getBody().size == 1) {
                    emit(node.firstChildNode.startOffset, "Single expression methods should use expression body", false)
                }
            }
        }
    }

    /**
     * Get the content between LBRACE and RBRACE
     */
    private fun CompositeElement.getBody(): List<ASTNode> {
        return children().filter {
            it.elementType != ElementType.LBRACE &&
                it.elementType != ElementType.RBRACE &&
                it.elementType != ElementType.WHITE_SPACE
        }.toList()
    }
}
