package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.core.KtLint
import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.ast.ElementType.FUNCTION_LITERAL
import com.pinterest.ktlint.core.ast.ElementType.LPAR
import com.pinterest.ktlint.core.ast.ElementType.RPAR
import com.pinterest.ktlint.core.ast.ElementType.VALUE_PARAMETER
import com.pinterest.ktlint.core.ast.ElementType.VALUE_PARAMETER_LIST
import com.pinterest.ktlint.core.ast.ElementType.WHITE_SPACE
import com.pinterest.ktlint.core.ast.children
import com.pinterest.ktlint.core.ast.isRoot
import com.pinterest.ktlint.core.ast.prevLeaf
import com.pinterest.ktlint.core.ast.visit
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafElement
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.PsiWhiteSpaceImpl

class ParameterListWrappingRule : Rule("parameter-list-wrapping") {

    private var indentSize = -1
    private var maxLineLength = -1

    override fun visit(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit
    ) {
        if (node.isRoot()) {
            val editorConfig = node.getUserData(KtLint.EDITOR_CONFIG_USER_DATA_KEY)!!
            indentSize = editorConfig.indentSize
            maxLineLength = editorConfig.maxLineLength
            return
        }
        if (indentSize <= 0) {
            return
        }
        if (node.elementType == VALUE_PARAMETER_LIST &&
            // skip when there are no parameters
            node.firstChildNode?.treeNext?.elementType != RPAR &&
            // skip lambda parameters
            node.treeParent?.elementType != FUNCTION_LITERAL
        ) {
            // each parameter should be on a separate line if
            // - at least one of the parameters is
            // - maxLineLength exceeded (and separating parameters with \n would actually help)
            // in addition, "(" and ")" must be on separates line if any of the parameters are (otherwise on the same)
            val putParametersOnSeparateLines =
                node.textContains('\n') ||
                    // max_line_length exceeded
                    maxLineLength > -1 && (node.column - 1 + node.textLength) > maxLineLength
            if (putParametersOnSeparateLines) {
                // aiming for
                // ... LPAR
                // <line indent + indentSize> VALUE_PARAMETER...
                // <line indent> RPAR
                val indent = "\n" + node.lineIndent()
                val paramIndent = indent + " ".repeat(indentSize) // single indent as recommended by Jetbrains/Google
                nextChild@ for (child in node.children()) {
                    when (child.elementType) {
                        LPAR -> {
                            val prevLeaf = child.prevLeaf()
                            if (prevLeaf is PsiWhiteSpace && prevLeaf.textContains('\n')) {
                                emit(child.startOffset, errorMessage(child), true)
                                if (autoCorrect) {
                                    prevLeaf.delete()
                                }
                            }
                        }
                        VALUE_PARAMETER,
                        RPAR -> {
                            var paramInnerIndentAdjustment = 0
                            val prevLeaf = child.prevLeaf()
                            val intendedIndent = if (child.elementType == VALUE_PARAMETER) {
                                paramIndent
                            } else {
                                indent
                            }
                            if (prevLeaf is PsiWhiteSpace) {
                                val spacing = prevLeaf.getText()
                                val cut = spacing.lastIndexOf("\n")
                                if (cut > -1) {
                                    val childIndent = spacing.substring(cut)
                                    if (childIndent == intendedIndent) {
                                        continue@nextChild
                                    }
                                    emit(
                                        child.startOffset,
                                        "Unexpected indentation" +
                                            " (expected ${intendedIndent.length - 1}, actual ${childIndent.length - 1})",
                                        true
                                    )
                                } else {
                                    emit(child.startOffset, errorMessage(child), true)
                                }
                                if (autoCorrect) {
                                    val adjustedIndent = (if (cut > -1) spacing.substring(0, cut) else "") + intendedIndent
                                    paramInnerIndentAdjustment = adjustedIndent.length - prevLeaf.getTextLength()
                                    (prevLeaf as LeafPsiElement).rawReplaceWithText(adjustedIndent)
                                }
                            } else {
                                emit(child.startOffset, errorMessage(child), true)
                                if (autoCorrect) {
                                    paramInnerIndentAdjustment = intendedIndent.length - child.column
                                    node.addChild(PsiWhiteSpaceImpl(intendedIndent), child)
                                }
                            }
                            if (paramInnerIndentAdjustment != 0 &&
                                child.elementType == VALUE_PARAMETER
                            ) {
                                child.visit { n ->
                                    if (n.elementType == WHITE_SPACE && n.textContains('\n')) {
                                        val split = n.text.split("\n")
                                        (n as LeafElement).rawReplaceWithText(
                                            split.joinToString("\n") {
                                                if (paramInnerIndentAdjustment > 0) {
                                                    it + " ".repeat(paramInnerIndentAdjustment)
                                                } else {
                                                    it.substring(0, Math.max(it.length + paramInnerIndentAdjustment, 0))
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private val ASTNode.column: Int
        get() {
            var leaf = this.prevLeaf()
            var offsetToTheLeft = 0
            while (leaf != null) {
                if (leaf.elementType == WHITE_SPACE && leaf.textContains('\n')) {
                    offsetToTheLeft += leaf.textLength - 1 - leaf.text.lastIndexOf('\n')
                    break
                }
                offsetToTheLeft += leaf.textLength
                leaf = leaf.prevLeaf()
            }
            return offsetToTheLeft + 1
        }

    private fun errorMessage(node: ASTNode) =
        when (node.elementType) {
            LPAR -> """Unnecessary newline before "(""""
            VALUE_PARAMETER ->
                "Parameter should be on a separate line (unless all parameters can fit a single line)"
            RPAR -> """Missing newline before ")""""
            else -> throw UnsupportedOperationException()
        }

    private fun ASTNode.lineIndent(): String {
        var leaf = this.prevLeaf()
        while (leaf != null) {
            if (leaf.elementType == WHITE_SPACE && leaf.textContains('\n')) {
                return leaf.text.substring(leaf.text.lastIndexOf('\n') + 1)
            }
            leaf = leaf.prevLeaf()
        }
        return ""
    }
}
