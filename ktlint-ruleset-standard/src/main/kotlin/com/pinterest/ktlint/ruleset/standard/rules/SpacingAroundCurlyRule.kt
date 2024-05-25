package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.ElementType.AT
import com.pinterest.ktlint.rule.engine.core.api.ElementType.CLASS_BODY
import com.pinterest.ktlint.rule.engine.core.api.ElementType.COLONCOLON
import com.pinterest.ktlint.rule.engine.core.api.ElementType.COMMA
import com.pinterest.ktlint.rule.engine.core.api.ElementType.DOT
import com.pinterest.ktlint.rule.engine.core.api.ElementType.EXCLEXCL
import com.pinterest.ktlint.rule.engine.core.api.ElementType.FUN
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
import com.pinterest.ktlint.rule.engine.core.api.isLeaf
import com.pinterest.ktlint.rule.engine.core.api.isPartOfComment
import com.pinterest.ktlint.rule.engine.core.api.isPartOfString
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpace
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpaceWithNewline
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpaceWithoutNewline
import com.pinterest.ktlint.rule.engine.core.api.leavesIncludingSelf
import com.pinterest.ktlint.rule.engine.core.api.nextLeaf
import com.pinterest.ktlint.rule.engine.core.api.prevLeaf
import com.pinterest.ktlint.rule.engine.core.api.upsertWhitespaceAfterMe
import com.pinterest.ktlint.rule.engine.core.api.upsertWhitespaceBeforeMe
import com.pinterest.ktlint.ruleset.standard.StandardRule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtLambdaExpression

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
        if (node.isLeaf() && !node.isPartOfString()) {
            val prevLeaf = node.prevLeaf()
            val nextLeaf = node.nextLeaf()
            val spacingBefore: Boolean
            val spacingAfter: Boolean
            when (node.elementType) {
                LBRACE -> {
                    spacingBefore =
                        prevLeaf is PsiWhiteSpace ||
                        prevLeaf?.elementType == AT ||
                        (
                            prevLeaf?.elementType == LPAR &&
                                ((node as LeafPsiElement).parent is KtLambdaExpression || node.parent.parent is KtLambdaExpression)
                        )
                    spacingAfter = nextLeaf is PsiWhiteSpace || nextLeaf?.elementType == RBRACE
                    if (prevLeaf is PsiWhiteSpace &&
                        !prevLeaf.textContains('\n') &&
                        prevLeaf.prevLeaf()?.let {
                            it.elementType == LPAR || it.elementType == AT
                        } == true
                    ) {
                        emit(node.startOffset, "Unexpected space before \"${node.text}\"", true)
                            .ifAutocorrectAllowed {
                                prevLeaf.node.treeParent.removeChild(prevLeaf.node)
                            }
                    }
                    prevLeaf
                        ?.takeIf { it.isWhiteSpaceWithNewline() }
                        ?.takeIf {
                            prevLeaf.prevLeaf()?.let { it.elementType == RPAR || KtTokens.KEYWORDS.contains(it.elementType) } == true ||
                                node.treeParent.elementType == CLASS_BODY ||
                                // allow newline for lambda return type
                                (prevLeaf.treeParent.elementType == FUN && prevLeaf.treeNext.elementType != LAMBDA_EXPRESSION)
                        }?.run {
                            emit(node.startOffset, "Unexpected newline before \"${node.text}\"", true)
                                .ifAutocorrectAllowed {
                                    if (isPrecededByEolComment()) {
                                        // All consecutive whitespaces and comments preceding the curly have to be moved after the curly brace
                                        leavesIncludingSelf(forward = false)
                                            .takeWhile { it.isWhiteSpace() || it.isPartOfComment() }
                                            .toList()
                                            .reversed()
                                            .takeIf { it.isNotEmpty() }
                                            ?.let { leavesToMoveAfterCurly ->
                                                node.treeParent.addChildren(
                                                    leavesToMoveAfterCurly.first(),
                                                    leavesToMoveAfterCurly.last(),
                                                    node.treeNext,
                                                )
                                            }
                                    }
                                    (this as LeafPsiElement).rawReplaceWithText(" ")
                                }
                        }
                }

                RBRACE -> {
                    spacingBefore = prevLeaf is PsiWhiteSpace || prevLeaf?.elementType == LBRACE
                    spacingAfter = nextLeaf == null || nextLeaf is PsiWhiteSpace || shouldNotToBeSeparatedBySpace(nextLeaf)
                    nextLeaf
                        .takeIf { it.isWhiteSpaceWithoutNewline() }
                        ?.takeIf { shouldNotToBeSeparatedBySpace(it.nextLeaf()) }
                        ?.let { leaf ->
                            emit(node.startOffset, "Unexpected space after \"${node.text}\"", true)
                                .ifAutocorrectAllowed {
                                    leaf.treeParent.removeChild(leaf)
                                }
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

    private fun ASTNode.isPrecededByEolComment() =
        prevLeaf()
            ?.isPartOfComment()
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
