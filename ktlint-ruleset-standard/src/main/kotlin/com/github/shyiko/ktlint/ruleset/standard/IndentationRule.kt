package com.github.shyiko.ktlint.ruleset.standard

import com.github.shyiko.ktlint.core.Rule
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.lang.FileASTNode
import org.jetbrains.kotlin.com.intellij.psi.PsiComment
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtParameterList
import org.jetbrains.kotlin.psi.KtParenthesizedExpression
import org.jetbrains.kotlin.psi.KtSafeQualifiedExpression
import org.jetbrains.kotlin.psi.KtSecondaryConstructor
import org.jetbrains.kotlin.psi.KtSuperTypeList
import org.jetbrains.kotlin.psi.KtSuperTypeListEntry
import org.jetbrains.kotlin.psi.KtTypeProjection
import org.jetbrains.kotlin.psi.KtValueArgumentList
import org.jetbrains.kotlin.psi.psiUtil.getPrevSiblingIgnoringWhitespaceAndComments
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes

class IndentationRule : Rule("indent") {

    private var indentSize = -1
    private var continuationIndentSize = -1

    override fun visit(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit
    ) {
        if (node.elementType == KtStubElementTypes.FILE) {
            val ec = EditorConfig.from(node as FileASTNode)
            indentSize = ec.indentSize
            continuationIndentSize = ec.continuationIndentSize
            return
        }
        if (indentSize <= 0 || continuationIndentSize <= 0) {
            return
        }
        if (node is PsiWhiteSpace && !node.isPartOf(PsiComment::class)) {
            val lines = node.getText().split("\n")
            if (lines.size > 1) {
                var offset = node.startOffset + lines.first().length + 1
                val previousIndentSize = node.previousIndentSize()
                val expectedIndentSize = if (continuationIndentSize == indentSize || shouldUseContinuationIndent(node))
                    continuationIndentSize else indentSize
                lines.tail().forEach { line ->
                    if (line.isNotEmpty() && (line.length - previousIndentSize) % expectedIndentSize != 0) {
                        if (!node.isPartOf(KtParameterList::class)) { // parameter list wrapping enforced by ParameterListWrappingRule
                            emit(offset,
                                "Unexpected indentation (${line.length - previousIndentSize}) " +
                                    "(it should be $expectedIndentSize)",
                                false)
                        }
                    }
                    offset += line.length + 1
                }
            }
        }
    }

    private fun shouldUseContinuationIndent(node: PsiWhiteSpace): Boolean {
        val parentNode = node.parent
        val prevNode = node.getPrevSiblingIgnoringWhitespaceAndComments()?.node?.elementType
        val nextNode = node.nextSibling?.node?.elementType
        return (
            prevNode in KtTokens.ALL_ASSIGNMENTS ||
            parentNode is KtSecondaryConstructor ||
            nextNode == KtStubElementTypes.TYPE_REFERENCE ||
            node.nextSibling is KtSuperTypeList ||
            node.nextSibling is KtSuperTypeListEntry ||
            node.nextSibling is KtTypeProjection ||
            parentNode is KtValueArgumentList ||
            parentNode is KtBinaryExpression ||
            parentNode is KtDotQualifiedExpression ||
            parentNode is KtSafeQualifiedExpression ||
            parentNode is KtParenthesizedExpression
        )
    }

    private fun ASTNode.previousIndentSize(): Int {
        val parentNode = this.treeParent?.psi
        var prevSibling = parentNode
        while (prevSibling != null) {
            val nextNode = prevSibling.nextSibling?.node?.elementType
            if (prevSibling is PsiWhiteSpace &&
                nextNode != KtStubElementTypes.TYPE_REFERENCE &&
                nextNode != KtStubElementTypes.SUPER_TYPE_LIST &&
                nextNode != KtNodeTypes.CONSTRUCTOR_DELEGATION_CALL) {
                val prevLines = prevSibling.text.split('\n')
                if (prevLines.size > 1) {
                    return prevLines.last().length
                }
            }
            prevSibling = prevSibling.prevSibling ?: prevSibling.parent
        }
        return 0
    }
}
