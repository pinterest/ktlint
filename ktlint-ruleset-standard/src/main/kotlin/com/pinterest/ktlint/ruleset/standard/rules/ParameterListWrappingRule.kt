package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.rule.engine.core.api.ElementType
import com.pinterest.ktlint.rule.engine.core.api.ElementType.FUNCTION_LITERAL
import com.pinterest.ktlint.rule.engine.core.api.ElementType.FUNCTION_TYPE
import com.pinterest.ktlint.rule.engine.core.api.ElementType.LPAR
import com.pinterest.ktlint.rule.engine.core.api.ElementType.NULLABLE_TYPE
import com.pinterest.ktlint.rule.engine.core.api.ElementType.PRIMARY_CONSTRUCTOR
import com.pinterest.ktlint.rule.engine.core.api.ElementType.RPAR
import com.pinterest.ktlint.rule.engine.core.api.ElementType.TYPE_PARAMETER_LIST
import com.pinterest.ktlint.rule.engine.core.api.ElementType.VALUE_PARAMETER
import com.pinterest.ktlint.rule.engine.core.api.ElementType.VALUE_PARAMETER_LIST
import com.pinterest.ktlint.rule.engine.core.api.IndentConfig
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.column
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.CODE_STYLE_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.CodeStyleValue.ktlint_official
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.EditorConfig
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.INDENT_SIZE_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.INDENT_STYLE_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.MAX_LINE_LENGTH_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.firstChildLeafOrSelf
import com.pinterest.ktlint.rule.engine.core.api.indent
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpaceWithNewline
import com.pinterest.ktlint.rule.engine.core.api.parent
import com.pinterest.ktlint.rule.engine.core.api.prevCodeLeaf
import com.pinterest.ktlint.rule.engine.core.api.prevLeaf
import com.pinterest.ktlint.rule.engine.core.api.prevSibling
import com.pinterest.ktlint.rule.engine.core.api.upsertWhitespaceAfterMe
import com.pinterest.ktlint.rule.engine.core.api.upsertWhitespaceBeforeMe
import com.pinterest.ktlint.ruleset.standard.StandardRule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.PsiWhiteSpaceImpl
import org.jetbrains.kotlin.psi.KtTypeArgumentList
import org.jetbrains.kotlin.psi.psiUtil.children
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.jetbrains.kotlin.psi.psiUtil.leaves

