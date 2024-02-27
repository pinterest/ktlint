package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.rule.engine.core.api.ElementType.BINARY_EXPRESSION
import com.pinterest.ktlint.rule.engine.core.api.ElementType.CALL_EXPRESSION
import com.pinterest.ktlint.rule.engine.core.api.ElementType.CONDITION
import com.pinterest.ktlint.rule.engine.core.api.ElementType.ELVIS
import com.pinterest.ktlint.rule.engine.core.api.ElementType.EQ
import com.pinterest.ktlint.rule.engine.core.api.ElementType.FUN
import com.pinterest.ktlint.rule.engine.core.api.ElementType.FUNCTION_LITERAL
import com.pinterest.ktlint.rule.engine.core.api.ElementType.LAMBDA_ARGUMENT
import com.pinterest.ktlint.rule.engine.core.api.ElementType.LAMBDA_EXPRESSION
import com.pinterest.ktlint.rule.engine.core.api.ElementType.LBRACE
import com.pinterest.ktlint.rule.engine.core.api.ElementType.LONG_STRING_TEMPLATE_ENTRY
import com.pinterest.ktlint.rule.engine.core.api.ElementType.OPERATION_REFERENCE
import com.pinterest.ktlint.rule.engine.core.api.ElementType.PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.ElementType.RBRACE
import com.pinterest.ktlint.rule.engine.core.api.ElementType.VALUE_ARGUMENT
import com.pinterest.ktlint.rule.engine.core.api.IndentConfig
import com.pinterest.ktlint.rule.engine.core.api.Rule
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint.Status.EXPERIMENTAL
import com.pinterest.ktlint.rule.engine.core.api.children
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.EditorConfig
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.INDENT_SIZE_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.INDENT_STYLE_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.MAX_LINE_LENGTH_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.firstChildLeafOrSelf
import com.pinterest.ktlint.rule.engine.core.api.isCodeLeaf
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpace
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpaceWithNewline
import com.pinterest.ktlint.rule.engine.core.api.lastChildLeafOrSelf
import com.pinterest.ktlint.rule.engine.core.api.leavesOnLine
import com.pinterest.ktlint.rule.engine.core.api.lineLength
import com.pinterest.ktlint.rule.engine.core.api.lineLengthWithoutNewlinePrefix
import com.pinterest.ktlint.rule.engine.core.api.nextLeaf
import com.pinterest.ktlint.rule.engine.core.api.nextSibling
import com.pinterest.ktlint.rule.engine.core.api.noNewLineInClosedRange
import com.pinterest.ktlint.rule.engine.core.api.parent
import com.pinterest.ktlint.rule.engine.core.api.prevLeaf
import com.pinterest.ktlint.rule.engine.core.api.prevSibling
import com.pinterest.ktlint.rule.engine.core.api.upsertWhitespaceAfterMe
import com.pinterest.ktlint.rule.engine.core.api.upsertWhitespaceBeforeMe
import com.pinterest.ktlint.ruleset.standard.StandardRule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.tree.IElementType

/**
 * Wraps a binary expression whenever the expression does not fit on the line. Wrapping a binary expression should take precedence before
 * argument of function calls inside that binary expression are wrapped.
 */
