package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.ElementType.ANDAND
import com.pinterest.ktlint.rule.engine.core.api.ElementType.COMMA
import com.pinterest.ktlint.rule.engine.core.api.ElementType.DIV
import com.pinterest.ktlint.rule.engine.core.api.ElementType.DOT
import com.pinterest.ktlint.rule.engine.core.api.ElementType.ELSE_KEYWORD
import com.pinterest.ktlint.rule.engine.core.api.ElementType.ELVIS
import com.pinterest.ktlint.rule.engine.core.api.ElementType.LBRACE
import com.pinterest.ktlint.rule.engine.core.api.ElementType.LPAR
import com.pinterest.ktlint.rule.engine.core.api.ElementType.MINUS
import com.pinterest.ktlint.rule.engine.core.api.ElementType.MUL
import com.pinterest.ktlint.rule.engine.core.api.ElementType.OPERATION_REFERENCE
import com.pinterest.ktlint.rule.engine.core.api.ElementType.OROR
import com.pinterest.ktlint.rule.engine.core.api.ElementType.PERC
import com.pinterest.ktlint.rule.engine.core.api.ElementType.PLUS
import com.pinterest.ktlint.rule.engine.core.api.ElementType.PREFIX_EXPRESSION
import com.pinterest.ktlint.rule.engine.core.api.ElementType.SAFE_ACCESS
import com.pinterest.ktlint.rule.engine.core.api.IndentConfig
import com.pinterest.ktlint.rule.engine.core.api.IndentConfig.Companion.DEFAULT_INDENT_CONFIG
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint.Status.STABLE
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.EditorConfig
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.INDENT_SIZE_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.INDENT_STYLE_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.ifAutocorrectAllowed
import com.pinterest.ktlint.rule.engine.core.api.isPartOfComment
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpaceWithNewline
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpaceWithoutNewline
import com.pinterest.ktlint.rule.engine.core.api.nextCodeLeaf
import com.pinterest.ktlint.rule.engine.core.api.nextLeaf
import com.pinterest.ktlint.rule.engine.core.api.nextSibling
import com.pinterest.ktlint.rule.engine.core.api.prevCodeLeaf
import com.pinterest.ktlint.rule.engine.core.api.prevCodeSibling
import com.pinterest.ktlint.rule.engine.core.api.prevLeaf
import com.pinterest.ktlint.rule.engine.core.api.remove
import com.pinterest.ktlint.rule.engine.core.api.upsertWhitespaceAfterMe
import com.pinterest.ktlint.rule.engine.core.api.upsertWhitespaceBeforeMe
import com.pinterest.ktlint.ruleset.standard.StandardRule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafElement
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.com.intellij.psi.tree.TokenSet
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.psiUtil.leaves

