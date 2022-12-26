package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.ast.ElementType
import com.pinterest.ktlint.core.ast.findCompositeElementOfType
import com.pinterest.ktlint.core.ast.isPartOfCompositeElementOfType
import com.pinterest.ktlint.core.ast.nextLeaf
import com.pinterest.ktlint.core.ast.nextSibling
import com.pinterest.ktlint.core.ast.parent
import com.pinterest.ktlint.core.ast.prevLeaf
import com.pinterest.ktlint.core.ast.prevSibling
import org.jetbrains.kotlin.com.intellij.lang.ASTNode

/**
 * Lints and formats the spacing before and after the angle brackets of a type argument list.
 */
public class TypeArgumentListSpacingRule :
    Rule("type-argument-list-spacing"),
    Rule.Experimental {
    override fun beforeVisitChildNodes(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        when (node.elementType) {
            ElementType.TYPE_ARGUMENT_LIST -> {
                visitFunctionDeclaration(node, autoCorrect, emit)
                visitInsideTypeArgumentList(node, autoCorrect, emit)
            }
            ElementType.SUPER_TYPE_LIST, ElementType.SUPER_EXPRESSION ->
                visitInsideTypeArgumentList(node, autoCorrect, emit)
        }
    }

    private fun visitFunctionDeclaration(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        // No whitespace expected before type argument list of function call
        //    val list = listOf <String>()
        node
            .prevLeaf(includeEmpty = true)
            ?.takeIf { it.elementType == ElementType.WHITE_SPACE }
            ?.let { noWhitespaceExpected(it, autoCorrect, emit) }

        // No whitespace expected after type argument list of function call
        //    val list = listOf<String> ()
        node
            .takeUnless {
                // unless it is part of a type reference:
                //    fun foo(): List<Foo> { ... }
                //    var bar: List<Bar> = emptyList()
                it.isPartOfCompositeElementOfType(ElementType.TYPE_REFERENCE)
            }
            ?.takeUnless {
                // unless it is part of a call expression followed by lambda:
                //    bar<Foo> { ... }
                it.isPartOfCallExpressionFollowedByLambda()
            }
            ?.lastChildNode
            ?.nextLeaf(includeEmpty = true)
            ?.takeIf { it.elementType == ElementType.WHITE_SPACE }
            ?.let { noWhitespaceExpected(it, autoCorrect, emit) }
    }

    private fun visitInsideTypeArgumentList(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        // No whitespace expected after opening angle bracket of type argument list
        //    val list = listOf< String>()
        node
            .findChildByType(ElementType.LT)
            ?.nextSibling { true }
            ?.takeIf { it.elementType == ElementType.WHITE_SPACE }
            ?.let { noWhitespaceExpected(it, autoCorrect, emit) }

        // No whitespace expected before closing angle bracket of type argument list
        //    val list = listOf<String >()
        node
            .findChildByType(ElementType.GT)
            ?.prevSibling { true }
            ?.takeIf { it.elementType == ElementType.WHITE_SPACE }
            ?.let { noWhitespaceExpected(it, autoCorrect, emit) }
    }

    private fun noWhitespaceExpected(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        if (node.text != "") {
            emit(
                node.startOffset,
                "No whitespace expected at this position",
                true,
            )
            if (autoCorrect) {
                node.treeParent.removeChild(node)
            }
        }
    }
}

private fun ASTNode.isPartOfCallExpressionFollowedByLambda(): Boolean =
    parent(findCompositeElementOfType(ElementType.CALL_EXPRESSION))
        ?.takeIf { it.elementType == ElementType.CALL_EXPRESSION }
        ?.findChildByType(ElementType.LAMBDA_ARGUMENT)
        .let { it != null }
