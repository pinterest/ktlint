package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.ElementType.AT
import com.pinterest.ktlint.rule.engine.core.api.ElementType.BLOCK
import com.pinterest.ktlint.rule.engine.core.api.ElementType.CLASS_BODY
import com.pinterest.ktlint.rule.engine.core.api.ElementType.COLONCOLON
import com.pinterest.ktlint.rule.engine.core.api.ElementType.COMMA
import com.pinterest.ktlint.rule.engine.core.api.ElementType.DOT
import com.pinterest.ktlint.rule.engine.core.api.ElementType.EXCLEXCL
import com.pinterest.ktlint.rule.engine.core.api.ElementType.LAMBDA_EXPRESSION
import com.pinterest.ktlint.rule.engine.core.api.ElementType.LBRACE
import com.pinterest.ktlint.rule.engine.core.api.ElementType.LBRACKET
import com.pinterest.ktlint.rule.engine.core.api.ElementType.LPAR
import com.pinterest.ktlint.rule.engine.core.api.ElementType.RANGE
import com.pinterest.ktlint.rule.engine.core.api.ElementType.RANGE_UNTIL
import com.pinterest.ktlint.rule.engine.core.api.ElementType.RBRACE
import com.pinterest.ktlint.rule.engine.core.api.ElementType.RBRACKET
import com.pinterest.ktlint.rule.engine.core.api.ElementType.RPAR
import com.pinterest.ktlint.rule.engine.core.api.ElementType.SAFE_ACCESS
import com.pinterest.ktlint.rule.engine.core.api.ElementType.SEMICOLON
import com.pinterest.ktlint.rule.engine.core.api.IndentConfig
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint.Status.STABLE
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.CODE_STYLE_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.EditorConfig
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.INDENT_SIZE_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.INDENT_STYLE_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.ifAutocorrectAllowed
import com.pinterest.ktlint.rule.engine.core.api.isCode
import com.pinterest.ktlint.rule.engine.core.api.isLeaf20
import com.pinterest.ktlint.rule.engine.core.api.isPartOfComment20
import com.pinterest.ktlint.rule.engine.core.api.isPartOfString20
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpace20
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpaceWithNewline20
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpaceWithoutNewline20
import com.pinterest.ktlint.rule.engine.core.api.leavesBackwardsIncludingSelf
import com.pinterest.ktlint.rule.engine.core.api.nextLeaf
import com.pinterest.ktlint.rule.engine.core.api.nextSibling20
import com.pinterest.ktlint.rule.engine.core.api.parent
import com.pinterest.ktlint.rule.engine.core.api.prevLeaf
import com.pinterest.ktlint.rule.engine.core.api.remove
import com.pinterest.ktlint.rule.engine.core.api.replaceTextWith
import com.pinterest.ktlint.rule.engine.core.api.upsertWhitespaceAfterMe
import com.pinterest.ktlint.rule.engine.core.api.upsertWhitespaceBeforeMe
import com.pinterest.ktlint.ruleset.standard.StandardRule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode

