package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.ElementType.ANDAND
import com.pinterest.ktlint.rule.engine.core.api.ElementType.BINARY_EXPRESSION
import com.pinterest.ktlint.rule.engine.core.api.ElementType.OPERATION_REFERENCE
import com.pinterest.ktlint.rule.engine.core.api.ElementType.OROR
import com.pinterest.ktlint.rule.engine.core.api.IndentConfig
import com.pinterest.ktlint.rule.engine.core.api.Rule
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint.Status.EXPERIMENTAL
import com.pinterest.ktlint.rule.engine.core.api.children
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.EditorConfig
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.INDENT_SIZE_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.INDENT_STYLE_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.firstChildLeafOrSelf
import com.pinterest.ktlint.rule.engine.core.api.ifAutocorrectAllowed
import com.pinterest.ktlint.rule.engine.core.api.isPartOfComment
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpace
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpaceWithNewline
import com.pinterest.ktlint.rule.engine.core.api.lastChildLeafOrSelf
import com.pinterest.ktlint.rule.engine.core.api.leavesInClosedRange
import com.pinterest.ktlint.rule.engine.core.api.nextSibling
import com.pinterest.ktlint.rule.engine.core.api.upsertWhitespaceAfterMe
import com.pinterest.ktlint.ruleset.standard.StandardRule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.tree.TokenSet

/**
 * Wraps a condition (a boolean binary expression) whenever the expression does not fit on the line. In addition to the
 * `binary-expression-wrapping`, operands are being wrapped in multiline expressions.
 */
@SinceKtlint("1.1.0", EXPERIMENTAL)
public class ConditionWrappingRule :
    StandardRule(
        id = "condition-wrapping",
        usesEditorConfigProperties =
            setOf(
                INDENT_SIZE_PROPERTY,
                INDENT_STYLE_PROPERTY,
            ),
    ),
    Rule.Experimental {
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
            .takeIf { it.isLogicalBinaryExpression() }
            ?.takeIf { it.isMultiline() }
            ?.let {
                visitLogicalExpression(it, emit)
            }
    }

    private fun ASTNode.isLogicalBinaryExpression() =
        takeIf { elementType == BINARY_EXPRESSION }
            ?.findChildByType(OPERATION_REFERENCE)
            ?.let { it.firstChildNode.elementType in logicalOperators }
            ?: false

    private fun visitLogicalExpression(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        node
            .findChildByType(OPERATION_REFERENCE)
            ?.takeUnless { it.nextSibling().isWhiteSpaceWithNewline() }
            ?.let { operationReference ->
                val startOffset =
                    with(operationReference) { startOffset + textLength }
                        .plus(
                            operationReference
                                .nextSibling()
                                ?.takeIf { it.isWhiteSpace() }
                                ?.textLength
                                ?: 0,
                        )
                emit(startOffset, "Newline expected before operand in multiline condition", true)
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
            parent.children().any { it.isWhiteSpaceWithNewline() }
        }
    }

    private fun ASTNode.leftHandSide() = children().firstOrNull { !it.isWhiteSpace() && !it.isPartOfComment() }

    private fun ASTNode.rightHandSide() = children().lastOrNull { !it.isWhiteSpace() && !it.isPartOfComment() }

    private fun ASTNode?.isMultilineOperand() =
        if (this == null) {
            false
        } else {
            leavesInClosedRange(this.firstChildLeafOrSelf(), this.lastChildLeafOrSelf())
                .any { it.isWhiteSpaceWithNewline() }
        }

    private fun ASTNode.anyParentBinaryExpression(predicate: (ASTNode) -> Boolean): Boolean {
        var current = this
        while (current.elementType == BINARY_EXPRESSION) {
            if (predicate(current)) {
                return true
            }
            current = current.treeParent
        }
        return false
    }

    private companion object {
        val logicalOperators = TokenSet.create(OROR, ANDAND)
    }
}

public val CONDITION_WRAPPING_RULE_ID: RuleId = ConditionWrappingRule().ruleId
