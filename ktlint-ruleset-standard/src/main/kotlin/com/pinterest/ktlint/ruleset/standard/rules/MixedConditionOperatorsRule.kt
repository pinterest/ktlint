package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.ElementType.ANDAND
import com.pinterest.ktlint.rule.engine.core.api.ElementType.BINARY_EXPRESSION
import com.pinterest.ktlint.rule.engine.core.api.ElementType.OPERATION_REFERENCE
import com.pinterest.ktlint.rule.engine.core.api.ElementType.OROR
import com.pinterest.ktlint.rule.engine.core.api.Rule
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint.Status.EXPERIMENTAL
import com.pinterest.ktlint.rule.engine.core.api.parent
import com.pinterest.ktlint.ruleset.standard.StandardRule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.tree.TokenSet

/**
 * Conditions should not use a both '&&' and '||' operators between operators at the same level. By using parenthesis the expression is to
 * be clarified.
 */
@SinceKtlint("1.1.0", EXPERIMENTAL)
public class MixedConditionOperatorsRule :
    StandardRule("condition-wrapping"),
    Rule.Experimental {
    override fun beforeVisitChildNodes(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        node
            .takeIf { it.isLogicalBinaryExpression() }
            ?.takeIf { it.isPartOfExpressionUsingDifferentLogicalOperators() }
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
            .parent { it.elementType == BINARY_EXPRESSION && it.parent?.elementType != BINARY_EXPRESSION }
            ?.let { rootBinaryExpression ->
                emit(
                    rootBinaryExpression.startOffset,
                    "A condition with mixed usage of '&&' and '||' is hard to read. Use parenthesis to clarify the (sub)condition.",
                    false,
                )
            }
    }

    private fun ASTNode.anyParentBinaryExpression(predicate: (ASTNode) -> Boolean): Boolean {
        var current: ASTNode? = this
        while (current?.elementType == BINARY_EXPRESSION) {
            if (predicate(current)) {
                return true
            }
            current = current.parent
        }
        return false
    }

    private fun ASTNode.isPartOfExpressionUsingDifferentLogicalOperators(): Boolean {
        val operatorStartNode = findOperatorElementTypeOrNull() ?: return false

        return anyParentBinaryExpression { it.findOperatorElementTypeOrNull() != operatorStartNode }
    }

    private fun ASTNode.findOperatorElementTypeOrNull() = findChildByType(OPERATION_REFERENCE)?.firstChildNode?.elementType

    private companion object {
        val logicalOperators = TokenSet.create(OROR, ANDAND)
    }
}

public val MIXED_CONDITION_OPERATORS_RULE_ID: RuleId = MixedConditionOperatorsRule().ruleId