@SinceKtlint("0.1", STABLE)
public class SpacingAroundCurlyRule :
    StandardRule(
        id = "curly-spacing",
        usesEditorConfigProperties =
            setOf(
                CODE_STYLE_PROPERTY,
                INDENT_SIZE_PROPERTY,
                INDENT_STYLE_PROPERTY,
            ),
    ) {
    private var codeStyle = CODE_STYLE_PROPERTY.defaultValue
    private var indentConfig = IndentConfig.DEFAULT_INDENT_CONFIG

    override fun beforeFirstNode(editorConfig: EditorConfig) {
        codeStyle = editorConfig[CODE_STYLE_PROPERTY]
        indentConfig =
            IndentConfig(
                indentStyle = editorConfig[INDENT_STYLE_PROPERTY],
                tabWidth = editorConfig[INDENT_SIZE_PROPERTY],
            )
        if (indentConfig.disabled) {
            stopTraversalOfAST()
        }
    }

    override fun beforeVisitChildNodes(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        if (node.isLeaf20 && !node.isPartOfString20) {
            val prevLeaf = node.prevLeaf
            val nextLeaf = node.nextLeaf
            val spacingBefore: Boolean
            val spacingAfter: Boolean
            when (node.elementType) {
                LBRACE -> {
                    spacingBefore =
                        prevLeaf.isWhiteSpace20 ||
                        prevLeaf?.elementType == AT ||
                        (
                            (prevLeaf?.elementType == LPAR || prevLeaf?.elementType == LBRACKET) &&
                                (
                                    node.parent?.elementType == LAMBDA_EXPRESSION ||
                                        node.parent?.parent?.elementType == LAMBDA_EXPRESSION
                                )
                        )
                    spacingAfter = nextLeaf.isWhiteSpace20 || nextLeaf?.elementType == RBRACE
                    if (prevLeaf.isWhiteSpaceWithoutNewline20 &&
                        prevLeaf!!.isPrecededBy { it.elementType == LPAR || it.elementType == AT }
                    ) {
                        emit(node.startOffset, "Unexpected space before \"${node.text}\"", true)
                            .ifAutocorrectAllowed { prevLeaf.remove() }
                    }
                    if (prevLeaf != null && node.hasUnexpectedNewlineBeforeLbrace()) {
                        prevLeaf
                            .run {
                                emit(node.startOffset, "Unexpected newline before \"${node.text}\"", true)
                                    .ifAutocorrectAllowed {
                                        if (prevLeaf.isPrecededByEolComment()) {
                                            // All consecutive whitespaces and comments preceding the curly have to be moved after the curly brace
                                            prevLeaf
                                                .leavesBackwardsIncludingSelf
                                                .takeWhile { !it.isCode }
                                                .toList()
                                                .reversed()
                                                .takeIf { it.isNotEmpty() }
                                                ?.let { leavesToMoveAfterCurly ->
                                                    node.parent?.addChildren(
                                                        leavesToMoveAfterCurly.first(),
                                                        leavesToMoveAfterCurly.last(),
                                                        node.nextSibling20,
                                                    )
                                                }
                                        }
                                        replaceTextWith(" ")
                                    }
                            }
                    }
                }

                RBRACE -> {
                    spacingBefore = prevLeaf.isWhiteSpace20 || prevLeaf?.elementType == LBRACE
                    spacingAfter = nextLeaf == null || nextLeaf.isWhiteSpace20 || shouldNotToBeSeparatedBySpace(nextLeaf)
                    nextLeaf
                        .takeIf { it.isWhiteSpaceWithoutNewline20 }
                        ?.takeIf { shouldNotToBeSeparatedBySpace(it.nextLeaf) }
                        ?.let { leaf ->
                            emit(node.startOffset, "Unexpected space after \"${node.text}\"", true)
                                .ifAutocorrectAllowed { leaf.remove() }
                        }
                }

                else -> {
                    return
                }
            }
            when {
                !spacingBefore && !spacingAfter -> {
                    emit(node.startOffset, "Missing spacing around \"${node.text}\"", true)
                        .ifAutocorrectAllowed {
                            node.upsertWhitespaceBeforeMe(" ")
                            node.upsertWhitespaceAfterMe(" ")
                        }
                }

                !spacingBefore -> {
                    emit(node.startOffset, "Missing spacing before \"${node.text}\"", true)
                        .ifAutocorrectAllowed {
                            node.upsertWhitespaceBeforeMe(" ")
                        }
                }

                !spacingAfter -> {
                    emit(node.startOffset + 1, "Missing spacing after \"${node.text}\"", true)
                        .ifAutocorrectAllowed {
                            node.upsertWhitespaceAfterMe(" ")
                        }
                }
            }
        }
    }

    private fun ASTNode.hasUnexpectedNewlineBeforeLbrace(): Boolean =
        also { require(it.elementType == LBRACE) }
            .takeIf { prevLeaf.isWhiteSpaceWithNewline20 }
            ?.let { parent?.elementType == CLASS_BODY || parent?.elementType == BLOCK }
            ?: false

    private fun ASTNode.isPrecededBy(predicate: (ASTNode) -> Boolean) =
        prevLeaf
            ?.let { predicate(it) }
            ?: false

    private fun ASTNode.isPrecededByEolComment() =
        prevLeaf
            ?.isPartOfComment20
            ?: false

    private fun shouldNotToBeSeparatedBySpace(leaf: ASTNode?): Boolean {
        val nextElementType = leaf?.elementType
        return (
            nextElementType == DOT ||
                nextElementType == COMMA ||
                nextElementType == RBRACKET ||
                nextElementType == RPAR ||
                nextElementType == SEMICOLON ||
                nextElementType == SAFE_ACCESS ||
                nextElementType == EXCLEXCL ||
                nextElementType == LBRACKET ||
                nextElementType == LPAR ||
                nextElementType == COLONCOLON ||
                nextElementType == RANGE ||
                nextElementType == RANGE_UNTIL
        )
    }
}

public val SPACING_AROUND_CURLY_RULE_ID: RuleId = SpacingAroundCurlyRule().ruleId
