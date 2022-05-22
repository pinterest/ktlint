package com.pinterest.ktlint.ruleset.experimental

import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.ast.ElementType.FUN
import com.pinterest.ktlint.core.ast.ElementType.NULLABLE_TYPE
import com.pinterest.ktlint.core.ast.ElementType.TYPE_REFERENCE
import com.pinterest.ktlint.core.ast.ElementType.VALUE_PARAMETER_LIST
import com.pinterest.ktlint.core.ast.ElementType.WHITE_SPACE
import com.pinterest.ktlint.core.ast.nextSibling
import org.jetbrains.kotlin.com.intellij.lang.ASTNode

public class FunctionTypeReferenceSpacingRule : Rule("$experimentalRulesetId:function-type-reference-spacing") {
    override fun visit(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit
    ) {
        if (node.elementType == FUN) {
            node
                .findFunctionReceiverTypeReference()
                ?.let { typeReference ->
                    typeReference
                        .firstChildNode
                        .takeIf { it.elementType == NULLABLE_TYPE }
                        ?.let { nullableTypeElement ->
                            visitNodesUntilStartOfValueParameterList(nullableTypeElement.firstChildNode, emit, autoCorrect)
                        }

                    if (typeReference.elementType != NULLABLE_TYPE) {
                        visitNodesUntilStartOfValueParameterList(typeReference, emit, autoCorrect)
                    }
                }
        }
    }

    private fun ASTNode.findFunctionReceiverTypeReference(): ASTNode? {
        require(elementType == FUN)
        var currentNode: ASTNode? = firstChildNode
        while (currentNode != null && currentNode.elementType != VALUE_PARAMETER_LIST) {
            if (currentNode.elementType == TYPE_REFERENCE) {
                return currentNode
            }
            currentNode = currentNode.nextSibling { true }
        }
        return null
    }

    private fun visitNodesUntilStartOfValueParameterList(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
        autoCorrect: Boolean
    ) {
        var currentNode: ASTNode? = node
        while (currentNode != null && currentNode.elementType != VALUE_PARAMETER_LIST) {
            val nextNode = currentNode.nextSibling { true }
            removeIfNonEmptyWhiteSpace(currentNode, emit, autoCorrect)
            currentNode = nextNode
        }
    }

    private fun removeIfNonEmptyWhiteSpace(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
        autoCorrect: Boolean
    ) {
        if (node.elementType == WHITE_SPACE && node.text.isNotEmpty()) {
            emit(node.startOffset, "Unexpected whitespace", true)
            if (autoCorrect) {
                node.treeParent.removeChild(node)
            }
        }
    }
}
