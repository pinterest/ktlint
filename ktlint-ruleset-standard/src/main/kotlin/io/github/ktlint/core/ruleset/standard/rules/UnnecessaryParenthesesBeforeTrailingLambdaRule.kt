package io.github.ktlint.core.ruleset.standard.rules

import io.github.ktlint.core.rule.engine.core.api.AutocorrectDecision
import io.github.ktlint.core.rule.engine.core.api.ElementType.CALL_EXPRESSION
import io.github.ktlint.core.rule.engine.core.api.ElementType.FUNCTION_LITERAL
import io.github.ktlint.core.rule.engine.core.api.ElementType.LAMBDA_ARGUMENT
import io.github.ktlint.core.rule.engine.core.api.ElementType.LAMBDA_EXPRESSION
import io.github.ktlint.core.rule.engine.core.api.ElementType.LPAR
import io.github.ktlint.core.rule.engine.core.api.ElementType.RPAR
import io.github.ktlint.core.rule.engine.core.api.ElementType.VALUE_ARGUMENT_LIST
import io.github.ktlint.core.rule.engine.core.api.RuleId
import io.github.ktlint.core.rule.engine.core.api.SinceKtlint
import io.github.ktlint.core.rule.engine.core.api.SinceKtlint.Status.EXPERIMENTAL
import io.github.ktlint.core.rule.engine.core.api.SinceKtlint.Status.STABLE
import io.github.ktlint.core.rule.engine.core.api.children
import io.github.ktlint.core.rule.engine.core.api.ifAutocorrectAllowed
import io.github.ktlint.core.rule.engine.core.api.isPartOf
import io.github.ktlint.core.rule.engine.core.api.nextCodeSibling
import io.github.ktlint.core.rule.engine.core.api.prevCodeSibling
import io.github.ktlint.core.rule.engine.core.api.remove
import io.github.ktlint.core.ruleset.standard.StandardRule
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
            node.nextCodeSibling?.elementType == LAMBDA_ARGUMENT &&
            node.prevCodeSibling?.elementType != CALL_EXPRESSION
        ) {
            emit(
                node.startOffset,
                "Empty parentheses in function call followed by lambda are unnecessary",
                true,
            ).ifAutocorrectAllowed { node.remove() }
        }
    }

    private fun ASTNode.isEmptyArgumentList(): Boolean =
        elementType == VALUE_ARGUMENT_LIST &&
            children
                .filterNot { it.elementType == LPAR || it.elementType == RPAR }
                .none()

    private fun ASTNode.isNotPrecededByCallExpressionEndingWithLambdaArgument() =
        prevCodeSibling
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
