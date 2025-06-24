package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.ElementType
import com.pinterest.ktlint.rule.engine.core.api.IndentConfig
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint.Status.EXPERIMENTAL
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint.Status.STABLE
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.EditorConfig
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.INDENT_SIZE_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.INDENT_STYLE_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.ifAutocorrectAllowed
import com.pinterest.ktlint.rule.engine.core.api.isPartOf
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpace20
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpaceWithoutNewline20
import com.pinterest.ktlint.rule.engine.core.api.nextLeaf
import com.pinterest.ktlint.rule.engine.core.api.nextSibling
import com.pinterest.ktlint.rule.engine.core.api.nextSibling20
import com.pinterest.ktlint.rule.engine.core.api.parent
import com.pinterest.ktlint.rule.engine.core.api.prevLeaf
import com.pinterest.ktlint.rule.engine.core.api.prevSibling
import com.pinterest.ktlint.rule.engine.core.api.prevSibling20
import com.pinterest.ktlint.rule.engine.core.api.remove
import com.pinterest.ktlint.rule.engine.core.api.upsertWhitespaceAfterMe
import com.pinterest.ktlint.rule.engine.core.api.upsertWhitespaceBeforeMe
import com.pinterest.ktlint.ruleset.standard.StandardRule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode

/**
 * Lints and formats the spacing before and after the angle brackets of a type argument list.
 */
@SinceKtlint("0.45", EXPERIMENTAL)
@SinceKtlint("1.0", STABLE)
public class TypeArgumentListSpacingRule :
    StandardRule(
        id = "type-argument-list-spacing",
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
        when (node.elementType) {
            ElementType.TYPE_ARGUMENT_LIST -> {
                visitFunctionDeclaration(node, emit)
                visitInsideTypeArgumentList(node, emit)
            }

            ElementType.SUPER_TYPE_LIST, ElementType.SUPER_EXPRESSION -> {
                visitInsideTypeArgumentList(node, emit)
            }
        }
    }

    private fun visitFunctionDeclaration(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        // No whitespace expected before type argument list of function call
        //    val list = listOf <String>()
        node
            .prevLeaf(includeEmpty = true)
            ?.takeIf { it.elementType == ElementType.WHITE_SPACE }
            ?.let { noWhitespaceExpected(it, emit) }

        // No whitespace expected after type argument list of function call
        //    val list = listOf<String> ()
        node
            .takeUnless {
                // unless it is part of a type reference:
                //    fun foo(): List<Foo> { ... }
                //    var bar: List<Bar> = emptyList()
                it.isPartOf(ElementType.TYPE_REFERENCE)
            }?.takeUnless {
                // unless it is part of a call expression followed by lambda:
                //    bar<Foo> { ... }
                it.isPartOfCallExpressionFollowedByLambda()
            }?.lastChildNode
            ?.nextLeaf
            ?.takeIf { it.elementType == ElementType.WHITE_SPACE }
            ?.let { noWhitespaceExpected(it, emit) }
    }

    private fun visitInsideTypeArgumentList(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        val multiline = node.textContains('\n')
        val expectedIndent =
            if (multiline) {
                indentConfig.childIndentOf(node)
            } else {
                indentConfig.siblingIndentOf(node)
            }

        node
            .findChildByType(ElementType.LT)
            ?.nextSibling20
            ?.let { nextSibling ->
                if (multiline) {
                    if (nextSibling.text != expectedIndent) {
                        if (nextSibling.isWhiteSpaceWithoutNewline20) {
                            emit(nextSibling.startOffset, "Expected newline", true)
                                .ifAutocorrectAllowed {
                                    nextSibling.upsertWhitespaceAfterMe(expectedIndent)
                                }
                        } else {
                            // Let Indentation rule fix the indentation
                        }
                    }
                } else {
                    if (nextSibling.isWhiteSpace20) {
                        // Disallow
                        //    val list = listOf< String>()
                        noWhitespaceExpected(nextSibling, emit)
                    }
                }
            }

        node
            .findChildByType(ElementType.GT)
            ?.prevSibling20
            ?.let { prevSibling ->
                if (multiline) {
                    if (prevSibling.text != expectedIndent) {
                        if (prevSibling.isWhiteSpaceWithoutNewline20) {
                            emit(prevSibling.startOffset, "Expected newline", true)
                                .ifAutocorrectAllowed {
                                    prevSibling.upsertWhitespaceBeforeMe(expectedIndent)
                                }
                        } else {
                            // Let Indentation rule fix the indentation
                        }
                    }
                } else {
                    if (prevSibling.isWhiteSpace20) {
                        // Disallow
                        //    val list = listOf<String >()
                        noWhitespaceExpected(prevSibling, emit)
                    }
                }
            }
    }

    private fun noWhitespaceExpected(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        if (node.text != "") {
            emit(node.startOffset, "No whitespace expected at this position", true)
                .ifAutocorrectAllowed { node.remove() }
        }
    }
}

private fun ASTNode.isPartOfCallExpressionFollowedByLambda(): Boolean =
    parent(ElementType.CALL_EXPRESSION)
        ?.takeIf { it.elementType == ElementType.CALL_EXPRESSION }
        ?.findChildByType(ElementType.LAMBDA_ARGUMENT)
        .let { it != null }

public val TYPE_ARGUMENT_LIST_SPACING_RULE_ID: RuleId = TypeArgumentListSpacingRule().ruleId
