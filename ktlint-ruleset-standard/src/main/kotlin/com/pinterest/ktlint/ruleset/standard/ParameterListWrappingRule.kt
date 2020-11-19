package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.core.KtLint
import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.ast.ElementType.FUNCTION_LITERAL
import com.pinterest.ktlint.core.ast.ElementType.LPAR
import com.pinterest.ktlint.core.ast.ElementType.PRIMARY_CONSTRUCTOR
import com.pinterest.ktlint.core.ast.ElementType.RPAR
import com.pinterest.ktlint.core.ast.ElementType.TYPE_PARAMETER_LIST
import com.pinterest.ktlint.core.ast.ElementType.VALUE_PARAMETER
import com.pinterest.ktlint.core.ast.ElementType.VALUE_PARAMETER_LIST
import com.pinterest.ktlint.core.ast.ElementType.WHITE_SPACE
import com.pinterest.ktlint.core.ast.children
import com.pinterest.ktlint.core.ast.column
import com.pinterest.ktlint.core.ast.isRoot
import com.pinterest.ktlint.core.ast.isWhiteSpaceWithNewline
import com.pinterest.ktlint.core.ast.lineIndent
import com.pinterest.ktlint.core.ast.prevLeaf
import com.pinterest.ktlint.core.ast.prevSibling
import com.pinterest.ktlint.core.ast.visit
import kotlin.math.max
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafElement
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.PsiWhiteSpaceImpl
import org.jetbrains.kotlin.psi.KtTypeArgumentList
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType

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
                // IDEA quirk:
                // fun <
                //     T,
                //     R> test(
                //     param1: T
                //     param2: R
                // )
                // instead of
                // fun <
                //     T,
                //     R> test(
                //         param1: T
                //         param2: R
                //     )
                val adjustedIndent = if (node.hasTypeParameterListInFront()) indentSize else 0

                // aiming for
                // ... LPAR
                // <line indent + indentSize> VALUE_PARAMETER...
                // <line indent> RPAR
                val lineIndent = node.lineIndent()
                val indent = "\n" + lineIndent.substring(0, (lineIndent.length - adjustedIndent).coerceAtLeast(0))
                val paramIndent = indent + " ".repeat(indentSize)
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
                            if (paramInnerIndentAdjustment != 0 && child.elementType == VALUE_PARAMETER) {
                                child.visit { n ->
                                    if (n.elementType == WHITE_SPACE && n.textContains('\n')) {
                                        val split = n.text.split("\n")
                                        (n as LeafElement).rawReplaceWithText(
                                            split.joinToString("\n") {
                                                if (paramInnerIndentAdjustment > 0) {
                                                    it + " ".repeat(paramInnerIndentAdjustment)
                                                } else {
                                                    it.substring(0, max(it.length + paramInnerIndentAdjustment, 0))
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

    private fun errorMessage(node: ASTNode) =
        when (node.elementType) {
            LPAR -> """Unnecessary newline before "(""""
            VALUE_PARAMETER ->
                "Parameter should be on a separate line (unless all parameters can fit a single line)"
            RPAR -> """Missing newline before ")""""
            else -> throw UnsupportedOperationException()
        }

    private fun ASTNode.hasTypeParameterListInFront(): Boolean {
        val parent = this.treeParent
        val typeParameterList = if (parent.elementType == PRIMARY_CONSTRUCTOR) {
            parent.prevSibling { it.elementType == TYPE_PARAMETER_LIST }
        } else {
            parent.children().firstOrNull { it.elementType == TYPE_PARAMETER_LIST }
        }
        val typeListNode = typeParameterList
            ?: parent.psi.collectDescendantsOfType<KtTypeArgumentList>().firstOrNull()?.node
            ?: return false
        return typeListNode.children().any { it.isWhiteSpaceWithNewline() }
    }
}
