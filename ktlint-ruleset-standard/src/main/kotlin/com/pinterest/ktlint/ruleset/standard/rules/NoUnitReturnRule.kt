package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.ElementType.FUN
import com.pinterest.ktlint.rule.engine.core.api.ElementType.LBRACE
import com.pinterest.ktlint.rule.engine.core.api.ElementType.TYPE_REFERENCE
import com.pinterest.ktlint.rule.engine.core.api.ElementType.VALUE_PARAMETER_LIST
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint.Status.STABLE
import com.pinterest.ktlint.rule.engine.core.api.ifAutocorrectAllowed
import com.pinterest.ktlint.rule.engine.core.api.nextCodeLeaf
import com.pinterest.ktlint.ruleset.standard.StandardRule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode

@SinceKtlint("0.7", STABLE)
public class NoUnitReturnRule : StandardRule("no-unit-return") {
    override fun beforeVisitChildNodes(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        if (node.elementType == TYPE_REFERENCE &&
            node.treeParent.elementType == FUN &&
            node.text == "Unit" &&
            node.nextCodeLeaf(skipSubtree = true)?.elementType == LBRACE
        ) {
            emit(node.startOffset, "Unnecessary \"Unit\" return type", true)
                .ifAutocorrectAllowed {
                    var prevNode = node
                    while (prevNode.treePrev.elementType != VALUE_PARAMETER_LIST) {
                        prevNode = prevNode.treePrev
                    }
                    node.treeParent.removeRange(prevNode, node.treeNext)
                }
        }
    }
}

public val NO_UNIT_RETURN_RULE_ID: RuleId = NoUnitReturnRule().ruleId
