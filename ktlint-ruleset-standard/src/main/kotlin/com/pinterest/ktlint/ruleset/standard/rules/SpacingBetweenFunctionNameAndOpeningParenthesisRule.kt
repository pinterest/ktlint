package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.ElementType.FUN
import com.pinterest.ktlint.rule.engine.core.api.ElementType.IDENTIFIER
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint.Status.EXPERIMENTAL
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint.Status.STABLE
import com.pinterest.ktlint.rule.engine.core.api.ifAutocorrectAllowed
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpace20
import com.pinterest.ktlint.rule.engine.core.api.nextSibling20
import com.pinterest.ktlint.rule.engine.core.api.remove
import com.pinterest.ktlint.ruleset.standard.StandardRule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode

@SinceKtlint("0.46", EXPERIMENTAL)
@SinceKtlint("0.49", STABLE)
public class SpacingBetweenFunctionNameAndOpeningParenthesisRule : StandardRule("spacing-between-function-name-and-opening-parenthesis") {
    override fun beforeVisitChildNodes(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        node
            .takeIf { node.elementType == FUN }
            ?.findChildByType(IDENTIFIER)
            ?.nextSibling20
            ?.takeIf { it.isWhiteSpace20 }
            ?.let { whiteSpace ->
                emit(whiteSpace.startOffset, "Unexpected whitespace", true)
                    .ifAutocorrectAllowed { whiteSpace.remove() }
            }
    }
}

public val SPACING_BETWEEN_FUNCTION_NAME_AND_OPENING_PARENTHESIS_RULE_ID: RuleId =
    SpacingBetweenFunctionNameAndOpeningParenthesisRule().ruleId
