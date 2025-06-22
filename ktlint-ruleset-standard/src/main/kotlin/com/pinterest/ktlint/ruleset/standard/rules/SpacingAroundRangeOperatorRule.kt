package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.ElementType.RANGE
import com.pinterest.ktlint.rule.engine.core.api.ElementType.RANGE_UNTIL
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint.Status.STABLE
import com.pinterest.ktlint.rule.engine.core.api.ifAutocorrectAllowed
import com.pinterest.ktlint.rule.engine.core.api.nextLeaf
import com.pinterest.ktlint.rule.engine.core.api.prevLeaf
import com.pinterest.ktlint.rule.engine.core.api.remove
import com.pinterest.ktlint.ruleset.standard.StandardRule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.lexer.KtSingleValueToken

@SinceKtlint("0.13", STABLE)
public class SpacingAroundRangeOperatorRule : StandardRule("range-spacing") {
    override fun beforeVisitChildNodes(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        if (node.elementType == RANGE || node.elementType == RANGE_UNTIL) {
            val prevLeaf = node.prevLeaf()
            val nextLeaf = node.nextLeaf
            when {
                prevLeaf is PsiWhiteSpace && nextLeaf is PsiWhiteSpace -> {
                    emit(node.startOffset, "Unexpected spacing around \"${node.elementTypeDescription()}\"", true)
                        .ifAutocorrectAllowed {
                            prevLeaf.node.remove()
                            nextLeaf.node.remove()
                        }
                }

                prevLeaf is PsiWhiteSpace -> {
                    emit(prevLeaf.node.startOffset, "Unexpected spacing before \"${node.elementTypeDescription()}\"", true)
                        .ifAutocorrectAllowed { prevLeaf.node.remove() }
                }

                nextLeaf is PsiWhiteSpace -> {
                    emit(nextLeaf.node.startOffset, "Unexpected spacing after \"${node.elementTypeDescription()}\"", true)
                        .ifAutocorrectAllowed { nextLeaf.node.remove() }
                }
            }
        }
    }

    private fun ASTNode.elementTypeDescription() = (elementType as? KtSingleValueToken)?.value ?: elementType
}

public val SPACING_AROUND_RANGE_OPERATOR_RULE_ID: RuleId = SpacingAroundRangeOperatorRule().ruleId