public class ParameterListWrappingRule :
    StandardRule(
        id = "parameter-list-wrapping",
        usesEditorConfigProperties =
            setOf(
                CODE_STYLE_PROPERTY,
                INDENT_SIZE_PROPERTY,
                INDENT_STYLE_PROPERTY,
                MAX_LINE_LENGTH_PROPERTY,
            ),
    ) {
    private var codeStyle = CODE_STYLE_PROPERTY.defaultValue
    private var indentConfig = IndentConfig.DEFAULT_INDENT_CONFIG
    private var maxLineLength = MAX_LINE_LENGTH_PROPERTY.defaultValue

    override fun beforeFirstNode(editorConfig: EditorConfig) {
        codeStyle = editorConfig[CODE_STYLE_PROPERTY]
        maxLineLength = editorConfig[MAX_LINE_LENGTH_PROPERTY]
        indentConfig =
            IndentConfig(
                indentStyle = editorConfig[INDENT_STYLE_PROPERTY],
                tabWidth = editorConfig[INDENT_SIZE_PROPERTY],
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
                // skip when max line length is not exceedd
                (node.column - 1 + node.textLength) <= maxLineLength
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
                                    it.upsertWhitespaceAfterMe("\n${indentConfig.indent}")
                                }
                            }
                            RPAR -> {
                                emit(it.startOffset, errorMessage(it), true)
                                if (autoCorrect) {
                                    it.upsertWhitespaceBeforeMe("\n")
                                }
                            }
                        }
                    }
            }
    }

    private fun ASTNode.needToWrapParameterList() =
        when {
            hasNoParameters() -> false
            codeStyle != ktlint_official && isPartOfFunctionLiteralInNonKtlintOfficialCodeStyle() -> false
            codeStyle == ktlint_official && isPartOfFunctionLiteralStartingOnSameLineAsClosingParenthesisOfPrecedingReferenceExpression() ->
                false
            isFunctionTypeWrappedInNullableType() -> false
            textContains('\n') -> true
            codeStyle == ktlint_official && containsAnnotatedParameter() -> true
            exceedsMaxLineLength() -> true
            else -> false
        }

    private fun ASTNode.hasNoParameters(): Boolean {
        require(elementType == VALUE_PARAMETER_LIST)
        return firstChildNode?.treeNext?.elementType == RPAR
    }

    private fun ASTNode.isPartOfFunctionLiteralInNonKtlintOfficialCodeStyle(): Boolean {
        require(elementType == VALUE_PARAMETER_LIST)
        return treeParent?.elementType == FUNCTION_LITERAL
    }

    private fun ASTNode.isPartOfFunctionLiteralStartingOnSameLineAsClosingParenthesisOfPrecedingReferenceExpression(): Boolean {
        require(elementType == VALUE_PARAMETER_LIST)
        return firstChildLeafOrSelf()
            .let { startOfFunctionLiteral ->
                treeParent
                    ?.takeIf { it.elementType == FUNCTION_LITERAL }
                    ?.prevCodeLeaf()
                    ?.takeIf { it.treeParent.elementType == ElementType.VALUE_ARGUMENT_LIST }
                    ?.takeIf { it.treeParent.treeParent.elementType == ElementType.CALL_EXPRESSION }
                    ?.leaves()
                    ?.takeWhile { it != startOfFunctionLiteral }
                    ?.none { it.isWhiteSpaceWithNewline() }
                    ?: false
            }
    }

    private fun ASTNode.isFunctionTypeWrappedInNullableType(): Boolean {
        require(elementType == VALUE_PARAMETER_LIST)
        return treeParent.elementType == FUNCTION_TYPE && treeParent?.treeParent?.elementType == NULLABLE_TYPE
    }

    private fun ASTNode.containsAnnotatedParameter(): Boolean {
        require(elementType == VALUE_PARAMETER_LIST)
        return this.children()
            .filter { it.elementType == VALUE_PARAMETER }
            .any { it.isAnnotated() }
    }

    private fun ASTNode.isAnnotated() =
        findChildByType(ElementType.MODIFIER_LIST)
            ?.children()
            .orEmpty()
            .any { it.elementType == ElementType.ANNOTATION_ENTRY }

    private fun wrapParameterList(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
        autoCorrect: Boolean,
    ) {
        val newIndentLevel = getNewIndentLevel(node)
        node
            .children()
            .forEach { child -> wrapParameterInList(newIndentLevel, child, emit, autoCorrect) }
    }

    private fun getNewIndentLevel(node: ASTNode): Int {
        val currentIndentLevel = indentConfig.indentLevelFrom(node.indent(false))
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

    private fun wrapParameterInList(
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
                val intendedIndent =
                    if (child.elementType == VALUE_PARAMETER) {
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

    private fun ASTNode.exceedsMaxLineLength() = (column - 1 + textLength) > maxLineLength && !textContains('\n')

    private fun errorMessage(node: ASTNode) =
        when (node.elementType) {
            LPAR -> """Unnecessary newline before "(""""
            VALUE_PARAMETER ->
                "Parameter should start on a newline"
            RPAR -> """Missing newline before ")""""
            else -> throw UnsupportedOperationException()
        }

    private fun ASTNode.hasTypeParameterListInFront(): Boolean {
        val parent = this.treeParent
        val typeParameterList =
            if (parent.elementType == PRIMARY_CONSTRUCTOR) {
                parent.prevSibling { it.elementType == TYPE_PARAMETER_LIST }
            } else {
                parent.children().firstOrNull { it.elementType == TYPE_PARAMETER_LIST }
            }
        val typeListNode =
            typeParameterList
                ?: parent.psi.collectDescendantsOfType<KtTypeArgumentList>().firstOrNull()?.node
                ?: return false
        return typeListNode.children().any { it.isWhiteSpaceWithNewline() }
    }
}

public val PARAMETER_LIST_WRAPPING_RULE_ID: RuleId = ParameterListWrappingRule().ruleId
