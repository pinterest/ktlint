package com.github.shyiko.ktlint.ruleset.standard

import com.github.shyiko.ktlint.core.Rule
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.lang.FileASTNode
import org.jetbrains.kotlin.com.intellij.psi.PsiComment
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtParameterList
import org.jetbrains.kotlin.psi.KtTypeConstraintList
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes

class IndentationRule : Rule("indent") {

    private var config: EditorConfig = EditorConfig(-1, -1, -1, false)

    override fun visit(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit
    ) {
        if (node.elementType == KtStubElementTypes.FILE) {
            config = EditorConfig.from(node as FileASTNode)
            return
        }
        if (config.indentSize <= 1) {
            return
        }
        if (node is PsiWhiteSpace) {
            val lines = node.getText().split("\n")
            if (lines.size > 1 && formatIsEnabledForNode(node)) {
                var offset = node.startOffset + lines.first().length + 1
                val previousIndentSize = node.previousIndentSize()
                val indentSize = expectedIndentForNode(node, config)
                lines.tail()
                    .filter { it.isNotEmpty() }
                    .forEach { indent ->
                        if (
                            (indentSize == 0 && (indent.length - previousIndentSize) != 0) ||
                            (indentSize != 0 && (indent.length - previousIndentSize) % indentSize != 0)
                        ) {
                            emit(
                                offset,
                                "Unexpected indentation (${indent.length}) (it should be ${previousIndentSize + indentSize})",
                                true
                            )
                            if (autoCorrect) {
                                replaceWithExpectedIndent(node, previousIndentSize + indentSize)
                            }
                        }
                        offset += indent.length + 1
                    }
            }
        }
    }

    private fun gcd(a: Int, b: Int): Int = when {
        a > b -> gcd(a - b, b)
        a < b -> gcd(a, b - a)
        else -> a
    }

    private fun replaceWithExpectedIndent(node: ASTNode, expectedIndentSize: Int) {
        val correctedIndent = "\n" + " ".repeat(expectedIndentSize)
        (node as LeafPsiElement).rawReplaceWithText(correctedIndent)
    }

    // todo: calculating indent based on the previous line value is wrong (see IndentationRule.testLint)
    private fun ASTNode.previousIndentSize(): Int {
        var node = this.getParentNodeForIndent().psi
        while (node != null) {
            val nextNode = node.nextSibling?.node?.elementType
            if (node is PsiWhiteSpace &&
                nextNode != KtStubElementTypes.TYPE_REFERENCE &&
                nextNode != KtStubElementTypes.SUPER_TYPE_LIST &&
                nextNode != KtNodeTypes.CONSTRUCTOR_DELEGATION_CALL &&
                node.textContains('\n')
            ) {
                return node.text.length - node.text.lastIndexOf('\n') - 1
            }
            node = node.prevSibling ?: node.parent
        }
        return 0
    }

    private fun ASTNode.getParentNodeForIndent(): ASTNode {
        val parentNode: ASTNode? = this.treeParent
        return when {
            parentNode == null -> this
            parentNode.elementType == KtNodeTypes.CLASS_BODY ||
                parentNode.elementType == KtStubElementTypes.FUNCTION -> parentNode.getParentNodeForIndent()
            else -> parentNode
        }
    }

    private fun formatIsEnabledForNode(node: PsiWhiteSpace): Boolean {
        return !node.isPartOf(PsiComment::class) &&
            // formatting inside "where" clause is not implemented yet
            // https://github.com/shyiko/ktlint/issues/180
            !node.isPartOf(KtTypeConstraintList::class) &&
            // parameter list wrapping enforced by ParameterListWrappingRule
            !node.isPartOf(KtParameterList::class)
    }

    private fun expectedIndentForNode(node: PsiWhiteSpace, config: EditorConfig): Int {
        val nextNode = node.nextSibling?.node?.elementType
        return if (nextNode == KtTokens.RBRACE) {
            0
        } else {
            gcd(maxOf(config.indentSize, 1), maxOf(config.continuationIndentSize, 1))
        }
    }
}
