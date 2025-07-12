package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.ElementType.BINARY_EXPRESSION
import com.pinterest.ktlint.rule.engine.core.api.ElementType.OPERATION_REFERENCE
import com.pinterest.ktlint.rule.engine.core.api.IndentConfig
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint.Status.EXPERIMENTAL
import com.pinterest.ktlint.rule.engine.core.api.children20
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.EditorConfig
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.INDENT_SIZE_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.INDENT_STYLE_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.firstChildLeafOrSelf20
import com.pinterest.ktlint.rule.engine.core.api.ifAutocorrectAllowed
import com.pinterest.ktlint.rule.engine.core.api.isCode
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpace20
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpaceWithNewline20
import com.pinterest.ktlint.rule.engine.core.api.lastChildLeafOrSelf20
import com.pinterest.ktlint.rule.engine.core.api.leavesInClosedRange
import com.pinterest.ktlint.rule.engine.core.api.nextSibling20
import com.pinterest.ktlint.rule.engine.core.api.upsertWhitespaceAfterMe
import com.pinterest.ktlint.ruleset.standard.StandardRule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.psi.psiUtil.parents

/**
 * Wraps the operands in an expression to a newline whenever not all operands, ignore operands in sub-expressions, do fit on the same line.
 *     val foo1 =
 *         foo || bar ||
 *             baz
 *     val foo2 =
 *         foo + bar +
 *            baz
 * but rewrite them as
 *     val foo1 =
 *         foo ||
 *            bar ||
 *            baz
 *     val foo2 =
 *         foo +
 *            bar +
 *            baz
 */
@SinceKtlint("1.7.0", EXPERIMENTAL)
public class ExpressionOperandWrappingRule :
    StandardRule(
        id = "expression-operand-wrapping",
        usesEditorConfigProperties =
            setOf(
                INDENT_SIZE_PROPERTY,
                INDENT_STYLE_PROPERTY,
            ),
    ) {
    private var indentConfig = IndentConfig.DEFAULT_INDENT_CONFIG

    override fun beforeFirstNode(editorConfig: EditorConfig) {
        indentConfig =
            IndentConfig(
                indentStyle = editorConfig[INDENT_STYLE_PROPERTY],
                tabWidth = editorConfig[INDENT_SIZE_PROPERTY],
            )
    }

    override fun beforeVisitChildNodes(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        node
            .takeIf { it.isBinaryExpression() }
            ?.takeIf { it.isMultiline() }
            ?.let { visitMultilineBinaryExpression(it, emit) }
    }

    private fun ASTNode.isBinaryExpression() =
        null !=
            takeIf { elementType == BINARY_EXPRESSION }
                ?.findChildByType(OPERATION_REFERENCE)

    private fun visitMultilineBinaryExpression(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        node
            .findChildByType(OPERATION_REFERENCE)
            ?.takeUnless { it.nextSibling20.isWhiteSpaceWithNewline20 }
            ?.let { operationReference ->
                val startOffset =
                    with(operationReference) { startOffset + textLength }
                        .plus(
                            operationReference
                                .nextSibling20
                                ?.takeIf { it.isWhiteSpace20 }
                                ?.textLength
                                ?: 0,
                        )
                emit(startOffset, "Newline expected before operand in multiline expression", true)
                    .ifAutocorrectAllowed {
                        operationReference.upsertWhitespaceAfterMe(indentConfig.siblingIndentOf(node))
                    }
            }
    }

    private fun ASTNode.isMultiline(): Boolean {
        if (this.leftHandSide().isMultilineOperand() ||
            this.rightHandSide().isMultilineOperand()
        ) {
            return true
        }

        return anyParentBinaryExpression { parent ->
            parent.children20.any { it.isWhiteSpaceWithNewline20 }
        }
    }

    private fun ASTNode.leftHandSide() = children20.firstOrNull { it.isCode }

    private fun ASTNode.rightHandSide() = children20.lastOrNull { it.isCode }

    private fun ASTNode?.isMultilineOperand() =
        when {
            this == null -> {
                false
            }

            else -> {
                leavesInClosedRange(this.firstChildLeafOrSelf20, this.lastChildLeafOrSelf20)
                    .any { it.isWhiteSpaceWithNewline20 }
            }
        }

    private fun ASTNode.anyParentBinaryExpression(predicate: (ASTNode) -> Boolean): Boolean =
        null !=
            parents()
                .takeWhile { it.elementType == BINARY_EXPRESSION }
                .firstOrNull { predicate(it) }
}

public val EXPRESSION_OPERAND_WRAPPING_RULE_ID: RuleId = ExpressionOperandWrappingRule().ruleId
