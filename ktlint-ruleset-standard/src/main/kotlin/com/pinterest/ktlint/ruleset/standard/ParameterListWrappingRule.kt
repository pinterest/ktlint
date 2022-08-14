package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.core.IndentConfig
import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.api.DefaultEditorConfigProperties.indentSizeProperty
import com.pinterest.ktlint.core.api.DefaultEditorConfigProperties.indentStyleProperty
import com.pinterest.ktlint.core.api.DefaultEditorConfigProperties.maxLineLengthProperty
import com.pinterest.ktlint.core.api.EditorConfigProperties
import com.pinterest.ktlint.core.api.UsesEditorConfigProperties
import com.pinterest.ktlint.core.ast.ElementType.FUNCTION_LITERAL
import com.pinterest.ktlint.core.ast.ElementType.FUNCTION_TYPE
import com.pinterest.ktlint.core.ast.ElementType.LPAR
import com.pinterest.ktlint.core.ast.ElementType.NULLABLE_TYPE
import com.pinterest.ktlint.core.ast.ElementType.PRIMARY_CONSTRUCTOR
import com.pinterest.ktlint.core.ast.ElementType.RPAR
import com.pinterest.ktlint.core.ast.ElementType.TYPE_PARAMETER_LIST
import com.pinterest.ktlint.core.ast.ElementType.VALUE_PARAMETER
import com.pinterest.ktlint.core.ast.ElementType.VALUE_PARAMETER_LIST
import com.pinterest.ktlint.core.ast.column
import com.pinterest.ktlint.core.ast.isWhiteSpaceWithNewline
import com.pinterest.ktlint.core.ast.lineIndent
import com.pinterest.ktlint.core.ast.prevLeaf
import com.pinterest.ktlint.core.ast.prevSibling
import com.pinterest.ktlint.core.ast.upsertWhitespaceAfterMe
import com.pinterest.ktlint.core.ast.upsertWhitespaceBeforeMe
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafElement
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.PsiWhiteSpaceImpl
import org.jetbrains.kotlin.psi.KtTypeArgumentList
import org.jetbrains.kotlin.psi.psiUtil.children
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType

