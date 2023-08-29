package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.rule.engine.core.api.ElementType.BINARY_EXPRESSION
import com.pinterest.ktlint.rule.engine.core.api.ElementType.CALL_EXPRESSION
import com.pinterest.ktlint.rule.engine.core.api.ElementType.ELVIS
import com.pinterest.ktlint.rule.engine.core.api.ElementType.EQ
import com.pinterest.ktlint.rule.engine.core.api.ElementType.FUN
import com.pinterest.ktlint.rule.engine.core.api.ElementType.LAMBDA_ARGUMENT
import com.pinterest.ktlint.rule.engine.core.api.ElementType.LONG_STRING_TEMPLATE_ENTRY
import com.pinterest.ktlint.rule.engine.core.api.ElementType.OPERATION_REFERENCE
import com.pinterest.ktlint.rule.engine.core.api.ElementType.PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.ElementType.VALUE_ARGUMENT_LIST
import com.pinterest.ktlint.rule.engine.core.api.IndentConfig
import com.pinterest.ktlint.rule.engine.core.api.Rule
import com.pinterest.ktlint.rule.engine.core.api.Rule.VisitorModifier.RunAfterRule
import com.pinterest.ktlint.rule.engine.core.api.Rule.VisitorModifier.RunAfterRule.Mode.REGARDLESS_WHETHER_RUN_AFTER_RULE_IS_LOADED_OR_DISABLED
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint.Status.EXPERIMENTAL
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.EditorConfig
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.INDENT_SIZE_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.INDENT_STYLE_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.MAX_LINE_LENGTH_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.firstChildLeafOrSelf
import com.pinterest.ktlint.rule.engine.core.api.indent
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpaceWithNewline
import com.pinterest.ktlint.rule.engine.core.api.leavesOnLine
import com.pinterest.ktlint.rule.engine.core.api.nextCodeSibling
import com.pinterest.ktlint.rule.engine.core.api.nextSibling
import com.pinterest.ktlint.rule.engine.core.api.noNewLineInClosedRange
import com.pinterest.ktlint.rule.engine.core.api.parent
import com.pinterest.ktlint.rule.engine.core.api.prevLeaf
import com.pinterest.ktlint.rule.engine.core.api.prevSibling
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
        visitorModifiers =
            setOf(
                RunAfterRule(ARGUMENT_LIST_WRAPPING_RULE_ID, REGARDLESS_WHETHER_RUN_AFTER_RULE_IS_LOADED_OR_DISABLED),
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
            BINARY_EXPRESSION -> visitExpression(node, emit, autoCorrect)
        }
    }

    private fun visitExpression(
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
                    expression.upsertWhitespaceBeforeMe(expression.indent().plus(indentConfig.indent))
                }
            }

        node
            .findChildByType(OPERATION_REFERENCE)
            ?.let { operationReference -> visitOperationReference(operationReference, emit, autoCorrect) }
    }

    private fun visitOperationReference(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
        autoCorrect: Boolean,
    ) {
        node
            .takeIf { it.elementType == OPERATION_REFERENCE }
            ?.takeIf { it.treeParent.elementType == BINARY_EXPRESSION }
            ?.takeIf { binaryExpression ->
                // Ignore binary expression inside raw string literals. Raw string literals are allowed to exceed max-line-length. Wrapping
                // (each) binary expression inside such a literal seems to create more chaos than it resolves.
                binaryExpression.parent { it.elementType == LONG_STRING_TEMPLATE_ENTRY } == null
            }?.takeIf { it.isOnLineExceedingMaxLineLength() }
            ?.let { operationReference ->
                if (node.isCallExpressionFollowedByLambdaArgument() || cannotBeWrappedAtOperationReference(operationReference)) {
                    // Wrapping after operation reference might not be the best place in case of a call expression or just won't work as
                    // the left hand side still does not fit on a single line
                    val offset =
                        operationReference
                            .prevLeaf { it.isWhiteSpaceWithNewline() }
                            ?.let { previousNewlineNode ->
                                previousNewlineNode.startOffset +
                                    previousNewlineNode.text.indexOfLast { it == '\n' } +
                                    1
                            }
                            ?: operationReference.startOffset
                    emit(offset, "Line is exceeding max line length", false)
                } else {
                    operationReference
                        .nextSibling()
                        ?.let { nextSibling ->
                            emit(
                                nextSibling.startOffset,
                                "Line is exceeding max line length. Break line after operator in binary expression",
                                true,
                            )
                            if (autoCorrect) {
                                nextSibling.upsertWhitespaceBeforeMe(operationReference.indent().plus(indentConfig.indent))
                            }
                        }
                }
            }
    }

    private fun ASTNode.isCallExpressionFollowedByLambdaArgument() =
        parent { it.elementType == VALUE_ARGUMENT_LIST }
            ?.takeIf { it.treeParent.elementType == CALL_EXPRESSION }
            ?.nextCodeSibling()
            .let { it?.elementType == LAMBDA_ARGUMENT }

    private fun cannotBeWrappedAtOperationReference(operationReference: ASTNode) =
        if (operationReference.firstChildNode.elementType == ELVIS) {
            true
        } else {
            operationReference
                .takeUnless { it.nextCodeSibling()?.elementType == BINARY_EXPRESSION }
                ?.let {
                    val stopAtOperationReferenceLeaf = operationReference.firstChildLeafOrSelf()
                    maxLineLength <=
                        it
                            .leavesOnLine()
                            .takeWhile { leaf -> leaf != stopAtOperationReferenceLeaf }
                            .lengthWithoutNewlinePrefix()
                }
                ?: false
        }

    private fun ASTNode.isOnLineExceedingMaxLineLength() = leavesOnLine().lengthWithoutNewlinePrefix() > maxLineLength

    private fun Sequence<ASTNode>.lengthWithoutNewlinePrefix() =
        joinToString(separator = "") { it.text }
            .dropWhile { it == '\n' }
            .length
}

private fun IElementType.anyOf(vararg elementType: IElementType): Boolean = elementType.contains(this)

public val BINARY_EXPRESSION_WRAPPING_RULE_ID: RuleId = BinaryExpressionWrappingRule().ruleId
