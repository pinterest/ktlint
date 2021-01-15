package com.pinterest.ktlint.ruleset.experimental

import com.pinterest.ktlint.core.KtLint
import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.ast.ElementType
import com.pinterest.ktlint.core.ast.ElementType.DOT_QUALIFIED_EXPRESSION
import com.pinterest.ktlint.core.ast.ElementType.ELSE
import com.pinterest.ktlint.core.ast.ElementType.TYPE_ARGUMENT_LIST
import com.pinterest.ktlint.core.ast.children
import com.pinterest.ktlint.core.ast.column
import com.pinterest.ktlint.core.ast.isPartOf
import com.pinterest.ktlint.core.ast.isPartOfComment
import com.pinterest.ktlint.core.ast.isRoot
import com.pinterest.ktlint.core.ast.isWhiteSpace
import com.pinterest.ktlint.core.ast.isWhiteSpaceWithNewline
import com.pinterest.ktlint.core.ast.lineIndent
import com.pinterest.ktlint.core.ast.lineNumber
import com.pinterest.ktlint.core.ast.prevLeaf
import com.pinterest.ktlint.core.ast.visit
import kotlin.math.max
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafElement
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.PsiWhiteSpaceImpl
import org.jetbrains.kotlin.psi.KtContainerNode
import org.jetbrains.kotlin.psi.KtDoWhileExpression
import org.jetbrains.kotlin.psi.KtIfExpression
import org.jetbrains.kotlin.psi.KtWhileExpression
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType

/**
 * https://kotlinlang.org/docs/reference/coding-conventions.html#method-call-formatting
 *
 * The rule is more aggressive in inserting newlines after arguments than mentioned in the styleguide:
 * Each argument should be on a separate line if
 * - at least one of the arguments is
 * - maxLineLength exceeded (and separating arguments with \n would actually help)
 * in addition, "(" and ")" must be on separates line if any of the arguments are (otherwise on the same)
 */
class ArgumentListWrappingRule : Rule("argument-list-wrapping") {

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