@SinceKtlint("0.13", STABLE)
public class ChainWrappingRule :
    StandardRule(
        id = "chain-wrapping",
        usesEditorConfigProperties =
            setOf(
                INDENT_SIZE_PROPERTY,
                INDENT_STYLE_PROPERTY,
            ),
    ) {
    private val sameLineTokens = TokenSet.create(MUL, DIV, PERC, ANDAND, OROR)
    private val prefixTokens = TokenSet.create(PLUS, MINUS)
    private val nextLineTokens = TokenSet.create(DOT, SAFE_ACCESS, ELVIS)
    private var indentConfig = DEFAULT_INDENT_CONFIG

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
        /*
           org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement (DOT) | "."
           org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.PsiWhiteSpaceImpl (WHITE_SPACE) | "\n        "
           org.jetbrains.kotlin.psi.KtCallExpression (CALL_EXPRESSION)
         */
        val elementType = node.elementType
        if (nextLineTokens.contains(elementType)) {
            if (node.isPartOfComment()) {
                return
            }
            val nextLeaf = node.nextCodeLeaf()?.prevLeaf()
            if (nextLeaf.isWhiteSpaceWithNewline() && !node.isElvisOperatorAndComment()) {
                emit(node.startOffset, "Line must not end with \"${node.text}\"", true)
                    .ifAutocorrectAllowed {
                        // rewriting
                        // <prevLeaf><node="."><nextLeaf="\n"> to
                        // <prevLeaf><delete space if any><nextLeaf="\n"><node="."><space if needed>
                        // (or)
                        // <prevLeaf><node="."><spaceBeforeComment><comment><nextLeaf="\n"> to
                        // <prevLeaf><delete space if any><spaceBeforeComment><comment><nextLeaf="\n"><node="."><space if needed>
                        if (node.elementType == ELVIS) {
                            node.upsertWhitespaceBeforeMe(indentConfig.childIndentOf(node))
                            node.upsertWhitespaceAfterMe(" ")
                        } else {
                            node.remove()
                            (nextLeaf as LeafElement).rawInsertAfterMe(node as LeafElement)
                        }
                    }
            }
        } else if (sameLineTokens.contains(elementType) || prefixTokens.contains(elementType)) {
            if (node.isPartOfComment()) {
                return
            }
            val prevLeaf = node.prevLeaf()
            if (node.isPartOfSpread()) {
                // Allow:
                //    fn(
                //        *typedArray<...>()
                //    )
                return
            }
            if (prefixTokens.contains(elementType) && node.isInPrefixPosition()) {
                // Allow:
                //    fn(
                //        -42
                //    )
                return
            }

            if (prevLeaf != null && prevLeaf.isWhiteSpaceWithNewline()) {
                emit(node.startOffset, "Line must not begin with \"${node.text}\"", true)
                    .ifAutocorrectAllowed {
                        // rewriting
                        // <insertionPoint><prevLeaf="\n"><node="&&"><nextLeaf=" "> to
                        // <insertionPoint><prevLeaf=" "><node="&&"><nextLeaf="\n"><delete node="&&"><delete nextLeaf=" ">
                        // (or)
                        // <insertionPoint><spaceBeforeComment><comment><prevLeaf="\n"><node="&&"><nextLeaf=" "> to
                        // <insertionPoint><space if needed><node="&&"><spaceBeforeComment><comment><prevLeaf="\n"><delete node="&&"><delete nextLeaf=" ">
                        val nextLeaf = node.nextLeaf()
                        val whiteSpaceToBeDeleted =
                            when {
                                nextLeaf.isWhiteSpaceWithNewline() -> {
                                    // Node is preceded and followed by whitespace. Prefer to remove the whitespace before the node as this will
                                    // change the indent of the next line
                                    prevLeaf
                                }

                                nextLeaf.isWhiteSpaceWithoutNewline() -> {
                                    nextLeaf
                                }

                                else -> {
                                    null
                                }
                            }

                        if (node.treeParent.elementType == OPERATION_REFERENCE) {
                            val operationReference = node.treeParent
                            val insertBeforeSibling =
                                operationReference
                                    .prevCodeSibling()
                                    ?.nextSibling()
                            operationReference.remove()
                            insertBeforeSibling?.treeParent?.addChild(operationReference, insertBeforeSibling)
                            node.treeParent.upsertWhitespaceBeforeMe(" ")
                        } else {
                            val insertionPoint = prevLeaf.prevCodeLeaf() as LeafPsiElement
                            (node as LeafPsiElement).remove()
                            insertionPoint.rawInsertAfterMe(node)
                            (insertionPoint as ASTNode).upsertWhitespaceAfterMe(" ")
                        }
                        whiteSpaceToBeDeleted
                            ?.treeParent
                            ?.removeChild(whiteSpaceToBeDeleted)
                    }
            }
        }
    }

    private fun ASTNode.isPartOfSpread() =
        elementType == MUL &&
            prevCodeLeaf()
                ?.let { leaf ->
                    val type = leaf.elementType
                    type == LPAR ||
                        type == COMMA ||
                        type == LBRACE ||
                        type == ELSE_KEYWORD ||
                        KtTokens.OPERATIONS.contains(type)
                } == true

    private fun ASTNode.isInPrefixPosition() = treeParent?.treeParent?.elementType == PREFIX_EXPRESSION

    private fun ASTNode.isElvisOperatorAndComment(): Boolean =
        elementType == ELVIS &&
            leaves().takeWhile { it.isWhiteSpaceWithoutNewline() || it.isPartOfComment() }.any()
}

public val CHAIN_WRAPPING_RULE_ID: RuleId = ChainWrappingRule().ruleId
