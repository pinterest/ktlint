package com.pinterest.ktlint.ruleset.standard.rules

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
import com.pinterest.ktlint.rule.engine.core.api.ElementType.OBJECT_LITERAL
import com.pinterest.ktlint.rule.engine.core.api.ElementType.OPERATION_REFERENCE
import com.pinterest.ktlint.rule.engine.core.api.ElementType.PREFIX_EXPRESSION
import com.pinterest.ktlint.rule.engine.core.api.ElementType.REFERENCE_EXPRESSION
import com.pinterest.ktlint.rule.engine.core.api.ElementType.RPAR
import com.pinterest.ktlint.rule.engine.core.api.ElementType.SAFE_ACCESS_EXPRESSION
import com.pinterest.ktlint.rule.engine.core.api.ElementType.TRY
import com.pinterest.ktlint.rule.engine.core.api.ElementType.VALUE_ARGUMENT
import com.pinterest.ktlint.rule.engine.core.api.ElementType.WHEN
import com.pinterest.ktlint.rule.engine.core.api.IndentConfig
import com.pinterest.ktlint.rule.engine.core.api.IndentConfig.Companion.DEFAULT_INDENT_CONFIG
import com.pinterest.ktlint.rule.engine.core.api.Rule
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.EditorConfig
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.INDENT_SIZE_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.INDENT_STYLE_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.firstChildLeafOrSelf
import com.pinterest.ktlint.rule.engine.core.api.isPartOfComment
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpaceWithNewline
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpaceWithoutNewline
import com.pinterest.ktlint.rule.engine.core.api.lastChildLeafOrSelf
import com.pinterest.ktlint.rule.engine.core.api.leavesIncludingSelf
import com.pinterest.ktlint.rule.engine.core.api.nextLeaf
import com.pinterest.ktlint.rule.engine.core.api.prevCodeLeaf
import com.pinterest.ktlint.rule.engine.core.api.prevCodeSibling
import com.pinterest.ktlint.rule.engine.core.api.prevLeaf
import com.pinterest.ktlint.rule.engine.core.api.upsertWhitespaceBeforeMe
import com.pinterest.ktlint.ruleset.standard.StandardRule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode

/**
 * This rule wraps each multiline expression to a newline.
 */
public class MultilineExpressionWrapping :
    StandardRule(
        id = "multiline-expression-wrapping",
        usesEditorConfigProperties =
            setOf(
                INDENT_SIZE_PROPERTY,
                INDENT_STYLE_PROPERTY,
            ),
    ),
    Rule.Experimental,
    Rule.OfficialCodeStyle {
    private var indentConfig = DEFAULT_INDENT_CONFIG

    override fun beforeFirstNode(editorConfig: EditorConfig) {
        indentConfig =
            IndentConfig(
                indentStyle = editorConfig[INDENT_STYLE_PROPERTY],
                tabWidth = editorConfig[INDENT_SIZE_PROPERTY],
            )
    }

    override fun beforeVisitChildNodes(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        if (node.elementType in CHAINABLE_EXPRESSION &&
            (node.treeParent.elementType !in CHAINABLE_EXPRESSION || node.isRightHandSideOfBinaryExpression())
        ) {
            visitExpression(node, emit, autoCorrect)
        }
        if (node.elementType == BINARY_EXPRESSION &&
            node.treeParent.elementType != BINARY_EXPRESSION
        ) {
            visitExpression(node, emit, autoCorrect)
        }
    }

    private fun visitExpression(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
        autoCorrect: Boolean,
    ) {
        if (node.containsWhitespaceWithNewline() && node.needToWrapMultilineExpression()) {
            node
                .prevLeaf { !it.isPartOfComment() }
                .let { prevLeaf ->
                    if (prevLeaf != null && !prevLeaf.textContains('\n')) {
                        emit(node.startOffset, "A multiline expression should start on a new line", true)
                        if (autoCorrect) {
                            node.upsertWhitespaceBeforeMe(indentConfig.siblingIndentOf(node))
                            node
                                .lastChildLeafOrSelf()
                                .nextLeaf { !it.isWhiteSpaceWithoutNewline() && !it.isPartOfComment() && it.elementType != COMMA }
                                ?.takeIf { !it.isWhiteSpaceWithNewline() }
                                ?.upsertWhitespaceBeforeMe(indentConfig.siblingIndentOf(node))
                        }
                    }
                }
        }
    }

    private fun ASTNode.containsWhitespaceWithNewline(): Boolean {
        val lastLeaf = lastChildLeafOrSelf()
        return firstChildLeafOrSelf()
            .leavesIncludingSelf()
            .takeWhile { it != lastLeaf }
            .any { it.isWhiteSpaceWithNewline() || it.isRegularStringPartWithNewline() }
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
            prevCodeSibling()
                ?.takeIf { it.elementType == EQ || it.elementType == OPERATION_REFERENCE }
                ?.takeUnless { it.isElvisOperator() }
                ?.takeUnless {
                    it.closingParenthesisOfFunctionOrNull()
                        ?.prevLeaf()
                        .isWhiteSpaceWithNewline()
                }

    private fun ASTNode?.isElvisOperator() =
        this != null &&
            elementType == OPERATION_REFERENCE &&
            firstChildNode.elementType == ElementType.ELVIS

    private fun ASTNode.closingParenthesisOfFunctionOrNull() =
        takeIf { treeParent.elementType == FUN }
            ?.prevCodeLeaf()
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

    private fun ASTNode.isAfterArrow() = prevCodeLeaf()?.elementType == ARROW

    private fun ASTNode.isRightHandSideOfBinaryExpression() =
        null !=
            takeIf { it.treeParent.elementType == BINARY_EXPRESSION }
                .takeIf { it?.prevCodeSibling()?.elementType == OPERATION_REFERENCE }

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
                REFERENCE_EXPRESSION,
                SAFE_ACCESS_EXPRESSION,
                TRY,
                WHEN,
            )
    }
}

public val MULTILINE_EXPRESSION_WRAPPING_RULE_ID: RuleId = MultilineExpressionWrapping().ruleId
