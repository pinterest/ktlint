package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.ElementType.QUEST
import com.pinterest.ktlint.rule.engine.core.api.ElementType.WHITE_SPACE
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint.Status.EXPERIMENTAL
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint.Status.STABLE
import com.pinterest.ktlint.rule.engine.core.api.ifAutocorrectAllowed
import com.pinterest.ktlint.rule.engine.core.api.prevLeaf
import com.pinterest.ktlint.ruleset.standard.StandardRule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement

@SinceKtlint("0.46", EXPERIMENTAL)
@SinceKtlint("0.49", STABLE)
public class NullableTypeSpacingRule : StandardRule("nullable-type-spacing") {
    override fun beforeVisitChildNodes(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        node
            .takeIf { node.elementType == QUEST }
            ?.prevLeaf()
            ?.takeIf { it.elementType == WHITE_SPACE }
            ?.let { whiteSpaceBeforeQuest ->
                emit(whiteSpaceBeforeQuest.startOffset, "Unexpected whitespace", true)
                    .ifAutocorrectAllowed {
                        (whiteSpaceBeforeQuest as LeafPsiElement).rawRemove()
                    }
            }
    }
}

public val NULLABLE_TYPE_SPACING_RULE_ID: RuleId = NullableTypeSpacingRule().ruleId
