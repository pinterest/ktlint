package com.pinterest.ktlint.ruleset.experimental

import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.ast.ElementType
import com.pinterest.ktlint.core.ast.assertElementType
import com.pinterest.ktlint.core.ast.children
import com.pinterest.ktlint.core.ast.logStructure
import com.pinterest.ktlint.core.ast.prevLeaf
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.psi.psiUtil.endOffset

class NoTrailingCommaRule : Rule("no-trailing-comma") {

    override fun visit(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit
    ) {
        when (node.elementType) {
            ElementType.COLLECTION_LITERAL_EXPRESSION -> visitCollectionLiteralExpression(node, emit, autoCorrect)
            ElementType.DESTRUCTURING_DECLARATION -> visitDestructuringDeclaration(node, emit, autoCorrect)
            ElementType.FUNCTION_LITERAL -> visitFunctionLiteral(node, emit, autoCorrect)
            ElementType.FUNCTION_TYPE -> visitFunctionType(node, emit, autoCorrect)
            ElementType.INDICES -> visitIndices(node, emit, autoCorrect)
            ElementType.TYPE_ARGUMENT_LIST -> visitTypeList(node, emit, autoCorrect)
            ElementType.TYPE_PARAMETER_LIST -> visitTypeList(node, emit, autoCorrect)
            ElementType.VALUE_ARGUMENT_LIST -> visitValueList(node, emit, autoCorrect)
            ElementType.VALUE_PARAMETER_LIST -> visitValueList(node, emit, autoCorrect)
            ElementType.WHEN_ENTRY -> visitWhenEntry(node, emit, autoCorrect)
            else -> Unit
        }
    }

    private fun visitCollectionLiteralExpression(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
        autoCorrect: Boolean
    ) {
        val inspectNode = node
            .assertElementType(ElementType.COLLECTION_LITERAL_EXPRESSION)
            .children()
            .last { it.elementType == ElementType.RBRACKET }
            .prevLeaf()
        node.reportAndOrCorrectTrailingCommaNodeBefore(inspectNode, emit, autoCorrect)
    }

    private fun visitDestructuringDeclaration(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
        autoCorrect: Boolean
    ) {
        val inspectNode = node
            .assertElementType(ElementType.DESTRUCTURING_DECLARATION)
            .children()
            .last { it.elementType == ElementType.RPAR }
            .prevLeaf()
        node.reportAndOrCorrectTrailingCommaNodeBefore(inspectNode, emit, autoCorrect)
    }

    private fun visitFunctionLiteral(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
        autoCorrect: Boolean
    ) {
        val inspectNode = node
            .assertElementType(ElementType.FUNCTION_LITERAL)
            .children()
            .lastOrNull() { it.elementType == ElementType.ARROW }
            ?.prevLeaf()
        node.reportAndOrCorrectTrailingCommaNodeBefore(inspectNode, emit, autoCorrect)
    }

    private fun visitFunctionType(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
        autoCorrect: Boolean
    ) {
        val inspectNode = node
            .assertElementType(ElementType.FUNCTION_TYPE)
            .firstChildNode
            .assertElementType(ElementType.VALUE_PARAMETER_LIST)
            .children()
            .last()
            .prevLeaf()
        node.reportAndOrCorrectTrailingCommaNodeBefore(inspectNode, emit, autoCorrect)
    }

    private fun visitIndices(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
        autoCorrect: Boolean
    ) {
        val inspectNode = node
            .assertElementType(ElementType.INDICES)
            .children()
            .last { it.elementType == ElementType.RBRACKET }
            .prevLeaf()
        node.reportAndOrCorrectTrailingCommaNodeBefore(inspectNode, emit, autoCorrect)
    }

    private fun visitValueList(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
        autoCorrect: Boolean
    ) {
        if (node.treeParent.elementType != ElementType.FUNCTION_LITERAL &&
            node.treeParent.elementType != ElementType.FUNCTION_TYPE
        ) {
            val inspectNode = node
                .assertElementType(ElementType.VALUE_ARGUMENT_LIST, ElementType.VALUE_PARAMETER_LIST)
                .children()
                .last { it.elementType == ElementType.RPAR }
                .prevLeaf()
            node.reportAndOrCorrectTrailingCommaNodeBefore(inspectNode, emit, autoCorrect)
        }
    }

    private fun visitTypeList(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
        autoCorrect: Boolean
    ) {
        val inspectNode = node
            .assertElementType(ElementType.TYPE_ARGUMENT_LIST, ElementType.TYPE_PARAMETER_LIST)
            .children()
            .first { it.elementType == ElementType.GT }
            .prevLeaf()
        node.reportAndOrCorrectTrailingCommaNodeBefore(inspectNode, emit, autoCorrect)
    }

    private fun visitWhenEntry(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
        autoCorrect: Boolean
    ) {
        val inspectNode = node
            .assertElementType(ElementType.WHEN_ENTRY)
            .children()
            .first { it.elementType == ElementType.ARROW }
            .prevLeaf()
        node.reportAndOrCorrectTrailingCommaNodeBefore(inspectNode, emit, autoCorrect)
    }

    private fun ASTNode.reportAndOrCorrectTrailingCommaNodeBefore(
        inspectNode: ASTNode?,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
        autoCorrect: Boolean
    ) {
        inspectNode
            .findPreviousTrailingCommaNodeOrNull()
            ?.let { trailingCommaNode ->
                emit(trailingCommaNode.psi.endOffset - 1, "Trailing comma is redundant", true)
                if (autoCorrect) {
                    this.removeChild(trailingCommaNode)
                }
            }
    }

    private fun ASTNode?.findPreviousTrailingCommaNodeOrNull(): ASTNode? {
        var node = this
        while (node != null && node.isIgnorable()) {
            node = node.prevLeaf()
        }
        return if (node != null && node.elementType == ElementType.COMMA) {
            node
        } else {
            null
        }
    }

    private fun ASTNode.isIgnorable(): Boolean =
        elementType == ElementType.WHITE_SPACE ||
            elementType == ElementType.EOL_COMMENT ||
            elementType == ElementType.BLOCK_COMMENT
}
