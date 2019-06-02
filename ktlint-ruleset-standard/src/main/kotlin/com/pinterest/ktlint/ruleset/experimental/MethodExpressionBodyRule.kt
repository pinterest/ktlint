package com.pinterest.ktlint.ruleset.experimental

import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.ast.ElementType
import com.pinterest.ktlint.core.ast.children
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.CompositeElement
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.PsiWhiteSpaceImpl

/**
 * Prefer using an expression body for functions with the body consisting of a single expression
 */
class MethodExpressionBodyRule : Rule("method-expression-body") {

    override fun visit(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit
    ) {
        if (node.elementType == KtNodeTypes.FUN) {
            node.findChildByType(KtNodeTypes.BLOCK)?.let {
                it as CompositeElement
                it.getBody().let { bodyStatements ->
                    if (bodyStatements.size == 1) {
                        emit(node.firstChildNode.startOffset, "Single expression methods should use an expression body", true)
                        if (autoCorrect) {
                            // If the single-statement inside the method has a 'return', we want to ignore it
                            val singleExpression = when (bodyStatements.first().elementType) {
                                ElementType.RETURN -> bodyStatements.first().lastChildNode
                                else -> bodyStatements.first()
                            }
                            node.removeChild(it)
                            node.addChild(LeafPsiElement(ElementType.EQ, "="), null)
                            node.addChild(PsiWhiteSpaceImpl(" "), null)
                            node.addChild(singleExpression, null)
                        }
                    }
                }
            }
        }
    }

    /**
     * Get the content of a composite element between LBRACE and RBRACE (ignoring
     * any whitespace after LBRACE or before RBRACE)
     */
    private fun CompositeElement.getBody(): List<ASTNode> {
        return children().filter {
            it.elementType != ElementType.LBRACE &&
                it.elementType != ElementType.RBRACE &&
                it.elementType != ElementType.WHITE_SPACE
        }.toList()
    }
}
