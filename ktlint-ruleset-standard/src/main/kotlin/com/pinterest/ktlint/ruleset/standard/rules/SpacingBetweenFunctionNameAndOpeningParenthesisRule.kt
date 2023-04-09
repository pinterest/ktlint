package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.rule.engine.core.api.ElementType
import com.pinterest.ktlint.rule.engine.core.api.ElementType.WHITE_SPACE
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.nextSibling
import com.pinterest.ktlint.ruleset.standard.StandardRule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode

public class SpacingBetweenFunctionNameAndOpeningParenthesisRule : StandardRule("spacing-between-function-name-and-opening-parenthesis") {
    override fun beforeVisitChildNodes(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        node
            .takeIf { node.elementType == ElementType.FUN }
            ?.findChildByType(ElementType.IDENTIFIER)
            ?.nextSibling()
            ?.takeIf { it.elementType == WHITE_SPACE }
            ?.let { whiteSpace ->
                emit(whiteSpace.startOffset, "Unexpected whitespace", true)
                if (autoCorrect) {
                    whiteSpace.treeParent.removeChild(whiteSpace)
                }
            }
    }
}

public val SPACING_BETWEEN_FUNCTION_NAME_AND_OPENING_PARENTHESIS_RULE_ID: RuleId =
    SpacingBetweenFunctionNameAndOpeningParenthesisRule().ruleId
