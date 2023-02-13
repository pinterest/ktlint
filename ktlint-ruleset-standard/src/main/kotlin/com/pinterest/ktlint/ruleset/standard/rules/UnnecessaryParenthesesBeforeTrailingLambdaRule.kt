package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.rule.engine.core.api.ElementType.CALL_EXPRESSION
import com.pinterest.ktlint.rule.engine.core.api.ElementType.LAMBDA_ARGUMENT
import com.pinterest.ktlint.rule.engine.core.api.ElementType.LPAR
import com.pinterest.ktlint.rule.engine.core.api.ElementType.RPAR
import com.pinterest.ktlint.rule.engine.core.api.ElementType.VALUE_ARGUMENT_LIST
import com.pinterest.ktlint.rule.engine.core.api.Rule
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.children
import com.pinterest.ktlint.rule.engine.core.api.isPartOf
import com.pinterest.ktlint.rule.engine.core.api.nextCodeSibling
import com.pinterest.ktlint.ruleset.standard.StandardRule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode

/**
 * Ensures there are no unnecessary parentheses before a trailing lambda.
 */
public class UnnecessaryParenthesesBeforeTrailingLambdaRule :
    StandardRule("unnecessary-parentheses-before-trailing-lambda"),
    Rule.Experimental {
    override fun beforeVisitChildNodes(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        if (node.isPartOf(CALL_EXPRESSION) &&
            node.isEmptyArgumentList() &&
            node.nextCodeSibling()?.elementType == LAMBDA_ARGUMENT
        ) {
            emit(
                node.startOffset,
                "Empty parentheses in function call followed by lambda are unnecessary",
                true,
            )
            if (autoCorrect) {
                node.removeChild(node)
            }
        }
    }

    private fun ASTNode.isEmptyArgumentList(): Boolean =
        elementType == VALUE_ARGUMENT_LIST &&
            children()
                .filterNot { it.elementType == LPAR || it.elementType == RPAR }
                .none()
}

public val UNNECESSARY_PARENTHESES_BEFORE_TRAILING_LAMBDA_RULE_ID: RuleId = UnnecessaryParenthesesBeforeTrailingLambdaRule().ruleId
