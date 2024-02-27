package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.rule.engine.core.api.ElementType
import com.pinterest.ktlint.rule.engine.core.api.ElementType.FUN
import com.pinterest.ktlint.rule.engine.core.api.ElementType.FUNCTION_LITERAL
import com.pinterest.ktlint.rule.engine.core.api.ElementType.FUNCTION_TYPE
import com.pinterest.ktlint.rule.engine.core.api.ElementType.LPAR
import com.pinterest.ktlint.rule.engine.core.api.ElementType.NULLABLE_TYPE
import com.pinterest.ktlint.rule.engine.core.api.ElementType.RPAR
import com.pinterest.ktlint.rule.engine.core.api.ElementType.TYPE_PARAMETER_LIST
import com.pinterest.ktlint.rule.engine.core.api.ElementType.VALUE_PARAMETER
import com.pinterest.ktlint.rule.engine.core.api.ElementType.VALUE_PARAMETER_LIST
import com.pinterest.ktlint.rule.engine.core.api.IndentConfig
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint.Status.STABLE
import com.pinterest.ktlint.rule.engine.core.api.column
import com.pinterest.ktlint.rule.engine.core.api.dropTrailingEolComment
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.CODE_STYLE_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.CodeStyleValue.ktlint_official
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.EditorConfig
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.INDENT_SIZE_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.INDENT_STYLE_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.MAX_LINE_LENGTH_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.firstChildLeafOrSelf
import com.pinterest.ktlint.rule.engine.core.api.indent
import com.pinterest.ktlint.rule.engine.core.api.isPartOfComment
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpace
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpaceWithNewline
import com.pinterest.ktlint.rule.engine.core.api.leavesIncludingSelf
import com.pinterest.ktlint.rule.engine.core.api.nextLeaf
import com.pinterest.ktlint.rule.engine.core.api.prevCodeLeaf
import com.pinterest.ktlint.rule.engine.core.api.prevLeaf
import com.pinterest.ktlint.rule.engine.core.api.upsertWhitespaceAfterMe
import com.pinterest.ktlint.rule.engine.core.api.upsertWhitespaceBeforeMe
import com.pinterest.ktlint.ruleset.standard.StandardRule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.PsiWhiteSpaceImpl
import org.jetbrains.kotlin.psi.psiUtil.children
import org.jetbrains.kotlin.psi.psiUtil.leaves

