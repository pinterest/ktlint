package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.core.IndentConfig
import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.api.DefaultEditorConfigProperties.indentSizeProperty
import com.pinterest.ktlint.core.api.DefaultEditorConfigProperties.indentStyleProperty
import com.pinterest.ktlint.core.api.DefaultEditorConfigProperties.maxLineLengthProperty
import com.pinterest.ktlint.core.api.UsesEditorConfigProperties
import com.pinterest.ktlint.core.ast.ElementType
import com.pinterest.ktlint.core.ast.ElementType.ELSE
import com.pinterest.ktlint.core.ast.children
import com.pinterest.ktlint.core.ast.column
import com.pinterest.ktlint.core.ast.isPartOfComment
import com.pinterest.ktlint.core.ast.isRoot
import com.pinterest.ktlint.core.ast.isWhiteSpace
import com.pinterest.ktlint.core.ast.isWhiteSpaceWithNewline
import com.pinterest.ktlint.core.ast.lineIndent
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
class ArgumentListWrappingRule :
    Rule("argument-list-wrapping"),
    UsesEditorConfigProperties {
    private var editorConfigIndent = IndentConfig.DEFAULT_INDENT_CONFIG
    private var maxLineLength = -1

    override val editorConfigProperties: List<UsesEditorConfigProperties.EditorConfigProperty<*>> =
        listOf(
            indentSizeProperty,
            indentStyleProperty,
            maxLineLengthProperty
        )

    override fun visit(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit
    ) {
        if (node.isRoot()) {
            editorConfigIndent = IndentConfig(
                indentStyle = node.getEditorConfigValue(indentStyleProperty),
                tabWidth = node.getEditorConfigValue(indentSizeProperty)
            )
            maxLineLength = node.getEditorConfigValue(maxLineLengthProperty)
            return
        }
        if (editorConfigIndent.disabled) {
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
                val currentIndentLevel = editorConfigIndent.indentLevelFrom(node.lineIndent())
                val newIndentLevel =
                    when {
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
                        currentIndentLevel > 0 && node.hasTypeArgumentListInFront() -> currentIndentLevel - 1

                        // IDEA quirk:
                        // foo
                        //     .bar = Baz(
                        //     1,
                        //     2
                        // )
                        // instead of
                        // foo
                        //     .bar = Baz(
                        //         1,
                        //         2
                        //     )
                        currentIndentLevel > 0 && node.isPartOfDotQualifiedAssignmentExpression() -> currentIndentLevel - 1

                        else -> currentIndentLevel
                    }.let {
                        if (node.isOnSameLineAsControlFlowKeyword()) {
                            it + 1
                        } else {
                            it
                        }
                    }
                val indent = "\n" + editorConfigIndent.indent.repeat(newIndentLevel)

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

                            // aiming for
                            // ... LPAR
                            // <line indent + indentSize> VALUE_PARAMETER...
                            // <line indent> RPAR
                            val intendedIndent = if (child.elementType == ElementType.VALUE_ARGUMENT) {
                                indent + editorConfigIndent.indent
                            } else {
                                indent
                            }

                            val prevLeaf = child.prevWhiteSpaceWithNewLine() ?: child.prevLeaf()
                            if (prevLeaf is PsiWhiteSpace) {
                                if (prevLeaf.getText().contains("\n")) {
                                    // The current child is already wrapped to a new line. Checking and fixing the
                                    // correct size of the indent is the responsibility of the IndentationRule.
                                    continue@nextChild
                                } else {
                                    // The current child needs to be wrapped to a newline.
                                    emit(child.startOffset, errorMessage(child), true)
                                    if (autoCorrect) {
                                        // The indentation is purely based on the previous leaf only. Note that in
                                        // autoCorrect mode the indent rule, if enabled, runs after this rule and
                                        // determines the final indentation. But if the indent rule is disabled then the
                                        // indent of this rule is kept.
                                        argumentInnerIndentAdjustment = intendedIndent.length - prevLeaf.getTextLength()
                                        (prevLeaf as LeafPsiElement).rawReplaceWithText(intendedIndent)
                                    }
                                }
                            } else {
                                // Insert a new whitespace element in order to wrap the current child to a new line.
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
                                            val expectedPosition = intendedIndent.length + editorConfigIndent.indent.length
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

    private fun ASTNode.hasTypeArgumentListInFront(): Boolean =
        treeParent.children()
            .firstOrNull { it.elementType == ElementType.TYPE_ARGUMENT_LIST }
            ?.children()
            ?.any { it.isWhiteSpaceWithNewline() } == true

    private fun ASTNode.isPartOfDotQualifiedAssignmentExpression(): Boolean =
        treeParent?.treeParent?.elementType == ElementType.BINARY_EXPRESSION &&
            treeParent?.treeParent?.children()?.find { it.elementType == ElementType.DOT_QUALIFIED_EXPRESSION } != null

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
        val controlFlowKeyword = when (val parent = containerNode.parent) {
            is KtIfExpression -> parent.ifKeyword.node
            is KtWhileExpression -> parent.firstChild.node
            is KtDoWhileExpression -> parent.whileKeyword?.node
            else -> null
        } ?: return false

        var prevLeaf = prevLeaf() ?: return false
        while (prevLeaf != controlFlowKeyword) {
            if (prevLeaf.isWhiteSpaceWithNewline()) return false
            prevLeaf = prevLeaf.prevLeaf() ?: return false
        }
        return true
    }
}
