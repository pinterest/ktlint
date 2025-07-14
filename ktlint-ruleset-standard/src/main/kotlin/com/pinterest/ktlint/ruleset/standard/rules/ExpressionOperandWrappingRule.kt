package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.ElementType
import com.pinterest.ktlint.rule.engine.core.api.ElementType.BINARY_EXPRESSION
import com.pinterest.ktlint.rule.engine.core.api.ElementType.EOL_COMMENT
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
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpaceWithoutNewline20
import com.pinterest.ktlint.rule.engine.core.api.lastChildLeafOrSelf20
import com.pinterest.ktlint.rule.engine.core.api.leavesInClosedRange
import com.pinterest.ktlint.rule.engine.core.api.nextSibling20
import com.pinterest.ktlint.rule.engine.core.api.upsertWhitespaceAfterMe
import com.pinterest.ktlint.ruleset.standard.StandardRule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.tree.TokenSet
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
            .takeIf { it.isBinaryExpressionWithWrappableOperand() }
            ?.takeIf { it.isMultiline() }
            ?.let { visitMultilineBinaryExpression(it, emit) }
    }

    private fun ASTNode.isBinaryExpressionWithWrappableOperand() =
        null !=
            takeIf { elementType == BINARY_EXPRESSION }
                ?.findChildByType(OPERATION_REFERENCE)
                ?.firstChildNode
                ?.takeIf { it.elementType in WRAPPABLE_OPERAND }

    private fun visitMultilineBinaryExpression(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        node
            .findChildByType(OPERATION_REFERENCE)
            ?.takeUnless { it.nextSibling20.isWhiteSpaceWithNewline20 }
            ?.takeUnless {
                it.nextSibling20.isWhiteSpaceWithoutNewline20 &&
                    it.nextSibling20?.nextSibling20?.elementType == EOL_COMMENT
            }?.let { operationReference ->
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
                .takeWhile { it.isBinaryExpressionWithWrappableOperand() }
                .firstOrNull { predicate(it) }

    private companion object {
        val WRAPPABLE_OPERAND =
            TokenSet.create(
                // Logical operands
                ElementType.ANDAND,
                ElementType.OROR,
                // Arithmetic operands
                ElementType.PLUS,
                ElementType.MINUS,
                ElementType.MUL,
                ElementType.DIV,
            )
    }
}

public val EXPRESSION_OPERAND_WRAPPING_RULE_ID: RuleId = ExpressionOperandWrappingRule().ruleId
