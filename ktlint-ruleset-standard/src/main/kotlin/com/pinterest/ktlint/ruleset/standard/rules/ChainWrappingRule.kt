package com.pinterest.ktlint.ruleset.standard.rules

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
import com.pinterest.ktlint.rule.engine.core.api.ElementType.OROR
import com.pinterest.ktlint.rule.engine.core.api.ElementType.PERC
import com.pinterest.ktlint.rule.engine.core.api.ElementType.PLUS
import com.pinterest.ktlint.rule.engine.core.api.ElementType.PREFIX_EXPRESSION
import com.pinterest.ktlint.rule.engine.core.api.ElementType.SAFE_ACCESS
import com.pinterest.ktlint.rule.engine.core.api.ElementType.WHITE_SPACE
import com.pinterest.ktlint.rule.engine.core.api.IndentConfig
import com.pinterest.ktlint.rule.engine.core.api.IndentConfig.Companion.DEFAULT_INDENT_CONFIG
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.EditorConfig
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.INDENT_SIZE_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.INDENT_STYLE_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.indent
import com.pinterest.ktlint.rule.engine.core.api.isPartOfComment
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpaceWithNewline
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpaceWithoutNewline
import com.pinterest.ktlint.rule.engine.core.api.nextCodeLeaf
import com.pinterest.ktlint.rule.engine.core.api.nextLeaf
import com.pinterest.ktlint.rule.engine.core.api.prevCodeLeaf
import com.pinterest.ktlint.rule.engine.core.api.prevLeaf
import com.pinterest.ktlint.rule.engine.core.api.upsertWhitespaceAfterMe
import com.pinterest.ktlint.rule.engine.core.api.upsertWhitespaceBeforeMe
import com.pinterest.ktlint.ruleset.standard.StandardRule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafElement
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.com.intellij.psi.tree.TokenSet
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.psiUtil.leaves

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
    private val noSpaceAroundTokens = TokenSet.create(DOT, SAFE_ACCESS)
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
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
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
                if (autoCorrect) {
                    // rewriting
                    // <prevLeaf><node="."><nextLeaf="\n"> to
                    // <prevLeaf><delete space if any><nextLeaf="\n"><node="."><space if needed>
                    // (or)
                    // <prevLeaf><node="."><spaceBeforeComment><comment><nextLeaf="\n"> to
                    // <prevLeaf><delete space if any><spaceBeforeComment><comment><nextLeaf="\n"><node="."><space if needed>
                    if (node.elementType == ELVIS) {
                        node.upsertWhitespaceBeforeMe(node.indent().plus(indentConfig.indent))
                        node.upsertWhitespaceAfterMe(" ")
                    } else {
                        node.treeParent.removeChild(node)
                        (nextLeaf as LeafElement).rawInsertAfterMe(node as LeafElement)
                    }
                }
            }
        } else if (sameLineTokens.contains(elementType) || prefixTokens.contains(elementType)) {
            if (node.isPartOfComment()) {
                return
            }
            val prevLeaf = node.prevLeaf()
            if (
                prevLeaf?.elementType == WHITE_SPACE &&
                prevLeaf.textContains('\n') &&
                // fn(*typedArray<...>()) case
                (elementType != MUL || !prevLeaf.isPartOfSpread()) &&
                // unary +/-
                (!prefixTokens.contains(elementType) || !node.isInPrefixPosition())
            ) {
                emit(node.startOffset, "Line must not begin with \"${node.text}\"", true)
                if (autoCorrect) {
                    // rewriting
                    // <insertionPoint><prevLeaf="\n"><node="&&"><nextLeaf=" "> to
                    // <insertionPoint><prevLeaf=" "><node="&&"><nextLeaf="\n"><delete node="&&"><delete nextLeaf=" ">
                    // (or)
                    // <insertionPoint><spaceBeforeComment><comment><prevLeaf="\n"><node="&&"><nextLeaf=" "> to
                    // <insertionPoint><space if needed><node="&&"><spaceBeforeComment><comment><prevLeaf="\n"><delete node="&&"><delete nextLeaf=" ">
                    val nextLeaf = node.nextLeaf()
                    if (nextLeaf is PsiWhiteSpace) {
                        nextLeaf.node.treeParent.removeChild(nextLeaf.node)
                    }
                    val insertionPoint = prevLeaf.prevCodeLeaf() as LeafPsiElement
                    (node as LeafPsiElement).treeParent.removeChild(node)
                    insertionPoint.rawInsertAfterMe(node)
                    (insertionPoint as ASTNode).upsertWhitespaceAfterMe(" ")
                }
            }
        }
    }

    private fun ASTNode.isPartOfSpread() =
        prevCodeLeaf()?.let { leaf ->
            val type = leaf.elementType
            type == LPAR ||
                type == COMMA ||
                type == LBRACE ||
                type == ELSE_KEYWORD ||
                KtTokens.OPERATIONS.contains(type)
        } == true

    private fun ASTNode.isInPrefixPosition() = treeParent?.treeParent?.elementType == PREFIX_EXPRESSION

    private fun ASTNode.isElvisOperatorAndComment(): Boolean {
        return elementType == ELVIS &&
            leaves().takeWhile { it.isWhiteSpaceWithoutNewline() || it.isPartOfComment() }.any()
    }
}

public val CHAIN_WRAPPING_RULE_ID: RuleId = ChainWrappingRule().ruleId
