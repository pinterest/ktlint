package io.github.ktlint.core.ruleset.standard.rules

import io.github.ktlint.core.rule.engine.core.api.AutocorrectDecision
import io.github.ktlint.core.rule.engine.core.api.ElementType.AT
import io.github.ktlint.core.rule.engine.core.api.ElementType.BLOCK
import io.github.ktlint.core.rule.engine.core.api.ElementType.CLASS_BODY
import io.github.ktlint.core.rule.engine.core.api.ElementType.COLONCOLON
import io.github.ktlint.core.rule.engine.core.api.ElementType.COMMA
import io.github.ktlint.core.rule.engine.core.api.ElementType.DOT
import io.github.ktlint.core.rule.engine.core.api.ElementType.EXCLEXCL
import io.github.ktlint.core.rule.engine.core.api.ElementType.LAMBDA_EXPRESSION
import io.github.ktlint.core.rule.engine.core.api.ElementType.LBRACE
import io.github.ktlint.core.rule.engine.core.api.ElementType.LBRACKET
import io.github.ktlint.core.rule.engine.core.api.ElementType.LPAR
import io.github.ktlint.core.rule.engine.core.api.ElementType.RANGE
import io.github.ktlint.core.rule.engine.core.api.ElementType.RANGE_UNTIL
import io.github.ktlint.core.rule.engine.core.api.ElementType.RBRACE
import io.github.ktlint.core.rule.engine.core.api.ElementType.RBRACKET
import io.github.ktlint.core.rule.engine.core.api.ElementType.RPAR
import io.github.ktlint.core.rule.engine.core.api.ElementType.SAFE_ACCESS
import io.github.ktlint.core.rule.engine.core.api.ElementType.SEMICOLON
import io.github.ktlint.core.rule.engine.core.api.IndentConfig
import io.github.ktlint.core.rule.engine.core.api.RuleId
import io.github.ktlint.core.rule.engine.core.api.SinceKtlint
import io.github.ktlint.core.rule.engine.core.api.SinceKtlint.Status.STABLE
import io.github.ktlint.core.rule.engine.core.api.editorconfig.CODE_STYLE_PROPERTY
import io.github.ktlint.core.rule.engine.core.api.editorconfig.EditorConfig
import io.github.ktlint.core.rule.engine.core.api.editorconfig.INDENT_SIZE_PROPERTY
import io.github.ktlint.core.rule.engine.core.api.editorconfig.INDENT_STYLE_PROPERTY
import io.github.ktlint.core.rule.engine.core.api.ifAutocorrectAllowed
import io.github.ktlint.core.rule.engine.core.api.isCode
import io.github.ktlint.core.rule.engine.core.api.isLeaf
import io.github.ktlint.core.rule.engine.core.api.isPartOfComment
import io.github.ktlint.core.rule.engine.core.api.isPartOfString
import io.github.ktlint.core.rule.engine.core.api.isWhiteSpace
import io.github.ktlint.core.rule.engine.core.api.isWhiteSpaceWithNewline
import io.github.ktlint.core.rule.engine.core.api.isWhiteSpaceWithoutNewline
import io.github.ktlint.core.rule.engine.core.api.leavesBackwardsIncludingSelf
import io.github.ktlint.core.rule.engine.core.api.nextLeaf
import io.github.ktlint.core.rule.engine.core.api.nextSibling
import io.github.ktlint.core.rule.engine.core.api.parent
import io.github.ktlint.core.rule.engine.core.api.prevLeaf
import io.github.ktlint.core.rule.engine.core.api.remove
import io.github.ktlint.core.rule.engine.core.api.replaceTextWith
import io.github.ktlint.core.rule.engine.core.api.upsertWhitespaceAfterMe
import io.github.ktlint.core.rule.engine.core.api.upsertWhitespaceBeforeMe
import io.github.ktlint.core.ruleset.standard.StandardRule
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
        if (node.isLeaf && !node.isPartOfString) {
            val prevLeaf = node.prevLeaf
            val nextLeaf = node.nextLeaf
            val spacingBefore: Boolean
            val spacingAfter: Boolean
            when (node.elementType) {
                LBRACE -> {
                    spacingBefore =
                        prevLeaf.isWhiteSpace ||
                        prevLeaf?.elementType == AT ||
                        (
                            (prevLeaf?.elementType == LPAR || prevLeaf?.elementType == LBRACKET) &&
                                (
                                    node.parent?.elementType == LAMBDA_EXPRESSION ||
                                        node.parent?.parent?.elementType == LAMBDA_EXPRESSION
                                )
                        )
                    spacingAfter = nextLeaf.isWhiteSpace || nextLeaf?.elementType == RBRACE
                    if (prevLeaf.isWhiteSpaceWithoutNewline &&
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
                                                        node.nextSibling,
                                                    )
                                                }
                                        }
                                        replaceTextWith(" ")
                                    }
                            }
                    }
                }

                RBRACE -> {
                    spacingBefore = prevLeaf.isWhiteSpace || prevLeaf?.elementType == LBRACE
                    spacingAfter = nextLeaf == null || nextLeaf.isWhiteSpace || shouldNotToBeSeparatedBySpace(nextLeaf)
                    nextLeaf
                        .takeIf { it.isWhiteSpaceWithoutNewline }
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
            .takeIf { prevLeaf.isWhiteSpaceWithNewline }
            ?.let { parent?.elementType == CLASS_BODY || parent?.elementType == BLOCK }
            ?: false

    private fun ASTNode.isPrecededBy(predicate: (ASTNode) -> Boolean) =
        prevLeaf
            ?.let { predicate(it) }
            ?: false

    private fun ASTNode.isPrecededByEolComment() =
        prevLeaf
            ?.isPartOfComment
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
