package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.ElementType.CLASS
import com.pinterest.ktlint.rule.engine.core.api.ElementType.CLASS_BODY
import com.pinterest.ktlint.rule.engine.core.api.ElementType.CONSTRUCTOR_KEYWORD
import com.pinterest.ktlint.rule.engine.core.api.ElementType.EQ
import com.pinterest.ktlint.rule.engine.core.api.ElementType.FUN
import com.pinterest.ktlint.rule.engine.core.api.ElementType.GT
import com.pinterest.ktlint.rule.engine.core.api.ElementType.LT
import com.pinterest.ktlint.rule.engine.core.api.ElementType.PRIMARY_CONSTRUCTOR
import com.pinterest.ktlint.rule.engine.core.api.ElementType.TYPEALIAS
import com.pinterest.ktlint.rule.engine.core.api.ElementType.TYPE_PARAMETER_LIST
import com.pinterest.ktlint.rule.engine.core.api.ElementType.WHITE_SPACE
import com.pinterest.ktlint.rule.engine.core.api.IndentConfig
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint.Status.EXPERIMENTAL
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint.Status.STABLE
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.EditorConfig
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.INDENT_SIZE_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.INDENT_STYLE_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.ifAutocorrectAllowed
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpaceWithNewline20
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpaceWithoutNewline20
import com.pinterest.ktlint.rule.engine.core.api.nextCodeSibling20
import com.pinterest.ktlint.rule.engine.core.api.nextLeaf
import com.pinterest.ktlint.rule.engine.core.api.nextSibling
import com.pinterest.ktlint.rule.engine.core.api.nextSibling20
import com.pinterest.ktlint.rule.engine.core.api.prevLeaf
import com.pinterest.ktlint.rule.engine.core.api.prevSibling
import com.pinterest.ktlint.rule.engine.core.api.remove
import com.pinterest.ktlint.rule.engine.core.api.upsertWhitespaceBeforeMe
import com.pinterest.ktlint.ruleset.standard.StandardRule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement

/**
 * Lints and formats the spacing before and after the angle brackets of a type parameter list.
 */
