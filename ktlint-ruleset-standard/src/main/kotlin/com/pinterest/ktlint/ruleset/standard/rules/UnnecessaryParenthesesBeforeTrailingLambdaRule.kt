package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.ruleset.core.api.ElementType.CALL_EXPRESSION
import com.pinterest.ktlint.ruleset.core.api.ElementType.LAMBDA_ARGUMENT
import com.pinterest.ktlint.ruleset.core.api.ElementType.LPAR
import com.pinterest.ktlint.ruleset.core.api.ElementType.RPAR
import com.pinterest.ktlint.ruleset.core.api.ElementType.VALUE_ARGUMENT_LIST
import com.pinterest.ktlint.ruleset.core.api.children
import com.pinterest.ktlint.ruleset.core.api.isPartOf
import com.pinterest.ktlint.ruleset.core.api.nextCodeSibling
import org.jetbrains.kotlin.com.intellij.lang.ASTNode

/**
 * Ensures there are no unnecessary parentheses before a trailing lambda.
 */
public class UnnecessaryParenthesesBeforeTrailingLambdaRule :
    Rule("unnecessary-parentheses-before-trailing-lambda"),
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
