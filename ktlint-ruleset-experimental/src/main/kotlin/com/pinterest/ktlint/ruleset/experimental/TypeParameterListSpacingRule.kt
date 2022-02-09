package com.pinterest.ktlint.ruleset.experimental

import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.ast.ElementType.CLASS
import com.pinterest.ktlint.core.ast.ElementType.CLASS_BODY
import com.pinterest.ktlint.core.ast.ElementType.CONSTRUCTOR_KEYWORD
import com.pinterest.ktlint.core.ast.ElementType.PRIMARY_CONSTRUCTOR
import com.pinterest.ktlint.core.ast.ElementType.TYPE_PARAMETER_LIST
import com.pinterest.ktlint.core.ast.ElementType.WHITE_SPACE
import com.pinterest.ktlint.core.ast.nextCodeSibling
import com.pinterest.ktlint.core.ast.nextSibling
import com.pinterest.ktlint.core.ast.prevSibling
import com.pinterest.ktlint.core.ast.upsertWhitespaceBeforeMe
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement

public class TypeParameterListSpacingRule : Rule("type-parameter-list-spacing") {
    override fun visit(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit
    ) {
        if (node.elementType != TYPE_PARAMETER_LIST) {
            return
        }

        if (node.treeParent.elementType == CLASS) {
            visitClassDeclaration(node, autoCorrect, emit)
        } else {
            visitFunctionDeclaration(node, autoCorrect, emit)
        }
    }

    private fun visitClassDeclaration(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit
    ) {
        // No white space expected between class name and parameter list
        //     class Bar <T>
        node
            .prevSibling { true }
            ?.takeIf { it.elementType == WHITE_SPACE }
            ?.let { noWhitespaceExpected(it, autoCorrect, emit) }

        // No white space expected between parameter type list and the constructor except when followed by compound
        // constructor
        //     class Bar<T> (...)
        node
            .nextSibling { true }
            ?.takeIf { it.elementType == WHITE_SPACE && it.nextCodeSibling()?.elementType == PRIMARY_CONSTRUCTOR }
            ?.let { whiteSpace ->
                if (whiteSpace.nextCodeSibling()?.findChildByType(CONSTRUCTOR_KEYWORD) != null) {
                    //    class Bar<T> constructor(...)
                    //    class Bar<T> actual constructor(...)
                    //    class Bar<T> @SomeAnnotation constructor(...)
                    singleWhiteSpaceExpected(whiteSpace, autoCorrect, emit)
                } else {
                    noWhitespaceExpected(whiteSpace, autoCorrect, emit)
                }
            }

        // No white space expected between parameter type list and class body when constructor is missing
        //    class Bar<T> {
        node
            .nextSibling { true }
            ?.takeIf { it.elementType == WHITE_SPACE && it.nextCodeSibling()?.elementType == CLASS_BODY }
            ?.let { singleWhiteSpaceExpected(it, autoCorrect, emit) }
    }

    private fun visitFunctionDeclaration(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit
    ) {
        node
            .nextSibling { true }
            ?.takeUnless { it.elementType == WHITE_SPACE && it.text == " " }
            ?.let { node ->
                singleWhiteSpaceExpected(node, autoCorrect, emit)
            }
    }

    private fun noWhitespaceExpected(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit
    ) {
        if (node.text != "") {
            emit(
                node.startOffset,
                "No whitespace expected at this position",
                true
            )
            if (autoCorrect) {
                node.treeParent.removeChild(node)
            }
        }
    }

    private fun singleWhiteSpaceExpected(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit
    ) {
        when {
            node.text == " " -> Unit
            node.textContains('\n') -> {
                emit(
                    node.startOffset,
                    "Expected a single space instead of newline",
                    true
                )
                if (autoCorrect) {
                    (node as LeafPsiElement).rawReplaceWithText(" ")
                }
            }
            else -> {
                emit(
                    node.startOffset,
                    "Expected a single space",
                    true
                )
                if (autoCorrect) {
                    if (node.elementType == WHITE_SPACE) {
                        (node as LeafPsiElement).rawReplaceWithText(" ")
                    } else {
                        (node as LeafPsiElement).upsertWhitespaceBeforeMe(" ")
                    }
                }
            }
        }
    }
}
