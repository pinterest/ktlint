package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.ElementType
import com.pinterest.ktlint.rule.engine.core.api.ElementType.ARRAY_ACCESS_EXPRESSION
import com.pinterest.ktlint.rule.engine.core.api.ElementType.ARROW
import com.pinterest.ktlint.rule.engine.core.api.ElementType.BINARY_EXPRESSION
import com.pinterest.ktlint.rule.engine.core.api.ElementType.BINARY_WITH_TYPE
import com.pinterest.ktlint.rule.engine.core.api.ElementType.BLOCK
import com.pinterest.ktlint.rule.engine.core.api.ElementType.CALL_EXPRESSION
import com.pinterest.ktlint.rule.engine.core.api.ElementType.COMMA
import com.pinterest.ktlint.rule.engine.core.api.ElementType.DOT_QUALIFIED_EXPRESSION
import com.pinterest.ktlint.rule.engine.core.api.ElementType.EQ
import com.pinterest.ktlint.rule.engine.core.api.ElementType.FUN
import com.pinterest.ktlint.rule.engine.core.api.ElementType.IF
import com.pinterest.ktlint.rule.engine.core.api.ElementType.IS_EXPRESSION
import com.pinterest.ktlint.rule.engine.core.api.ElementType.MUL
import com.pinterest.ktlint.rule.engine.core.api.ElementType.OBJECT_LITERAL
import com.pinterest.ktlint.rule.engine.core.api.ElementType.OPERATION_REFERENCE
import com.pinterest.ktlint.rule.engine.core.api.ElementType.POSTFIX_EXPRESSION
import com.pinterest.ktlint.rule.engine.core.api.ElementType.PREFIX_EXPRESSION
import com.pinterest.ktlint.rule.engine.core.api.ElementType.REFERENCE_EXPRESSION
import com.pinterest.ktlint.rule.engine.core.api.ElementType.RPAR
import com.pinterest.ktlint.rule.engine.core.api.ElementType.SAFE_ACCESS_EXPRESSION
import com.pinterest.ktlint.rule.engine.core.api.ElementType.TRY
import com.pinterest.ktlint.rule.engine.core.api.ElementType.VALUE_ARGUMENT
import com.pinterest.ktlint.rule.engine.core.api.ElementType.VALUE_ARGUMENT_LIST
import com.pinterest.ktlint.rule.engine.core.api.ElementType.VALUE_PARAMETER_LIST
import com.pinterest.ktlint.rule.engine.core.api.ElementType.WHEN
import com.pinterest.ktlint.rule.engine.core.api.IndentConfig
import com.pinterest.ktlint.rule.engine.core.api.IndentConfig.Companion.DEFAULT_INDENT_CONFIG
import com.pinterest.ktlint.rule.engine.core.api.Rule
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint.Status.EXPERIMENTAL
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint.Status.STABLE
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.EditorConfig
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.INDENT_SIZE_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.INDENT_STYLE_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.firstChildLeafOrSelf20
import com.pinterest.ktlint.rule.engine.core.api.ifAutocorrectAllowed
import com.pinterest.ktlint.rule.engine.core.api.isPartOfComment20
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpaceWithNewline20
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpaceWithoutNewline20
import com.pinterest.ktlint.rule.engine.core.api.lastChildLeafOrSelf20
import com.pinterest.ktlint.rule.engine.core.api.leavesForwardsIncludingSelf
import com.pinterest.ktlint.rule.engine.core.api.nextLeaf
import com.pinterest.ktlint.rule.engine.core.api.prevCodeLeaf
import com.pinterest.ktlint.rule.engine.core.api.prevCodeSibling20
import com.pinterest.ktlint.rule.engine.core.api.prevLeaf
import com.pinterest.ktlint.rule.engine.core.api.upsertWhitespaceBeforeMe
import com.pinterest.ktlint.ruleset.standard.StandardRule
import com.pinterest.ktlint.ruleset.standard.rules.FunctionSignatureRule.Companion.FUNCTION_BODY_EXPRESSION_WRAPPING_PROPERTY
import com.pinterest.ktlint.ruleset.standard.rules.FunctionSignatureRule.FunctionBodyExpressionWrapping.default
import org.jetbrains.kotlin.com.intellij.lang.ASTNode