@SinceKtlint("0.50", EXPERIMENTAL)
public class BinaryExpressionWrappingRule :
    StandardRule(
        id = "binary-expression-wrapping",
        usesEditorConfigProperties =
            setOf(
                INDENT_SIZE_PROPERTY,
                INDENT_STYLE_PROPERTY,
                MAX_LINE_LENGTH_PROPERTY,
            ),
    ),
    Rule.Experimental {
    private var indentConfig = IndentConfig.DEFAULT_INDENT_CONFIG
    private var maxLineLength = MAX_LINE_LENGTH_PROPERTY.defaultValue

    override fun beforeFirstNode(editorConfig: EditorConfig) {
        indentConfig =
            IndentConfig(
                indentStyle = editorConfig[INDENT_STYLE_PROPERTY],
                tabWidth = editorConfig[INDENT_SIZE_PROPERTY],
            )
        maxLineLength = editorConfig[MAX_LINE_LENGTH_PROPERTY]
    }

    override fun beforeVisitChildNodes(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        when (node.elementType) {
            BINARY_EXPRESSION -> visitBinaryExpression(node, emit, autoCorrect)
        }
    }

    private fun visitBinaryExpression(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
        autoCorrect: Boolean,
    ) {
        require(node.elementType == BINARY_EXPRESSION)

        // First check whether the entire expression has to be pushed to the next line after and assignment in a property or function
        node
            .takeIf { it.treeParent.elementType.anyOf(PROPERTY, FUN) }
            ?.takeIf { binaryExpression ->
                binaryExpression
                    .prevSibling { it.elementType == EQ }
                    ?.let { noNewLineInClosedRange(it, binaryExpression.firstChildLeafOrSelf()) }
                    ?: false
            }?.takeIf { it.isOnLineExceedingMaxLineLength() }
            ?.let { expression ->
                emit(
                    expression.startOffset,
                    "Line is exceeding max line length. Break line between assignment and expression",
                    true,
                )
                if (autoCorrect) {
                    expression.upsertWhitespaceBeforeMe(indentConfig.childIndentOf(expression))
                }
            }

        // Prefer to wrap the entire binary expression to a newline instead of wrapping the binary expression at the operation reference.
        // E.g. prefer:
        //     fooBar(
        //         "foooooo" + "bar",
        //     )
        // instead of
        //     fooBar("foooooo" +
        //         "bar")
        node
            .takeIf { it.treeParent.elementType == VALUE_ARGUMENT }
            ?.takeUnless {
                // Allow
                //     fooBar(
                //         "tooLongToFitOnSingleLine" +
                //             "bar",
                //     )
                node.prevLeaf().isWhiteSpaceWithNewline()
            }?.takeIf { it.causesMaxLineLengthToBeExceeded() }
            ?.let { expression ->
                emit(
                    expression.startOffset,
                    "Line is exceeding max line length. Break line before expression",
                    true,
                )
                if (autoCorrect) {
                    expression.upsertWhitespaceBeforeMe(indentConfig.childIndentOf(expression))
                }
            }

        // When left hand side is a call expression which causes the max line length to be exceeded then first wrap that expression
        node
            .children()
            .firstOrNull { !it.isCodeLeaf() }
            ?.takeIf { it.elementType == CALL_EXPRESSION }
            ?.takeIf { it.causesMaxLineLengthToBeExceeded() }
            ?.let { callExpression -> visitCallExpression(callExpression, emit, autoCorrect) }

        // The remainder (operation reference plus right hand side) might still cause the max line length to be exceeded
        node
            .takeIf { node.lastChildNode.causesMaxLineLengthToBeExceeded() || node.isPartOfConditionExceedingMaxLineLength() }
            ?.findChildByType(OPERATION_REFERENCE)
            ?.let { operationReference -> visitOperationReference(operationReference, emit, autoCorrect) }
    }

    private fun ASTNode.isPartOfConditionExceedingMaxLineLength() =
        // Checks that when binary expression itself fits on the line, but the closing parenthesis or opening brace does not fit.
        //   // Suppose that X is the last possible character on the
        //   // line                                             X
        //   if (leftHandSideExpression && rightHandSideExpression) {
        treeParent
            .takeIf { it.elementType == CONDITION }
            ?.lastChildLeafOrSelf()
            ?.nextLeaf { it.isWhiteSpaceWithNewline() }
            ?.prevLeaf()
            ?.causesMaxLineLengthToBeExceeded()
            ?: false

    private fun visitCallExpression(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
        autoCorrect: Boolean,
    ) {
        node
            .takeIf { it.elementType == CALL_EXPRESSION }
            ?.takeIf { it.treeParent.elementType == BINARY_EXPRESSION }
            ?.let { callExpression ->
                // Breaking the lambda expression has priority over breaking value arguments
                callExpression
                    .findChildByType(LAMBDA_ARGUMENT)
                    ?.findChildByType(LAMBDA_EXPRESSION)
                    ?.findChildByType(FUNCTION_LITERAL)
                    ?.let { functionLiteral ->
                        functionLiteral
                            .findChildByType(LBRACE)
                            ?.let { lbrace ->
                                emit(lbrace.startOffset + 1, "Newline expected after '{'", true)
                                if (autoCorrect) {
                                    lbrace.upsertWhitespaceAfterMe(indentConfig.childIndentOf(lbrace.treeParent))
                                }
                            }
                        functionLiteral
                            .findChildByType(RBRACE)
                            ?.let { rbrace ->
                                emit(rbrace.startOffset, "Newline expected before '}'", true)
                                if (autoCorrect) {
                                    rbrace.upsertWhitespaceBeforeMe(indentConfig.siblingIndentOf(node.treeParent))
                                }
                            }
                    }
            }
    }

    private fun visitOperationReference(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
        autoCorrect: Boolean,
    ) {
        node
            .takeIf { it.elementType == OPERATION_REFERENCE }
            ?.takeUnless {
                // Allow:
                //   val foo = "string too long to fit on the line" +
                //       "more text"
                it.nextSibling().isWhiteSpaceWithNewline()
            }?.takeIf { it.treeParent.elementType == BINARY_EXPRESSION }
            ?.takeIf { binaryExpression ->
                // Ignore binary expression inside raw string literals. Raw string literals are allowed to exceed max-line-length. Wrapping
                // (each) binary expression inside such a literal seems to create more chaos than it resolves.
                binaryExpression.parent { it.elementType == LONG_STRING_TEMPLATE_ENTRY } == null
            }?.let { operationReference ->
                if (operationReference.firstChildNode.elementType == ELVIS) {
                    operationReference
                        .prevLeaf { it.isWhiteSpace() }
                        .takeUnless { it.isWhiteSpaceWithNewline() }
                        ?.let {
                            // Wrapping after the elvis operator leads to violating the 'chain-wrapping' rule, so it must wrapped itself
                            emit(operationReference.startOffset, "Line is exceeding max line length. Break line before '?:'", true)
                            if (autoCorrect) {
                                operationReference.upsertWhitespaceBeforeMe(indentConfig.childIndentOf(operationReference))
                            }
                        }
                } else {
                    operationReference
                        .nextSibling()
                        ?.let { nextSibling ->
                            emit(
                                nextSibling.startOffset,
                                "Line is exceeding max line length. Break line after '${operationReference.text}' in binary expression",
                                true,
                            )
                            if (autoCorrect) {
                                nextSibling.upsertWhitespaceBeforeMe(indentConfig.childIndentOf(operationReference))
                            }
                        }
                }
            }
    }

    private fun ASTNode.isOnLineExceedingMaxLineLength() = maxLineLength < lineLength(excludeEolComment = true)

    private fun ASTNode.causesMaxLineLengthToBeExceeded() =
        lastChildLeafOrSelf().let { lastChildLeaf ->
            leavesOnLine(excludeEolComment = true)
                .takeWhile { it.prevLeaf() != lastChildLeaf }
                .lineLengthWithoutNewlinePrefix()
        } > maxLineLength
}

private fun IElementType.anyOf(vararg elementType: IElementType): Boolean = elementType.contains(this)

public val BINARY_EXPRESSION_WRAPPING_RULE_ID: RuleId = BinaryExpressionWrappingRule().ruleId
