package com.pinterest.ktlint.ruleset.experimental

import com.pinterest.ktlint.core.EditorConfig
import com.pinterest.ktlint.core.KtLint
import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.ast.ElementType.BLOCK
import com.pinterest.ktlint.core.ast.ElementType.COLON
import com.pinterest.ktlint.core.ast.ElementType.DOT_QUALIFIED_EXPRESSION
import com.pinterest.ktlint.core.ast.ElementType.FUN
import com.pinterest.ktlint.core.ast.ElementType.FUN_KEYWORD
import com.pinterest.ktlint.core.ast.ElementType.LPAR
import com.pinterest.ktlint.core.ast.ElementType.RPAR
import com.pinterest.ktlint.core.ast.ElementType.TYPE_REFERENCE
import com.pinterest.ktlint.core.ast.ElementType.VALUE_PARAMETER_LIST
import com.pinterest.ktlint.core.ast.ElementType.WHITE_SPACE
import com.pinterest.ktlint.core.ast.children
import com.pinterest.ktlint.core.ast.isRoot
import com.pinterest.ktlint.core.ast.prevLeaf
import com.pinterest.ktlint.core.ast.upsertWhitespaceAfterMe
import com.pinterest.ktlint.core.ast.upsertWhitespaceBeforeMe
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafElement
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement

public class FunctionReturnTypeWrappingRule : Rule("function-return-type-wrapping") {
    private var indent: String? = null
    private var maxLineLength = -1

    override fun visit(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit
    ) {
        if (node.isRoot()) {
            val editorConfig = node.getUserData(KtLint.EDITOR_CONFIG_USER_DATA_KEY)!!
            indent = when (editorConfig.indentStyle) {
                EditorConfig.IndentStyle.SPACE -> " ".repeat(editorConfig.indentSize)
                EditorConfig.IndentStyle.TAB -> "\t"
            }
            maxLineLength = editorConfig.maxLineLength
            return
        }
        if (node.elementType == FUN) {
            val functionSignature = node.getSignature()
            if (functionSignature.contains('\n') || functionSignature.exceedsMaxLineLength()) {
                visitOpeningParenthesis(node, emit, autoCorrect)
                visitClosingParenthesisAndReturnType(node, emit, autoCorrect)
            }
        }
    }

    private fun ASTNode.getSignature(): String {
        require(elementType == FUN)

        val iterator = children().iterator()
        var currentNode: ASTNode? = null

        while (iterator.hasNext()) {
            currentNode = iterator.next()
            if (currentNode.elementType == FUN_KEYWORD) {
                break
            }
        }
        val indentBeforeFunKeyword =
            currentNode
                ?.prevLeaf()
                ?.takeIf { it.elementType == WHITE_SPACE }
                ?.text
                ?.substringAfter('\n')
                ?: ""

        var signature = indentBeforeFunKeyword + currentNode?.text
        while (iterator.hasNext()) {
            currentNode = iterator.next()
            if (currentNode.elementType == BLOCK || currentNode.elementType == DOT_QUALIFIED_EXPRESSION) {
                signature = signature.trimEnd()
                break
            }
            signature += currentNode.text
        }
        return signature
    }

    private fun String.exceedsMaxLineLength(): Boolean =
        isMaxLineLengthSet() && length > maxLineLength

    private fun isMaxLineLengthSet() = maxLineLength > -1

    private fun visitOpeningParenthesis(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
        autoCorrect: Boolean
    ) {
        val openingParentheses =
            node
                .findChildByType(VALUE_PARAMETER_LIST)
                ?.findChildByType(LPAR)
        if (openingParentheses != null && node.hasNonEmptyValueParameterList()) {
            emit(openingParentheses.startOffset, ERROR_PARAMETERS_ON_SEPARATE_LINES, true)
            if (autoCorrect) {
                (openingParentheses as LeafPsiElement).upsertWhitespaceAfterMe("\n$indent")
                // Rules 'parameter-list-wrapping' and 'indent' will take care of the individual parameters
            }
        }
    }

    private fun ASTNode.hasNonEmptyValueParameterList(): Boolean =
        findChildByType(VALUE_PARAMETER_LIST)
            ?.children()
            ?.any { it.elementType != LPAR && it.elementType != RPAR }
            ?: false

    private fun visitClosingParenthesisAndReturnType(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
        autoCorrect: Boolean
    ) {
        val closingParentheses =
            node
                .findChildByType(VALUE_PARAMETER_LIST)
                ?.findChildByType(RPAR)
        val typeReferenceNode = node.findChildByType(TYPE_REFERENCE)
        if (closingParentheses != null && typeReferenceNode != null) {
            emit(closingParentheses.startOffset, ERROR_RETURN_TYPE_ON_SAME_LINE_AS_CLOSING_PARENTHESES, true)
            if (autoCorrect) {
                // Force closing parenthesis on a new line
                closingParentheses.forceToNewLine()
                node.removeWhiteSpaceBetweenClosingParenthesisAndColon()
                typeReferenceNode.fixWhiteSpaceBetweenColonAndReturnType()
            }
        }
    }

    private fun ASTNode.forceToNewLine() {
        require(elementType == RPAR)
        (this as LeafPsiElement).upsertWhitespaceBeforeMe("\n")
    }

    private fun ASTNode.removeWhiteSpaceBetweenClosingParenthesisAndColon() =
        findChildByType(COLON)
            ?.prevLeaf()
            ?.takeIf { it.elementType == WHITE_SPACE }
            ?.let { whitespaceBeforeColonNode ->
                whitespaceBeforeColonNode.treeParent?.removeChild(whitespaceBeforeColonNode)
            }

    private fun ASTNode.fixWhiteSpaceBetweenColonAndReturnType() {
        require(elementType == TYPE_REFERENCE)
        prevLeaf()
            ?.takeIf { it.elementType == WHITE_SPACE && it.text != " " }
            ?.let {
                (it as LeafElement).rawReplaceWithText(" ")
            }
    }

    private companion object {
        const val ERROR_PARAMETERS_ON_SEPARATE_LINES = "Parameters should be on a separate line (unless entire function signature fits on a single line)"
        const val ERROR_RETURN_TYPE_ON_SAME_LINE_AS_CLOSING_PARENTHESES = "Return type should be on separate line with closing parentheses (unless entire function signature fits on a single line)"
    }
}