@SinceKtlint("0.45", EXPERIMENTAL)
@SinceKtlint("1.0", STABLE)
public class TypeParameterListSpacingRule :
    StandardRule(
        id = "type-parameter-list-spacing",
        usesEditorConfigProperties =
            setOf(
                INDENT_SIZE_PROPERTY,
                INDENT_STYLE_PROPERTY,
            ),
    ) {
    private var indentConfig = IndentConfig.DEFAULT_INDENT_CONFIG

    override fun beforeFirstNode(editorConfig: EditorConfig) {
        indentConfig =
            IndentConfig(
                indentStyle = editorConfig[INDENT_STYLE_PROPERTY],
                tabWidth = editorConfig[INDENT_SIZE_PROPERTY],
            )
    }

    override fun beforeVisitChildNodes(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        if (node.elementType != TYPE_PARAMETER_LIST) {
            return
        }

        when (node.treeParent.elementType) {
            CLASS -> visitClassDeclaration(node, emit)
            TYPEALIAS -> visitTypeAliasDeclaration(node, emit)
            FUN -> visitFunctionDeclaration(node, emit)
        }
        visitInsideTypeParameterList(node, emit)
    }

    private fun visitClassDeclaration(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        // No white space expected between class name and parameter list
        //     class Bar <T>
        node
            .prevSibling()
            ?.takeIf { it.elementType == WHITE_SPACE }
            ?.let { visitWhitespace(it, emit) }

        // No white space expected between parameter type list and the constructor except when followed by compound
        // constructor
        //     class Bar<T> (...)
        node
            .nextSibling20
            ?.takeIf { it.elementType == WHITE_SPACE && it.nextCodeSibling20?.elementType == PRIMARY_CONSTRUCTOR }
            ?.let { whiteSpace ->
                if (whiteSpace.nextCodeSibling20?.findChildByType(CONSTRUCTOR_KEYWORD) != null) {
                    if (whiteSpace.isWhiteSpaceWithNewline20) {
                        // Newline is acceptable before (modifier list of) constructor
                        //    class Bar<T>
                        //        constructor(...)
                        //    class Bar<T>
                        //        actual constructor(...)
                        //    class Bar<T>
                        //        @SomeAnnotation constructor(...)
                        //    class Bar<T>
                        //        @SomeAnnotation1
                        //        @SomeAnnotation2
                        //        constructor(...)
                        //    class Bar<T>
                        //        /**
                        //         * Some kdoc
                        //         */
                        //        constructor(...)
                    } else {
                        // Single space before (modifier list of) constructor
                        //    class Bar<T> constructor(...)
                        //    class Bar<T> actual constructor(...)
                        //    class Bar<T> @SomeAnnotation constructor(...)
                        if (whiteSpace.text != " ") {
                            emit(whiteSpace.startOffset, "Expected a single space", true)
                                .ifAutocorrectAllowed {
                                    // If line is to be wrapped this should have been done by other rules before running this rule
                                    whiteSpace.upsertWhitespaceBeforeMe(" ")
                                }
                        }
                    }
                } else {
                    visitWhitespace(whiteSpace, emit)
                }
            }

        // No white space expected between parameter type list and class body when constructor is missing
        //    class Bar<T> {
        node
            .nextSibling20
            ?.takeIf { it.elementType == WHITE_SPACE && it.nextCodeSibling20?.elementType == CLASS_BODY }
            ?.let { singleSpaceExpected(it, emit) }
    }

    private fun visitTypeAliasDeclaration(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        // No white space expected between typealias keyword name and parameter list
        //     typealias Bar <T>
        node
            .prevSibling()
            ?.takeIf { it.elementType == WHITE_SPACE }
            ?.let { visitWhitespace(it, emit) }

        // No white space expected between parameter type list and equals sign
        //    typealias Bar<T> = ...
        node
            .nextSibling20
            ?.takeIf { it.elementType == WHITE_SPACE && it.nextCodeSibling20?.elementType == EQ }
            ?.let { singleSpaceExpected(it, emit) }
    }

    private fun visitFunctionDeclaration(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        // Single space expected before type parameter list of function
        //    fun<T> foo(...)
        node
            .prevLeaf(includeEmpty = true)
            ?.let { prevLeaf ->
                if (prevLeaf.elementType == WHITE_SPACE) {
                    singleSpaceExpected(prevLeaf, emit)
                } else {
                    singleSpaceExpected(node.firstChildNode, emit)
                }
            }

        // Single space expected after type parameter list of function
        //   fun <T>foo(...)
        //   fun <T>List<T>foo(...)
        node
            .lastChildNode
            .nextLeaf
            ?.let { nextSibling ->
                singleSpaceExpected(nextSibling, emit)
            }
    }

    private fun visitInsideTypeParameterList(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        node
            .findChildByType(LT)
            ?.nextSibling20
            ?.takeIf { it.elementType == WHITE_SPACE }
            ?.let {
                val expectedWhitespace =
                    if (node.textContains('\n')) {
                        indentConfig.childIndentOf(node)
                    } else {
                        ""
                    }
                visitWhitespace(it, emit, expectedWhitespace)
            }

        node
            .findChildByType(GT)
            ?.prevSibling()
            ?.takeIf { it.elementType == WHITE_SPACE }
            ?.let {
                val expectedWhitespace =
                    if (node.textContains('\n')) {
                        indentConfig.childIndentOf(node)
                    } else {
                        ""
                    }
                visitWhitespace(it, emit, expectedWhitespace)
            }
    }

    private fun visitWhitespace(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
        expectedWhitespace: String = "",
    ) {
        if (node.text == expectedWhitespace) {
            return
        }

        when {
            expectedWhitespace.isEmpty() -> {
                emit(node.startOffset, "No whitespace expected", true)
                    .ifAutocorrectAllowed { node.remove() }
            }

            node.isWhiteSpaceWithoutNewline20 && expectedWhitespace.startsWith("\n") -> {
                emit(node.startOffset, "Expected a newline", true)
                    .ifAutocorrectAllowed {
                        (node as LeafPsiElement).rawReplaceWithText(expectedWhitespace)
                    }
            }

            expectedWhitespace == " " -> {
                emit(node.startOffset, "Expected a single space", true)
                    .ifAutocorrectAllowed {
                        (node as LeafPsiElement).rawReplaceWithText(expectedWhitespace)
                    }
            }
        }
    }

    private fun singleSpaceExpected(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        when {
            node.text == " " -> {
                Unit
            }

            node.textContains('\n') -> {
                emit(node.startOffset, "Expected a single space instead of newline", true)
                    .ifAutocorrectAllowed {
                        (node as LeafPsiElement).rawReplaceWithText(" ")
                    }
            }

            else -> {
                emit(node.startOffset, "Expected a single space", true)
                    .ifAutocorrectAllowed {
                        node.upsertWhitespaceBeforeMe(" ")
                    }
            }
        }
    }
}

public val TYPE_PARAMETER_LIST_SPACING_RULE_ID: RuleId = TypeParameterListSpacingRule().ruleId