/**
 * This rule wraps each multiline expression to a newline.
 */
@SinceKtlint("0.49", EXPERIMENTAL)
@SinceKtlint("1.0", STABLE)
public class MultilineExpressionWrappingRule :
    StandardRule(
        id = "multiline-expression-wrapping",
        usesEditorConfigProperties =
            setOf(
                INDENT_SIZE_PROPERTY,
                INDENT_STYLE_PROPERTY,
                FUNCTION_BODY_EXPRESSION_WRAPPING_PROPERTY,
            ),
    ),
    Rule.OfficialCodeStyle {
    private var indentConfig = DEFAULT_INDENT_CONFIG
    private lateinit var functionBodyExpressionWrapping: FunctionSignatureRule.FunctionBodyExpressionWrapping

    override fun beforeFirstNode(editorConfig: EditorConfig) {
        indentConfig =
            IndentConfig(
                indentStyle = editorConfig[INDENT_STYLE_PROPERTY],
                tabWidth = editorConfig[INDENT_SIZE_PROPERTY],
            )
        functionBodyExpressionWrapping = editorConfig[FUNCTION_BODY_EXPRESSION_WRAPPING_PROPERTY]
    }

    override fun beforeVisitChildNodes(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        if (node.elementType in CHAINABLE_EXPRESSION &&
            !node.isPartOfSpreadOperatorExpression() &&
            (node.treeParent.elementType !in CHAINABLE_EXPRESSION || node.isRightHandSideOfBinaryExpression())
        ) {
            visitExpression(node, emit)
        }
        if (node.elementType == BINARY_EXPRESSION &&
            node.treeParent.elementType != BINARY_EXPRESSION
        ) {
            visitExpression(node, emit)
        }
    }

    private fun ASTNode.isPartOfSpreadOperatorExpression() =
        prevCodeLeaf?.elementType == MUL &&
            treeParent.elementType == VALUE_ARGUMENT

    private fun visitExpression(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        if (node.containsWhitespaceWithNewline() && node.needToWrapMultilineExpression()) {
            node
                .prevLeaf { !it.isPartOfComment20 }
                .takeUnless { it.isWhiteSpaceWithNewline20 }
                ?.let { prevLeaf ->
                    emit(node.startOffset, "A multiline expression should start on a new line", true)
                        .ifAutocorrectAllowed {
                            node.upsertWhitespaceBeforeMe(indentConfig.siblingIndentOf(node))
                            val leafOnSameLineAfterMultilineExpression =
                                node
                                    .lastChildLeafOrSelf20
                                    .nextLeaf { !it.isWhiteSpaceWithoutNewline20 && !it.isPartOfComment20 }
                                    ?.takeIf { !it.isWhiteSpaceWithNewline20 }
                            when {
                                leafOnSameLineAfterMultilineExpression == null -> {
                                    Unit
                                }

                                leafOnSameLineAfterMultilineExpression.treeParent.elementType == OPERATION_REFERENCE -> {
                                    // When binary expressions are wrapped, each binary expression for itself is checked whether it is a
                                    // multiline expression. So there is no need to check whether wrapping after the operation reference is
                                    // needed
                                    Unit
                                }

                                leafOnSameLineAfterMultilineExpression.elementType == COMMA &&
                                    (
                                        leafOnSameLineAfterMultilineExpression.treeParent.elementType == VALUE_ARGUMENT_LIST ||
                                            leafOnSameLineAfterMultilineExpression.treeParent.elementType == VALUE_PARAMETER_LIST
                                    ) -> {
                                    // Keep comma on same line as multiline expression:
                                    //   foo(
                                    //      fooBar
                                    //          .filter { it.bar },
                                    //   )
                                    leafOnSameLineAfterMultilineExpression
                                        .nextLeaf
                                        ?.upsertWhitespaceBeforeMe(indentConfig.siblingIndentOf(node))
                                }

                                else -> {
                                    leafOnSameLineAfterMultilineExpression.upsertWhitespaceBeforeMe(indentConfig.siblingIndentOf(node))
                                }
                            }
                        }
                }
        }
    }

    private fun ASTNode.containsWhitespaceWithNewline(): Boolean {
        val lastLeaf = lastChildLeafOrSelf20
        return firstChildLeafOrSelf20
            .leavesForwardsIncludingSelf
            .takeWhile { it != lastLeaf }
            .any { it.isWhiteSpaceWithNewline20 || it.isRegularStringPartWithNewline() }
    }

    private fun ASTNode.isRegularStringPartWithNewline() =
        elementType == ElementType.REGULAR_STRING_PART &&
            text.startsWith("\n")

    private fun ASTNode.needToWrapMultilineExpression() =
        isValueInAnAssignment() ||
            isLambdaExpression() ||
            isValueArgument() ||
            isAfterArrow()

    private fun ASTNode.isValueInAnAssignment() =
        null !=
            prevCodeSibling20
                ?.takeIf { it.elementType == EQ || it.elementType == OPERATION_REFERENCE }
                ?.takeUnless { functionBodyExpressionWrapping == default && it.treeParent.elementType == FUN }
                ?.takeUnless { it.isElvisOperator() }
                ?.takeUnless {
                    it
                        .closingParenthesisOfFunctionOrNull()
                        ?.prevLeaf
                        .isWhiteSpaceWithNewline20
                }

    private fun ASTNode?.isElvisOperator() =
        this != null &&
            elementType == OPERATION_REFERENCE &&
            firstChildNode.elementType == ElementType.ELVIS

    private fun ASTNode.closingParenthesisOfFunctionOrNull() =
        takeIf { treeParent.elementType == FUN }
            ?.prevCodeLeaf
            ?.takeIf { it.elementType == RPAR }

    private fun ASTNode.isLambdaExpression() =
        null !=
            treeParent
                .takeIf {
                    // Function literals in lambda expression have an implicit block (no LBRACE and RBRACE). So only wrap when the node is
                    // the first node in the block
                    it.elementType == BLOCK && it.firstChildNode == this
                }?.treeParent
                ?.takeIf { it.elementType == ElementType.FUNCTION_LITERAL }
                ?.treeParent
                ?.takeIf { it.elementType == ElementType.LAMBDA_EXPRESSION }

    private fun ASTNode.isValueArgument() = treeParent.elementType == VALUE_ARGUMENT

    private fun ASTNode.isAfterArrow() = prevCodeLeaf?.elementType == ARROW

    private fun ASTNode.isRightHandSideOfBinaryExpression() =
        null !=
            takeIf { it.treeParent.elementType == BINARY_EXPRESSION }
                .takeIf { it?.prevCodeSibling20?.elementType == OPERATION_REFERENCE }

    private companion object {
        // Based  on https://kotlinlang.org/spec/expressions.html#expressions
        val CHAINABLE_EXPRESSION =
            setOf(
                ARRAY_ACCESS_EXPRESSION,
                BINARY_WITH_TYPE,
                CALL_EXPRESSION,
                DOT_QUALIFIED_EXPRESSION,
                IF,
                IS_EXPRESSION,
                OBJECT_LITERAL,
                PREFIX_EXPRESSION,
                POSTFIX_EXPRESSION,
                REFERENCE_EXPRESSION,
                SAFE_ACCESS_EXPRESSION,
                TRY,
                WHEN,
            )
    }
}

public val MULTILINE_EXPRESSION_WRAPPING_RULE_ID: RuleId = MultilineExpressionWrappingRule().ruleId