        if (node.elementType == ElementType.VALUE_ARGUMENT_LIST &&
            // skip when there are no arguments
            node.firstChildNode?.treeNext?.elementType != ElementType.RPAR &&
            // skip lambda arguments
            node.treeParent?.elementType != ElementType.FUNCTION_LITERAL &&
            // skip if number of arguments is big (we assume it with a magic number of 8)
            node.children().count { it.elementType == ElementType.VALUE_ARGUMENT } <= 8
        ) {
            // each argument should be on a separate line if
            // - at least one of the arguments is
            // - maxLineLength exceeded (and separating arguments with \n would actually help)
            // in addition, "(" and ")" must be on separates line if any of the arguments are (otherwise on the same)
            val putArgumentsOnSeparateLines =
                node.textContainsIgnoringLambda('\n') ||
                    // max_line_length exceeded
                    maxLineLength > -1 && (node.column - 1 + node.textLength) > maxLineLength && !node.textContains('\n')
            if (putArgumentsOnSeparateLines) {
                val prevWhitespaceWithNewline = node.prevLeaf { it.isWhiteSpaceWithNewline() }
                val adjustedIndent = when {
                    // IDEA quirk:
                    // generic<
                    //     T,
                    //     R>(
                    //     1,
                    //     2
                    // )
                    // instead of
                    // generic<
                    //     T,
                    //     R>(
                    //         1,
                    //         2
                    //     )
                    prevWhitespaceWithNewline?.isPartOf(TYPE_ARGUMENT_LIST) == true -> indentSize
                    // IDEA quirk:
                    // foo
                    //     .bar(
                    //     1,
                    //     2
                    // )
                    // instead of
                    // foo
                    //     .bar(
                    //         1,
                    //         2
                    //     )
                    prevWhitespaceWithNewline?.isPartOf(DOT_QUALIFIED_EXPRESSION) == true -> indentSize
                    else -> 0
                }

                // aiming for
                // ... LPAR
                // <line indent + indentSize> VALUE_ARGUMENT...
                // <line indent> RPAR
                val lineIndent = node.lineIndent()
                val indent = ("\n" + lineIndent.substring(0, (lineIndent.length - adjustedIndent).coerceAtLeast(0)))
                    .let { if (node.isOnSameLineAsControlFlowKeyword()) it + " ".repeat(indentSize) else it }
                val paramIndent = indent + " ".repeat(indentSize)
                nextChild@ for (child in node.children()) {
                    when (child.elementType) {
                        ElementType.LPAR -> {
                            val prevLeaf = child.prevLeaf()
                            if (prevLeaf is PsiWhiteSpace && prevLeaf.textContains('\n')) {
                                emit(child.startOffset, errorMessage(child), true)
                                if (autoCorrect) {
                                    prevLeaf.delete()
                                }
                            }
                        }
                        ElementType.VALUE_ARGUMENT,
                        ElementType.RPAR -> {
                            var argumentInnerIndentAdjustment = 0
                            val prevLeaf = child.prevWhiteSpaceWithNewLine() ?: child.prevLeaf()
                            val intendedIndent = if (child.elementType == ElementType.VALUE_ARGUMENT) {
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
                                    val adjustedIndent =
                                        (if (cut > -1) spacing.substring(0, cut) else "") + intendedIndent
                                    argumentInnerIndentAdjustment = adjustedIndent.length - prevLeaf.getTextLength()
                                    (prevLeaf as LeafPsiElement).rawReplaceWithText(adjustedIndent)
                                }
                            } else {
                                emit(child.startOffset, errorMessage(child), true)
                                if (autoCorrect) {
                                    argumentInnerIndentAdjustment = intendedIndent.length - child.column
                                    node.addChild(PsiWhiteSpaceImpl(intendedIndent), child)
                                }
                            }
                            if (argumentInnerIndentAdjustment != 0 && child.elementType == ElementType.VALUE_ARGUMENT) {
                                child.visit { n ->
                                    if (n.elementType == ElementType.WHITE_SPACE && n.textContains('\n')) {
                                        val isInCollectionOrFunctionLiteral =
                                            n.treeParent?.elementType == ElementType.COLLECTION_LITERAL_EXPRESSION || n.treeParent?.elementType == ElementType.FUNCTION_LITERAL

                                        // If we're inside a collection literal, let's recalculate the adjustment
                                        // because the items inside the collection should not be subject to the same
                                        // indentation as the brackets.
                                        val adjustment = if (isInCollectionOrFunctionLiteral) {
                                            val expectedPosition = intendedIndent.length + indentSize
                                            expectedPosition - child.column
                                        } else {
                                            argumentInnerIndentAdjustment
                                        }

                                        val split = n.text.split("\n")
                                        (n as LeafElement).rawReplaceWithText(
                                            split.joinToString("\n") {
                                                when {
                                                    it.isEmpty() -> it
                                                    adjustment > 0 -> it + " ".repeat(adjustment)
                                                    else -> it.substring(0, max(it.length + adjustment, 0))
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
            ElementType.LPAR -> """Unnecessary newline before "(""""
            ElementType.VALUE_ARGUMENT ->
                "Argument should be on a separate line (unless all arguments can fit a single line)"
            ElementType.RPAR -> """Missing newline before ")""""
            else -> throw UnsupportedOperationException()
        }

    private fun ASTNode.textContainsIgnoringLambda(char: Char): Boolean {
        return children().any { child ->
            val elementType = child.elementType
            elementType == ElementType.WHITE_SPACE && child.textContains(char) ||
                elementType == ElementType.COLLECTION_LITERAL_EXPRESSION && child.textContains(char) ||
                elementType == ElementType.VALUE_ARGUMENT && child.children().any { it.textContainsIgnoringLambda(char) }
        }
    }

    private fun ASTNode.prevWhiteSpaceWithNewLine(): ASTNode? {
        var prev = prevLeaf()
        while (prev != null && (prev.isWhiteSpace() || prev.isPartOfComment())) {
            if (prev.isWhiteSpaceWithNewline()) {
                return prev
            }
            prev = prev.prevLeaf()
        }
        return null
    }

    private fun ASTNode.isOnSameLineAsControlFlowKeyword(): Boolean {
        val containerNode = psi.getStrictParentOfType<KtContainerNode>() ?: return false
        if (containerNode.node.elementType == ELSE) return false
        return when (val parent = containerNode.parent) {
            is KtIfExpression, is KtWhileExpression -> parent.node
            is KtDoWhileExpression -> parent.whileKeyword?.node
            else -> null
        }?.lineNumber() == lineNumber()
    }
}