@SinceKtlint("0.16", STABLE)
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
            NULLABLE_TYPE -> visitNullableType(node, emit, autoCorrect)
            VALUE_PARAMETER_LIST -> visitParameterList(node, emit, autoCorrect)
        }
    }

    private fun visitNullableType(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
        autoCorrect: Boolean,
    ) {
        require(node.elementType == NULLABLE_TYPE)
        node
            .takeUnless {
                // skip when max line length is not exceeded
                (node.column - 1 + node.textLength) <= maxLineLength
            }?.takeUnless { it.textContains('\n') }
            ?.takeIf { it.isFunctionTypeWithNonEmptyValueParameterList() }
            ?.let { nullableType ->
                nullableType
                    .findChildByType(LPAR)
                    ?.takeUnless { it.nextLeaf()?.isWhiteSpaceWithNewline() == true }
                    ?.let { lpar ->
                        emit(
                            lpar.startOffset + 1,
                            "Expected new line before function type as it does not fit on a single line",
                            true,
                        )
                        if (autoCorrect) {
                            lpar.upsertWhitespaceAfterMe(indentConfig.childIndentOf(node))
                        }
                    }
                nullableType
                    .findChildByType(RPAR)
                    ?.takeUnless { it.prevLeaf()?.isWhiteSpaceWithNewline() == true }
                    ?.let { rpar ->
                        emit(
                            rpar.startOffset,
                            "Expected new line after function type as it does not fit on a single line",
                            true,
                        )
                        if (autoCorrect) {
                            rpar.upsertWhitespaceBeforeMe(indentConfig.parentIndentOf(node))
                        }
                    }
            }
    }

    private fun ASTNode.isFunctionTypeWithNonEmptyValueParameterList() =
        null !=
            findChildByType(FUNCTION_TYPE)
                ?.findChildByType(VALUE_PARAMETER_LIST)
                ?.findChildByType(VALUE_PARAMETER)

    private fun ASTNode.needToWrapParameterList() =
        when {
            hasNoParameters() -> false

            codeStyle != ktlint_official && isPartOfFunctionLiteralInNonKtlintOfficialCodeStyle() -> false

            codeStyle == ktlint_official && containsAnnotatedParameter() -> true

            codeStyle == ktlint_official && isPartOfFunctionLiteralStartingOnSameLineAsClosingParenthesisOfPrecedingReferenceExpression() ->
                false

            textContains('\n') -> true

            isOnLineExceedingMaxLineLength() -> true

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

    private fun ASTNode.containsAnnotatedParameter(): Boolean {
        require(elementType == VALUE_PARAMETER_LIST)
        return this
            .children()
            .filter { it.elementType == VALUE_PARAMETER }
            .any { it.isAnnotated() }
    }

    private fun ASTNode.isAnnotated() =
        findChildByType(ElementType.MODIFIER_LIST)
            ?.children()
            .orEmpty()
            .any { it.elementType == ElementType.ANNOTATION_ENTRY }

    private fun visitParameterList(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
        autoCorrect: Boolean,
    ) {
        if (isPrecededByComment(node)) {
            emit(node.startOffset, "Parameter list should not be preceded by a comment", false)
        } else if (node.needToWrapParameterList()) {
            node
                .children()
                .forEach { child -> wrapParameterInList(child, emit, autoCorrect) }
        }
    }

    private fun isPrecededByComment(node: ASTNode) =
        node
            .prevLeaf { !it.isWhiteSpace() }
            ?.prevLeaf()
            ?.isPartOfComment()
            ?: false

    private fun intendedIndent(child: ASTNode): String =
        when {
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
            child.treeParent.isFunWithTypeParameterListInFront() -> -1
            else -> 0
        }.let {
            if (child.elementType == VALUE_PARAMETER) {
                it + 1
            } else {
                it
            }
        }.let { indentLevelFix ->
            val indentLevel =
                indentConfig
                    .indentLevelFrom(child.treeParent.indent(false))
                    .plus(indentLevelFix)
            "\n" + indentConfig.indent.repeat(indentLevel)
        }

    private fun wrapParameterInList(
        child: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
        autoCorrect: Boolean,
    ) {
        when (child.elementType) {
            LPAR -> {
                val prevLeaf = child.prevLeaf()
                if (!child.treeParent.isValueParameterListInFunctionType() &&
                    prevLeaf.isWhiteSpaceWithNewline()
                ) {
                    emit(child.startOffset, errorMessage(child), true)
                    if (autoCorrect) {
                        (prevLeaf as PsiWhiteSpace).delete()
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
                val intendedIndent = intendedIndent(child)
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

    private fun ASTNode.isValueParameterListInFunctionType() =
        FUNCTION_TYPE ==
            takeIf { it.elementType == VALUE_PARAMETER_LIST }
                ?.treeParent
                ?.elementType

    private fun ASTNode.isOnLineExceedingMaxLineLength(): Boolean {
        val stopLeaf = nextLeaf { it.textContains('\n') }?.nextLeaf()
        val lineContent =
            prevLeaf { it.textContains('\n') }
                ?.leavesIncludingSelf()
                ?.takeWhile { it.prevLeaf() != stopLeaf }
                ?.dropTrailingEolComment()
                ?.joinToString(separator = "") { it.text }
                ?.substringAfter('\n')
                ?.substringBefore('\n')
                .orEmpty()
        return lineContent.length > maxLineLength
    }

    private fun errorMessage(node: ASTNode) =
        when (node.elementType) {
            LPAR -> """Unnecessary newline before "(""""

            VALUE_PARAMETER ->
                "Parameter should start on a newline"

            RPAR -> """Missing newline before ")""""

            else -> throw UnsupportedOperationException()
        }

    private fun ASTNode.isFunWithTypeParameterListInFront() =
        treeParent
            .takeIf { elementType == FUN }
            ?.findChildByType(TYPE_PARAMETER_LIST)
            ?.children()
            ?.any { it.isWhiteSpaceWithNewline() }
            ?: false
}

public val PARAMETER_LIST_WRAPPING_RULE_ID: RuleId = ParameterListWrappingRule().ruleId
