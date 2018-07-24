package com.github.shyiko.ktlint.ruleset.standard

import com.github.shyiko.ktlint.core.Rule
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.lang.FileASTNode
import org.jetbrains.kotlin.com.intellij.psi.PsiComment
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.psi.KtParameterList
import org.jetbrains.kotlin.psi.KtTypeConstraintList
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes

class IndentationRule : Rule("indent") {

    private var indentSize = -1

    override fun visit(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit
    ) {
        if (node.elementType == KtStubElementTypes.FILE) {
            val ec = EditorConfig.from(node as FileASTNode)
            indentSize = gcd(maxOf(ec.indentSize, 1), maxOf(ec.continuationIndentSize, 1))
            return
        }
        if (indentSize <= 1) {
            return
        }
        if (node is PsiWhiteSpace && !node.isPartOf(PsiComment::class)) {
            val lines = node.getText().split("\n")
            if (lines.size > 1 && !node.isPartOf(KtTypeConstraintList::class)) {
                var offset = node.startOffset + lines.first().length + 1
                val previousIndentSize = node.previousIndentSize()
                lines.tail().forEach { indent ->
                    if (indent.isNotEmpty() && (indent.length - previousIndentSize) % indentSize != 0) {
                        if (!node.isPartOf(KtParameterList::class)) { // parameter list wrapping enforced by ParameterListWrappingRule
                            emit(
                                offset,
                                "Unexpected indentation (${indent.length}) (it should be ${previousIndentSize + indentSize})",
                                false
                            )
                        }
                    }
                    offset += indent.length + 1
                }
            }
            if (node.textContains('\t')) {
                val text = node.getText()
                emit(node.startOffset + text.indexOf('\t'), "Unexpected Tab character(s)", true)
                if (autoCorrect) {
                    (node as LeafPsiElement).rawReplaceWithText(text.replace("\t", " ".repeat(indentSize)))
                }
            }
        }
    }

    private fun gcd(a: Int, b: Int): Int = when {
        a > b -> gcd(a - b, b)
        a < b -> gcd(a, b - a)
        else -> a
    }

    // todo: calculating indent based on the previous line value is wrong (see IndentationRule.testLint)
    private fun ASTNode.previousIndentSize(): Int {
        var node = this.treeParent?.psi
        while (node != null) {
            val nextNode = node.nextSibling?.node?.elementType
            if (node is PsiWhiteSpace &&
                nextNode != KtStubElementTypes.TYPE_REFERENCE &&
                nextNode != KtStubElementTypes.SUPER_TYPE_LIST &&
                nextNode != KtNodeTypes.CONSTRUCTOR_DELEGATION_CALL &&
                node.textContains('\n') &&
                node.nextLeaf()?.isPartOf(PsiComment::class) != true) {
                return node.text.length - node.text.lastIndexOf('\n') - 1
            }
            node = node.prevSibling ?: node.parent
        }
        return 0
    }
}
