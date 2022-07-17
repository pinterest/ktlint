package com.pinterest.ktlint.ruleset.experimental

import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.ast.ElementType.CALL_EXPRESSION
import com.pinterest.ktlint.core.ast.ElementType.LAMBDA_ARGUMENT
import com.pinterest.ktlint.core.ast.ElementType.LPAR
import com.pinterest.ktlint.core.ast.ElementType.RPAR
import com.pinterest.ktlint.core.ast.ElementType.VALUE_ARGUMENT_LIST
import com.pinterest.ktlint.core.ast.children
import com.pinterest.ktlint.core.ast.isPartOf
import com.pinterest.ktlint.core.ast.nextCodeSibling
import org.jetbrains.kotlin.com.intellij.lang.ASTNode

/**
 * Ensures there are no unnecessary parentheses before a trailing lambda.
 */
class UnnecessaryParenthesesBeforeTrailingLambdaRule : Rule("$experimentalRulesetId:unnecessary-parentheses-before-trailing-lambda") {
    override fun beforeVisitChildNodes(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit
    ) {
        if (node.isPartOf(CALL_EXPRESSION) &&
            node.isEmptyArgumentList() &&
            node.nextCodeSibling()?.elementType == LAMBDA_ARGUMENT
        ) {
            emit(
                node.startOffset,
                "Empty parentheses in function call followed by lambda are unnecessary",
                true
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
