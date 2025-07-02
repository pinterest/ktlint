package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.ElementType.ANNOTATION_ENTRY
import com.pinterest.ktlint.rule.engine.core.api.ElementType.CALL_EXPRESSION
import com.pinterest.ktlint.rule.engine.core.api.ElementType.FUN
import com.pinterest.ktlint.rule.engine.core.api.ElementType.FUNCTION_LITERAL
import com.pinterest.ktlint.rule.engine.core.api.ElementType.FUNCTION_TYPE
import com.pinterest.ktlint.rule.engine.core.api.ElementType.LPAR
import com.pinterest.ktlint.rule.engine.core.api.ElementType.MODIFIER_LIST
import com.pinterest.ktlint.rule.engine.core.api.ElementType.NULLABLE_TYPE
import com.pinterest.ktlint.rule.engine.core.api.ElementType.RPAR
import com.pinterest.ktlint.rule.engine.core.api.ElementType.TYPE_PARAMETER_LIST
import com.pinterest.ktlint.rule.engine.core.api.ElementType.VALUE_ARGUMENT_LIST
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
import com.pinterest.ktlint.rule.engine.core.api.firstChildLeafOrSelf20
import com.pinterest.ktlint.rule.engine.core.api.ifAutocorrectAllowed
import com.pinterest.ktlint.rule.engine.core.api.indentWithoutNewlinePrefix
import com.pinterest.ktlint.rule.engine.core.api.isPartOfComment20
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpace20
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpaceWithNewline20
import com.pinterest.ktlint.rule.engine.core.api.leavesOnLine20
import com.pinterest.ktlint.rule.engine.core.api.nextLeaf
import com.pinterest.ktlint.rule.engine.core.api.nextSibling20
import com.pinterest.ktlint.rule.engine.core.api.parent
import com.pinterest.ktlint.rule.engine.core.api.prevCodeLeaf
import com.pinterest.ktlint.rule.engine.core.api.prevLeaf
import com.pinterest.ktlint.rule.engine.core.api.remove
import com.pinterest.ktlint.rule.engine.core.api.replaceTextWith
import com.pinterest.ktlint.rule.engine.core.api.upsertWhitespaceAfterMe
import com.pinterest.ktlint.rule.engine.core.api.upsertWhitespaceBeforeMe
import com.pinterest.ktlint.ruleset.standard.StandardRule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
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
        maxLineLength = editorConfig.maxLineLength()
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
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        when (node.elementType) {
            NULLABLE_TYPE -> visitNullableType(node, emit)
            VALUE_PARAMETER_LIST -> visitParameterList(node, emit)
        }
    }

    private fun visitNullableType(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        require(node.elementType == NULLABLE_TYPE)
        node
            .takeUnless {
                // skip when max line length is not exceeded
                (node.column - 1 + node.textLength) <= maxLineLength
            }?.takeUnless { it.isWhiteSpaceWithNewline20 }
            ?.takeIf { it.isFunctionTypeWithNonEmptyValueParameterList() }
            ?.let { nullableType ->
                nullableType
                    .findChildByType(LPAR)
                    ?.takeUnless { it.nextLeaf?.isWhiteSpaceWithNewline20 == true }
                    ?.let { lpar ->
                        emit(
                            lpar.startOffset + 1,
                            "Expected new line before function type as it does not fit on a single line",
                            true,
                        ).ifAutocorrectAllowed {
                            lpar.upsertWhitespaceAfterMe(indentConfig.childIndentOf(node))
                        }
                    }
                nullableType
                    .findChildByType(RPAR)
                    ?.takeUnless { it.prevLeaf?.isWhiteSpaceWithNewline20 == true }
                    ?.let { rpar ->
                        emit(
                            rpar.startOffset,
                            "Expected new line after function type as it does not fit on a single line",
                            true,
                        ).ifAutocorrectAllowed {
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
            hasNoParameters() -> {
                false
            }

            codeStyle != ktlint_official && isPartOfFunctionLiteralInNonKtlintOfficialCodeStyle() -> {
                false
            }

            codeStyle == ktlint_official && containsAnnotatedParameter() -> {
                true
            }

            codeStyle == ktlint_official &&
                isPartOfFunctionLiteralStartingOnSameLineAsClosingParenthesisOfPrecedingReferenceExpression() -> {
                false
            }

            textContains('\n') -> {
                true
            }

            isOnLineExceedingMaxLineLength() -> {
                true
            }

            else -> {
                false
            }
        }

    private fun ASTNode.hasNoParameters(): Boolean {
        require(elementType == VALUE_PARAMETER_LIST)
        return firstChildNode?.nextSibling20?.elementType == RPAR
    }

    private fun ASTNode.isPartOfFunctionLiteralInNonKtlintOfficialCodeStyle(): Boolean {
        require(elementType == VALUE_PARAMETER_LIST)
        return parent?.elementType == FUNCTION_LITERAL
    }

    private fun ASTNode.isPartOfFunctionLiteralStartingOnSameLineAsClosingParenthesisOfPrecedingReferenceExpression(): Boolean {
        require(elementType == VALUE_PARAMETER_LIST)
        return firstChildLeafOrSelf20
            .let { startOfFunctionLiteral ->
                parent
                    ?.takeIf { it.elementType == FUNCTION_LITERAL }
                    ?.prevCodeLeaf
                    ?.takeIf { it.parent?.elementType == VALUE_ARGUMENT_LIST }
                    ?.takeIf { it.parent?.parent?.elementType == CALL_EXPRESSION }
                    ?.leaves()
                    ?.takeWhile { it != startOfFunctionLiteral }
                    ?.none { it.isWhiteSpaceWithNewline20 }
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
        findChildByType(MODIFIER_LIST)
            ?.children()
            .orEmpty()
            .any { it.elementType == ANNOTATION_ENTRY }

    private fun visitParameterList(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        if (isPrecededByComment(node)) {
            emit(node.startOffset, "Parameter list should not be preceded by a comment", false)
        } else if (node.needToWrapParameterList()) {
            node
                .children()
                .forEach { child -> wrapParameterInList(child, emit) }
        }
    }

    private fun isPrecededByComment(node: ASTNode) =
        node
            .prevLeaf { !it.isWhiteSpace20 }
            ?.prevLeaf
            ?.isPartOfComment20
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
            child.parent!!.isFunWithTypeParameterListInFront() -> -1

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
                    .indentLevelFrom(child.parent!!.indentWithoutNewlinePrefix)
                    .plus(indentLevelFix)
            "\n" + indentConfig.indent.repeat(indentLevel)
        }

    private fun wrapParameterInList(
        child: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        when (child.elementType) {
            LPAR -> {
                child
                    .parent
                    ?.takeUnless { it.isValueParameterListInFunctionType() }
                    ?.prevLeaf
                    ?.takeIf { it.isWhiteSpaceWithNewline20 }
                    ?.let { whitespace ->
                        emit(child.startOffset, errorMessage(child), true)
                            .ifAutocorrectAllowed { whitespace.remove() }
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
                val prevLeaf = child.prevLeaf
                when {
                    prevLeaf.isWhiteSpaceWithNewline20 -> {
                        // The current child is already wrapped to a new line. Checking and fixing the
                        // correct size of the indent is the responsibility of the IndentationRule.
                        return
                    }

                    prevLeaf.isWhiteSpace20 -> {
                        // The current child needs to be wrapped to a newline.
                        emit(child.startOffset, errorMessage(child), true)
                            .ifAutocorrectAllowed {
                                // The indentation is purely based on the previous leaf only. Note that in
                                // autoCorrect mode the indent rule, if enabled, runs after this rule and
                                // determines the final indentation. But if the indent rule is disabled then the
                                // indent of this rule is kept.
                                prevLeaf?.replaceTextWith(intendedIndent)
                            }
                    }

                    else -> {
                        // Insert a new whitespace element in order to wrap the current child to a new line.
                        emit(child.startOffset, errorMessage(child), true)
                            .ifAutocorrectAllowed {
                                child.parent?.addChild(PsiWhiteSpaceImpl(intendedIndent), child)
                            }
                    }
                }
                // Indentation of child nodes need to be fixed by the IndentationRule.
            }
        }
    }

    private fun ASTNode.isValueParameterListInFunctionType() =
        FUNCTION_TYPE ==
            takeIf { it.elementType == VALUE_PARAMETER_LIST }
                ?.parent
                ?.elementType

    private fun ASTNode.isOnLineExceedingMaxLineLength(): Boolean {
        val stopLeaf = nextLeaf { it.isWhiteSpaceWithNewline20 }?.nextLeaf
        val lineContent =
            leavesOnLine20
                .dropTrailingEolComment()
                .takeWhile { it.prevLeaf != stopLeaf }
                .joinToString(separator = "") { it.text }
                .substringAfter('\n')
                .substringBefore('\n')
        return lineContent.length > maxLineLength
    }

    private fun errorMessage(node: ASTNode) =
        when (node.elementType) {
            LPAR -> """Unnecessary newline before "(""""
            VALUE_PARAMETER -> "Parameter should start on a newline"
            RPAR -> """Missing newline before ")""""
            else -> throw UnsupportedOperationException()
        }

    private fun ASTNode.isFunWithTypeParameterListInFront() =
        parent
            ?.takeIf { elementType == FUN }
            ?.findChildByType(TYPE_PARAMETER_LIST)
            ?.children()
            ?.any { it.isWhiteSpaceWithNewline20 }
            ?: false
}

public val PARAMETER_LIST_WRAPPING_RULE_ID: RuleId = ParameterListWrappingRule().ruleId
