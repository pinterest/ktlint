package com.github.shyiko.ktlint.ruleset.standard

import com.github.shyiko.ktlint.core.KtLint
import com.github.shyiko.ktlint.core.Rule
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.PsiComment
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtParameterList
import org.jetbrains.kotlin.psi.KtParenthesizedExpression
import org.jetbrains.kotlin.psi.KtSafeQualifiedExpression
import org.jetbrains.kotlin.psi.KtSecondaryConstructor
import org.jetbrains.kotlin.psi.KtSuperTypeList
import org.jetbrains.kotlin.psi.KtSuperTypeListEntry
import org.jetbrains.kotlin.psi.KtTypeProjection
import org.jetbrains.kotlin.psi.KtValueArgumentList
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType
import org.jetbrains.kotlin.psi.psiUtil.getPrevSiblingIgnoringWhitespaceAndComments
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes

class IndentationRule : Rule("indent") {

    companion object {
        // indentation size recommended by JetBrains
        private const val DEFAULT_INDENT = 4
        private const val DEFAULT_CONTINUATION_INDENT = 4
    }

    private var indent = -1
    private var continuationIndent = -1

    override fun visit(node: ASTNode, autoCorrect: Boolean,
            emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit) {
        if (node.elementType == KtStubElementTypes.FILE) {
            val android = node.getUserData(KtLint.ANDROID_USER_DATA_KEY)!!
            val editorConfig = node.getUserData(KtLint.EDITOR_CONFIG_USER_DATA_KEY)!!
            val indentSize = editorConfig.get("indent_size")
            val continuationIndentSize = editorConfig.get("continuation_indent_size")
            indent = indentSize?.toIntOrNull() ?: if (indentSize?.toLowerCase() == "unset") -1 else DEFAULT_INDENT
            continuationIndent = continuationIndentSize?.toIntOrNull() ?: DEFAULT_CONTINUATION_INDENT
            return
        }
        if (indent <= 0 || continuationIndent <= 0) {
            return
        }
        if (node is PsiWhiteSpace && !node.isPartOf(PsiComment::class)) {
            val lines = node.getText().split("\n")
            if (lines.size > 1) {
                var offset = node.startOffset + lines.first().length + 1
                val firstParameter = lazy {
                    PsiTreeUtil.findChildOfType(
                        node.getNonStrictParentOfType(KtParameterList::class.java),
                        KtParameter::class.java
                    )
                }
                val firstParameterColumn = lazy { firstParameter.value?.column ?: 0 }
                val previousIndent = calculatePreviousIndent(node)
                val expectedIndentSize = if (continuationIndent == indent || shouldUseContinuationIndent(node))
                    continuationIndent else indent
                lines.tail().forEach { line ->
                    if (line.isNotEmpty() && (line.length - previousIndent) % expectedIndentSize != 0) {
                        if (node.isPartOf(KtParameterList::class) &&
                            firstParameterColumn.value != 0 &&
                            (
                                // is not the first parameter
                                node.parent.node.findChildByType(KtStubElementTypes.VALUE_PARAMETER) !=
                                firstParameter.value?.node ||
                                // ... or is next to (
                                firstParameter.value?.let { PsiTreeUtil.prevLeaf(it, true) }?.node
                                    ?.elementType == KtTokens.LPAR)
                            ) {
                            if (firstParameterColumn.value - 1 != line.length) {
                                emit(offset, "Unexpected indentation (${line.length}) (" +
                                    "parameters should be either vertically aligned or " +
                                    "indented by the multiple of $indent" +
                                    ")", false)
                            }
                        } else {
                            emit(offset,
                                "Unexpected indentation (${line.length - previousIndent}) " +
                                    "(it should be $expectedIndentSize)",
                                false)
                        }
                    }
                    offset += line.length + 1
                }
            }
        }
    }

    private val PsiElement.column: Int
        get() {
            var leaf = PsiTreeUtil.prevLeaf(this)
            var offsetToTheLeft = 0
            while (leaf != null) {
                if (leaf.node.elementType == KtTokens.WHITE_SPACE && leaf.textContains('\n')) {
                    offsetToTheLeft += leaf.textLength - 1 - leaf.text.lastIndexOf('\n')
                    break
                }
                offsetToTheLeft += leaf.textLength
                leaf = PsiTreeUtil.prevLeaf(leaf)
            }
            return offsetToTheLeft + 1
        }

    private fun shouldUseContinuationIndent(node: PsiWhiteSpace): Boolean {
        val parentNode = node.parent
        val prevNode = node.getPrevSiblingIgnoringWhitespaceAndComments()?.node?.elementType
        val nextNode = node.nextSibling?.node?.elementType
        return (
            prevNode in KtTokens.ALL_ASSIGNMENTS
                || parentNode is KtSecondaryConstructor
                || nextNode == KtStubElementTypes.TYPE_REFERENCE
                || node.nextSibling is KtSuperTypeList
                || node.nextSibling is KtSuperTypeListEntry
                || node.nextSibling is KtTypeProjection
                || parentNode is KtValueArgumentList
                || parentNode is KtBinaryExpression
                || parentNode is KtDotQualifiedExpression
                || parentNode is KtSafeQualifiedExpression
                || parentNode is KtParenthesizedExpression
            )
    }

    private fun calculatePreviousIndent(node: ASTNode): Int {
        val parentNode = node.treeParent?.psi
        var prevIndent = 0
        var prevSibling = parentNode
        var prevSpaceIsFound = false
        while (prevSibling != null && !prevSpaceIsFound) {
            val nextNode = prevSibling.nextSibling?.node?.elementType
            if (prevSibling is PsiWhiteSpace
                && nextNode != KtStubElementTypes.TYPE_REFERENCE
                && nextNode != KtStubElementTypes.SUPER_TYPE_LIST
                && nextNode != KtNodeTypes.CONSTRUCTOR_DELEGATION_CALL) {
                val prevLines = prevSibling.text.split('\n')
                if (prevLines.size > 1) {
                    prevIndent = prevLines.last().length
                    prevSpaceIsFound = true
                }
            }
            prevSibling = if (prevSpaceIsFound) {
                null
            } else {
                if (prevSibling.prevSibling != null) {
                    prevSibling.prevSibling
                } else {
                    prevSibling.parent
                }
            }
        }
        return prevIndent
    }
}
