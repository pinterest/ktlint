package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.core.IndentConfig
import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.api.EditorConfigProperties
import com.pinterest.ktlint.core.api.UsesEditorConfigProperties
import com.pinterest.ktlint.core.api.editorconfig.EditorConfigProperty
import com.pinterest.ktlint.core.api.editorconfig.INDENT_SIZE_PROPERTY
import com.pinterest.ktlint.core.api.editorconfig.INDENT_STYLE_PROPERTY
import com.pinterest.ktlint.core.ast.ElementType.ANDAND
import com.pinterest.ktlint.core.ast.ElementType.COMMA
import com.pinterest.ktlint.core.ast.ElementType.DIV
import com.pinterest.ktlint.core.ast.ElementType.DOT
import com.pinterest.ktlint.core.ast.ElementType.ELSE_KEYWORD
import com.pinterest.ktlint.core.ast.ElementType.ELVIS
import com.pinterest.ktlint.core.ast.ElementType.LBRACE
import com.pinterest.ktlint.core.ast.ElementType.LPAR
import com.pinterest.ktlint.core.ast.ElementType.MINUS
import com.pinterest.ktlint.core.ast.ElementType.MUL
import com.pinterest.ktlint.core.ast.ElementType.OROR
import com.pinterest.ktlint.core.ast.ElementType.PERC
import com.pinterest.ktlint.core.ast.ElementType.PLUS
import com.pinterest.ktlint.core.ast.ElementType.PREFIX_EXPRESSION
import com.pinterest.ktlint.core.ast.ElementType.SAFE_ACCESS
import com.pinterest.ktlint.core.ast.ElementType.WHITE_SPACE
import com.pinterest.ktlint.core.ast.isPartOfComment
import com.pinterest.ktlint.core.ast.isWhiteSpaceWithNewline
import com.pinterest.ktlint.core.ast.isWhiteSpaceWithoutNewline
import com.pinterest.ktlint.core.ast.lineIndent
import com.pinterest.ktlint.core.ast.nextCodeLeaf
import com.pinterest.ktlint.core.ast.nextLeaf
import com.pinterest.ktlint.core.ast.prevCodeLeaf
import com.pinterest.ktlint.core.ast.prevLeaf
import com.pinterest.ktlint.core.ast.upsertWhitespaceAfterMe
import com.pinterest.ktlint.core.ast.upsertWhitespaceBeforeMe
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafElement
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.com.intellij.psi.tree.TokenSet
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.psiUtil.leaves

public class ChainWrappingRule :
    Rule("chain-wrapping"),
    UsesEditorConfigProperties {
    override val editorConfigProperties: List<EditorConfigProperty<*>> =
        listOf(
            INDENT_SIZE_PROPERTY,
            INDENT_STYLE_PROPERTY,
        )

    private var indent: String? = null
    private val sameLineTokens = TokenSet.create(MUL, DIV, PERC, ANDAND, OROR)
    private val prefixTokens = TokenSet.create(PLUS, MINUS)
    private val nextLineTokens = TokenSet.create(DOT, SAFE_ACCESS, ELVIS)
    private val noSpaceAroundTokens = TokenSet.create(DOT, SAFE_ACCESS)

    override fun beforeFirstNode(editorConfigProperties: EditorConfigProperties) {
        with(editorConfigProperties) {
            val indentConfig = IndentConfig(
                indentStyle = getEditorConfigValue(INDENT_STYLE_PROPERTY),
                tabWidth = getEditorConfigValue(INDENT_SIZE_PROPERTY),
            )
            indent = indentConfig.indent
        }
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
                        node.upsertWhitespaceBeforeMe("\n" + node.lineIndent() + indent)
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

    private fun ASTNode.isInPrefixPosition() =
        treeParent?.treeParent?.elementType == PREFIX_EXPRESSION

    private fun ASTNode.isElvisOperatorAndComment(): Boolean {
        return elementType == ELVIS &&
            leaves().takeWhile { it.isWhiteSpaceWithoutNewline() || it.isPartOfComment() }.any()
    }
}