public class ParameterListWrappingRule :
    Rule("parameter-list-wrapping"),
    UsesEditorConfigProperties {
    override val editorConfigProperties: List<UsesEditorConfigProperties.EditorConfigProperty<*>> =
        listOf(
            indentSizeProperty,
            indentStyleProperty,
            maxLineLengthProperty,
        )

    private var indentConfig = IndentConfig.DEFAULT_INDENT_CONFIG
    private var maxLineLength = -1

    override fun beforeFirstNode(editorConfigProperties: EditorConfigProperties) {
        maxLineLength = editorConfigProperties.getEditorConfigValue(maxLineLengthProperty)
        indentConfig = IndentConfig(
            indentStyle = editorConfigProperties.getEditorConfigValue(indentStyleProperty),
            tabWidth = editorConfigProperties.getEditorConfigValue(indentSizeProperty),
        )
        if (indentConfig.disabled) {
            stopTraversalOfAST()
        }
    }

    override fun beforeVisitChildNodes(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        when (node.elementType) {
            NULLABLE_TYPE -> wrapNullableType(node, emit, autoCorrect)
            VALUE_PARAMETER_LIST -> {
                if (node.needToWrapParameterList()) {
                    wrapParameterList(node, emit, autoCorrect)
                }
            }
        }
    }

    private fun wrapNullableType(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
        autoCorrect: Boolean,
    ) {
        require(node.elementType == NULLABLE_TYPE)
        node
            .takeUnless {
                // skip when max line length not set or does not exceed max line length
                maxLineLength <= 0 || (node.column - 1 + node.textLength) <= maxLineLength
            }?.findChildByType(FUNCTION_TYPE)
            ?.findChildByType(VALUE_PARAMETER_LIST)
            ?.takeIf { it.findChildByType(VALUE_PARAMETER) != null }
            ?.takeUnless { it.textContains('\n') }
            ?.let {
                node
                    .children()
                    .forEach {
                        when (it.elementType) {
                            LPAR -> {
                                emit(
                                    it.startOffset,
                                    "Parameter of nullable type should be on a separate line (unless the type fits on a single line)",
                                    true,
                                )
                                if (autoCorrect) {
                                    (it as LeafElement).upsertWhitespaceAfterMe("\n${indentConfig.indent}")
                                }
                            }
                            RPAR -> {
                                emit(it.startOffset, errorMessage(it), true)
                                if (autoCorrect) {
                                    (it as LeafElement).upsertWhitespaceBeforeMe("\n")
                                }
                            }
                        }
                    }
            }
    }

    private fun ASTNode.needToWrapParameterList() =
        if ( // skip when there are no parameters
            firstChildNode?.treeNext?.elementType != RPAR &&
            // skip lambda parameters
            treeParent?.elementType != FUNCTION_LITERAL &&
            // skip when function type is wrapped in a nullable type [which was already when processing the nullable
            // type node itself.
            !(treeParent.elementType == FUNCTION_TYPE && treeParent?.treeParent?.elementType == NULLABLE_TYPE)
        ) {
            // each parameter should be on a separate line if
            // - at least one of the parameters is
            // - maxLineLength exceeded (and separating parameters with \n would actually help)
            // in addition, "(" and ")" must be on separates line if any of the parameters are (otherwise on the same)
            textContains('\n') || this.exceedsMaxLineLength()
        } else {
            false
        }

    private fun wrapParameterList(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
        autoCorrect: Boolean,
    ) {
        val newIndentLevel = getNewIndentLevel(node)
        node
            .children()
            .forEach { child -> wrapParemeterInList(newIndentLevel, child, emit, autoCorrect) }
    }

    private fun getNewIndentLevel(node: ASTNode): Int {
        val currentIndentLevel = indentConfig.indentLevelFrom(node.lineIndent())
        return when {
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
            currentIndentLevel > 0 && node.hasTypeParameterListInFront() -> currentIndentLevel - 1

            else -> currentIndentLevel
        }
    }

    private fun wrapParemeterInList(
        newIndentLevel: Int,
        child: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
        autoCorrect: Boolean,
    ) {
        val indent = "\n" + indentConfig.indent.repeat(newIndentLevel)
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
            RPAR,
            -> {
                // aiming for
                // ... LPAR
                // <line indent + indentSize> VALUE_PARAMETER...
                // <line indent> RPAR
                val intendedIndent = if (child.elementType == VALUE_PARAMETER) {
                    indent + indentConfig.indent
                } else {
                    indent
                }

                val prevLeaf = child.prevLeaf()
                if (prevLeaf is PsiWhiteSpace) {
                    if (prevLeaf.getText().contains("\n")) {
                        // The current child is already wrapped to a new line. Checking and fixing the
                        // correct size of the indent is the responsibility of the IndentationRule.
                        return
                    } else {
                        // The current child needs to be wrapped to a newline.
                        emit(child.startOffset, errorMessage(child), true)
                        if (autoCorrect) {
                            // The indentation is purely based on the previous leaf only. Note that in
                            // autoCorrect mode the indent rule, if enabled, runs after this rule and
                            // determines the final indentation. But if the indent rule is disabled then the
                            // indent of this rule is kept.
                            (prevLeaf as LeafPsiElement).rawReplaceWithText(intendedIndent)
                        }
                    }
                } else {
                    // Insert a new whitespace element in order to wrap the current child to a new line.
                    emit(child.startOffset, errorMessage(child), true)
                    if (autoCorrect) {
                        child.treeParent.addChild(PsiWhiteSpaceImpl(intendedIndent), child)
                    }
                }
                // Indentation of child nodes need to be fixed by the IndentationRule.
            }
        }
    }

    private fun ASTNode.exceedsMaxLineLength() =
        maxLineLength > -1 && (column - 1 + textLength) > maxLineLength && !textContains('\n')

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
