package com.pinterest.ktlint.ruleset.experimental

import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.ast.ElementType
import com.pinterest.ktlint.core.ast.ElementType.BLOCK
import com.pinterest.ktlint.core.ast.children
import com.pinterest.ktlint.core.ast.isWhiteSpaceWithNewline
import com.pinterest.ktlint.core.ast.parent
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
            node.findChildByType(KtNodeTypes.BLOCK)?.let { functionBlock ->
                functionBlock as CompositeElement
                if (functionBlock.containsSingleStatement()) {
                    functionBlock.getSingleStatementBody().let { statement ->
                        // If a single expression is multi-line, users often prefer to use a normal body,
                        // not an expression body, so don't fire a violation
                        if (!statement.containsNewline()) {
                            emit(node.firstChildNode.startOffset, "Single expression methods should use an expression body", true)
                            if (autoCorrect) {
                                // If the single-statement inside the method has a 'return', we want to remove it as
                                // it's not necessary in expression body syntax
                                val singleExpression = when (statement.elementType) {
                                    ElementType.RETURN -> statement.lastChildNode
                                    else -> statement
                                }
                                node.removeChild(functionBlock)
                                node.addChild(LeafPsiElement(ElementType.EQ, "="), null)
                                node.addChild(PsiWhiteSpaceImpl(" "), null)
                                node.addChild(singleExpression, null)
                            }

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

    private fun CompositeElement.getSingleStatementBody(): ASTNode = getBody().first()

    private fun CompositeElement.containsSingleStatement(): Boolean = getBody().size == 1

    private fun ASTNode.containsNewline(): Boolean = textContains('\n')


}
