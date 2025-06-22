package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.ElementType.FUNCTION_TYPE
import com.pinterest.ktlint.rule.engine.core.api.ElementType.MODIFIER_LIST
import com.pinterest.ktlint.rule.engine.core.api.ElementType.WHITE_SPACE
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint.Status.STABLE
import com.pinterest.ktlint.rule.engine.core.api.ifAutocorrectAllowed
import com.pinterest.ktlint.rule.engine.core.api.nextCodeSibling20
import com.pinterest.ktlint.rule.engine.core.api.prevSibling
import com.pinterest.ktlint.rule.engine.core.api.upsertWhitespaceBeforeMe
import com.pinterest.ktlint.ruleset.standard.StandardRule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode

/**
 * Lints and formats a single space between the modifier list and the function type
 */
@SinceKtlint("1.0", STABLE)
public class FunctionTypeModifierSpacingRule : StandardRule("function-type-modifier-spacing") {
    override fun beforeVisitChildNodes(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        node
            .takeIf { it.elementType == MODIFIER_LIST }
            ?.nextCodeSibling20
            ?.takeIf { it.elementType == FUNCTION_TYPE }
            ?.takeUnless { it.isPrecededBySingleSpace() }
            ?.let { functionTypeNode ->
                emit(functionTypeNode.startOffset, "Expected a single space between the modifier list and the function type", true)
                    .ifAutocorrectAllowed {
                        functionTypeNode.upsertWhitespaceBeforeMe(" ")
                    }
            }
    }

    private fun ASTNode.isPrecededBySingleSpace(): Boolean =
        prevSibling()
            ?.let { it.elementType == WHITE_SPACE && it.text == " " }
            ?: false
}

public val FUNCTION_TYPE_MODIFIER_SPACING_RULE: RuleId = FunctionTypeModifierSpacingRule().ruleId
