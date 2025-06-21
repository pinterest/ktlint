package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.ElementType.CALL_EXPRESSION
import com.pinterest.ktlint.rule.engine.core.api.ElementType.FUNCTION_LITERAL
import com.pinterest.ktlint.rule.engine.core.api.ElementType.LAMBDA_ARGUMENT
import com.pinterest.ktlint.rule.engine.core.api.ElementType.LAMBDA_EXPRESSION
import com.pinterest.ktlint.rule.engine.core.api.ElementType.LPAR
import com.pinterest.ktlint.rule.engine.core.api.ElementType.RPAR
import com.pinterest.ktlint.rule.engine.core.api.ElementType.VALUE_ARGUMENT_LIST
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint.Status.EXPERIMENTAL
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint.Status.STABLE
import com.pinterest.ktlint.rule.engine.core.api.children20
import com.pinterest.ktlint.rule.engine.core.api.ifAutocorrectAllowed
import com.pinterest.ktlint.rule.engine.core.api.isPartOf
import com.pinterest.ktlint.rule.engine.core.api.nextCodeSibling
import com.pinterest.ktlint.rule.engine.core.api.prevCodeSibling
import com.pinterest.ktlint.ruleset.standard.StandardRule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode

/**
 * Ensures there are no unnecessary parentheses before a trailing lambda.
 */
@SinceKtlint("0.44", EXPERIMENTAL)
@SinceKtlint("1.0", STABLE)
public class UnnecessaryParenthesesBeforeTrailingLambdaRule : StandardRule("unnecessary-parentheses-before-trailing-lambda") {
    override fun beforeVisitChildNodes(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        if (node.isEmptyArgumentList() &&
            node.isPartOf(CALL_EXPRESSION) &&
            node.isNotPrecededByCallExpressionEndingWithLambdaArgument() &&
            node.nextCodeSibling()?.elementType == LAMBDA_ARGUMENT
        ) {
            emit(
                node.startOffset,
                "Empty parentheses in function call followed by lambda are unnecessary",
                true,
            ).ifAutocorrectAllowed {
                node.removeChild(node)
            }
        }
    }

    private fun ASTNode.isEmptyArgumentList(): Boolean =
        elementType == VALUE_ARGUMENT_LIST &&
            children20
                .filterNot { it.elementType == LPAR || it.elementType == RPAR }
                .none()

    private fun ASTNode.isNotPrecededByCallExpressionEndingWithLambdaArgument() =
        prevCodeSibling()
            ?.takeIf { it.elementType == CALL_EXPRESSION }
            ?.lastChildNode
            ?.takeIf { it.elementType == LAMBDA_ARGUMENT }
            ?.lastChildNode
            ?.takeIf { it.elementType == LAMBDA_EXPRESSION }
            ?.lastChildNode
            ?.let { it.elementType != FUNCTION_LITERAL }
            ?: true
}

public val UNNECESSARY_PARENTHESES_BEFORE_TRAILING_LAMBDA_RULE_ID: RuleId = UnnecessaryParenthesesBeforeTrailingLambdaRule().ruleId
