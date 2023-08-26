package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.rule.engine.core.api.ElementType.FUNCTION_TYPE
import com.pinterest.ktlint.rule.engine.core.api.ElementType.MODIFIER_LIST
import com.pinterest.ktlint.rule.engine.core.api.ElementType.WHITE_SPACE
import com.pinterest.ktlint.rule.engine.core.api.Rule
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.nextCodeSibling
import com.pinterest.ktlint.rule.engine.core.api.prevSibling
import com.pinterest.ktlint.rule.engine.core.api.upsertWhitespaceBeforeMe
import com.pinterest.ktlint.ruleset.standard.StandardRule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode

/**
 * Lints and formats a single space between the modifier list and the function type
 */
public class FunctionTypeModifierSpacingRule :
    StandardRule("function-type-modifier-spacing"),
    Rule.Experimental {
    override fun beforeVisitChildNodes(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        if (node.elementType != MODIFIER_LIST) return
        val functionTypeNode = node.nextCodeSibling()?.takeIf { it.elementType == FUNCTION_TYPE } ?: return
        if (functionTypeNode.prevSibling().isSingleSpace()) return

        emit(
            functionTypeNode.startOffset,
            "Expected a single space between the modifier list and the function type",
            true,
        )
        if (autoCorrect) {
            functionTypeNode.upsertWhitespaceBeforeMe(" ")
        }
    }

    private fun ASTNode?.isSingleSpace(): Boolean = this != null && elementType == WHITE_SPACE && text == " "
}

public val FUNCTION_TYPE_MODIFIER_SPACING_RULE: RuleId = FunctionTypeModifierSpacingRule().ruleId
