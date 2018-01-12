package com.github.shyiko.ktlint.ruleset.standard

import com.github.shyiko.ktlint.core.IndentationConfig
import com.github.shyiko.ktlint.core.Rule
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

    private var indentConfig = IndentationConfig(-1, -1, true)

    override fun visit(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit) {
        if (node.elementType == KtStubElementTypes.FILE) {
            indentConfig = IndentationConfig.create(node)
            return
        }
        if (indentConfig.disabled) {
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
                val previousIndent = node.calculatePreviousIndent()
                val expectedIndentSize =
                    if (indentConfig.continuation == indentConfig.regular || shouldUseContinuationIndent(node)) {
                        indentConfig.continuation
                    } else {
                        indentConfig.regular
                    }
                lines.tail().forEach { line ->
                    if (node.isPartOf(KtParameterList::class)
                        && node.nextSibling is KtParameter
                        && firstParameter.value?.node != node.nextSibling.node) {
                        if ((line.length + 1) != firstParameterColumn.value) {
                            emit(offset, "Unexpected indentation (${line.length}) (" +
                                "parameters should be vertically aligned)", true)
                        }
                    } else if (line.isNotEmpty() && (line.length - previousIndent) % expectedIndentSize != 0) {

                        emit(offset,
                            "Unexpected indentation (${line.length - previousIndent}) " +
                                "(it should be $expectedIndentSize)",
                            false)
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
}
