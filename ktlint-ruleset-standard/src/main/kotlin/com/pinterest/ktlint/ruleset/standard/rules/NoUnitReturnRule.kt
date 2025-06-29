package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.ElementType.COLON
import com.pinterest.ktlint.rule.engine.core.api.ElementType.FUN
import com.pinterest.ktlint.rule.engine.core.api.ElementType.LBRACE
import com.pinterest.ktlint.rule.engine.core.api.ElementType.TYPE_REFERENCE
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint.Status.STABLE
import com.pinterest.ktlint.rule.engine.core.api.ifAutocorrectAllowed
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpace20
import com.pinterest.ktlint.rule.engine.core.api.nextCodeSibling20
import com.pinterest.ktlint.rule.engine.core.api.nextLeaf
import com.pinterest.ktlint.rule.engine.core.api.parent
import com.pinterest.ktlint.rule.engine.core.api.prevLeaf
import com.pinterest.ktlint.rule.engine.core.api.remove
import com.pinterest.ktlint.ruleset.standard.StandardRule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode

@SinceKtlint("0.7", STABLE)
public class NoUnitReturnRule : StandardRule("no-unit-return") {
    override fun beforeVisitChildNodes(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        if (node.elementType == TYPE_REFERENCE &&
            node.text == "Unit" &&
            node.parent?.elementType == FUN &&
            node.nextCodeSibling20?.firstChildNode?.elementType == LBRACE
        ) {
            emit(node.startOffset, "Unnecessary \"Unit\" return type", true)
                .ifAutocorrectAllowed {
                    node
                        .parent
                        ?.findChildByType(COLON)
                        ?.let { colonNode ->
                            // Remove space after colon when not followed by Unit node
                            node
                                .nextLeaf
                                .takeIf { it.isWhiteSpace20 }
                                ?.takeIf { it.nextLeaf != node }
                                ?.remove()
                            colonNode.remove()
                        }
                    node
                        .prevLeaf
                        .takeIf { it.isWhiteSpace20 }
                        ?.takeIf { it.prevLeaf?.elementType != COLON }
                        ?.remove()
                    node.remove()
                }
        }
    }
}

public val NO_UNIT_RETURN_RULE_ID: RuleId = NoUnitReturnRule().ruleId
