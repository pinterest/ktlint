package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.ruleset.core.api.ElementType.CLASS
import com.pinterest.ktlint.ruleset.core.api.ElementType.CLASS_BODY
import com.pinterest.ktlint.ruleset.core.api.ElementType.CONSTRUCTOR_KEYWORD
import com.pinterest.ktlint.ruleset.core.api.ElementType.EQ
import com.pinterest.ktlint.ruleset.core.api.ElementType.FUN
import com.pinterest.ktlint.ruleset.core.api.ElementType.GT
import com.pinterest.ktlint.ruleset.core.api.ElementType.LT
import com.pinterest.ktlint.ruleset.core.api.ElementType.PRIMARY_CONSTRUCTOR
import com.pinterest.ktlint.ruleset.core.api.ElementType.TYPEALIAS
import com.pinterest.ktlint.ruleset.core.api.ElementType.TYPE_PARAMETER_LIST
import com.pinterest.ktlint.ruleset.core.api.ElementType.WHITE_SPACE
import com.pinterest.ktlint.ruleset.core.api.nextCodeSibling
import com.pinterest.ktlint.ruleset.core.api.nextLeaf
import com.pinterest.ktlint.ruleset.core.api.nextSibling
import com.pinterest.ktlint.ruleset.core.api.prevLeaf
import com.pinterest.ktlint.ruleset.core.api.prevSibling
import com.pinterest.ktlint.ruleset.core.api.upsertWhitespaceBeforeMe
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement

/**
 * Lints and formats the spacing before and after the angle brackets of a type parameter list.
 */
public class TypeParameterListSpacingRule :
    Rule("type-parameter-list-spacing"),
    Rule.Experimental {
    override fun beforeVisitChildNodes(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        if (node.elementType != TYPE_PARAMETER_LIST) {
            return
        }

        when (node.treeParent.elementType) {
            CLASS -> visitClassDeclaration(node, autoCorrect, emit)
            TYPEALIAS -> visitTypeAliasDeclaration(node, autoCorrect, emit)
            FUN -> visitFunctionDeclaration(node, autoCorrect, emit)
        }
        visitInsideTypeParameterList(node, autoCorrect, emit)
    }

    private fun visitClassDeclaration(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
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
                    // Single space expect before (modifier list of) constructor
                    //    class Bar<T> constructor(...)
                    //    class Bar<T> actual constructor(...)
                    //    class Bar<T> @SomeAnnotation constructor(...)
                    singleSpaceExpected(whiteSpace, autoCorrect, emit)
                } else {
                    noWhitespaceExpected(whiteSpace, autoCorrect, emit)
                }
            }

        // No white space expected between parameter type list and class body when constructor is missing
        //    class Bar<T> {
        node
            .nextSibling { true }
            ?.takeIf { it.elementType == WHITE_SPACE && it.nextCodeSibling()?.elementType == CLASS_BODY }
            ?.let { singleSpaceExpected(it, autoCorrect, emit) }
    }

    private fun visitTypeAliasDeclaration(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        // No white space expected between typealias keyword name and parameter list
        //     typealias Bar <T>
        node
            .prevSibling { true }
            ?.takeIf { it.elementType == WHITE_SPACE }
            ?.let { noWhitespaceExpected(it, autoCorrect, emit) }

        // No white space expected between parameter type list and equals sign
        //    typealias Bar<T> = ...
        node
            .nextSibling { true }
            ?.takeIf { it.elementType == WHITE_SPACE && it.nextCodeSibling()?.elementType == EQ }
            ?.let { singleSpaceExpected(it, autoCorrect, emit) }
    }

    private fun visitFunctionDeclaration(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        // Single space expected before type parameter list of function
        //    fun<T> foo(...)
        node
            .prevLeaf(includeEmpty = true)
            ?.let { prevLeaf ->
                if (prevLeaf.elementType == WHITE_SPACE) {
                    singleSpaceExpected(prevLeaf, autoCorrect, emit)
                } else {
                    singleSpaceExpected(node.firstChildNode, autoCorrect, emit)
                }
            }

        // Single space expected after type parameter list of function
        //   fun <T>foo(...)
        //   fun <T>List<T>foo(...)
        node
            .lastChildNode
            .nextLeaf(includeEmpty = true)
            ?.let { nextSibling ->
                singleSpaceExpected(nextSibling, autoCorrect, emit)
            }
    }

    private fun visitInsideTypeParameterList(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        node
            .findChildByType(LT)
            ?.nextSibling { true }
            ?.takeIf { it.elementType == WHITE_SPACE }
            ?.let { noWhitespaceExpected(it, autoCorrect, emit) }

        node
            .findChildByType(GT)
            ?.prevSibling { true }
            ?.takeIf { it.elementType == WHITE_SPACE }
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

    private fun singleSpaceExpected(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        when {
            node.text == " " -> Unit
            node.textContains('\n') -> {
                emit(
                    node.startOffset,
                    "Expected a single space instead of newline",
                    true,
                )
                if (autoCorrect) {
                    (node as LeafPsiElement).rawReplaceWithText(" ")
                }
            }
            else -> {
                emit(
                    node.startOffset,
                    "Expected a single space",
                    true,
                )
                if (autoCorrect) {
                    node.upsertWhitespaceBeforeMe(" ")
                }
            }
        }
    }
}
