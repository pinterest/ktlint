package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.rule.engine.core.api.ElementType.QUEST
import com.pinterest.ktlint.rule.engine.core.api.ElementType.WHITE_SPACE
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.prevLeaf
import com.pinterest.ktlint.ruleset.standard.StandardRule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement

public class NullableTypeSpacingRule : StandardRule("nullable-type-spacing") {
    override fun beforeVisitChildNodes(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        node
            .takeIf { node.elementType == QUEST }
            ?.prevLeaf()
            ?.takeIf { it.elementType == WHITE_SPACE }
            ?.let { whiteSpaceBeforeQuest ->
                emit(whiteSpaceBeforeQuest.startOffset, "Unexpected whitespace", true)
                if (autoCorrect) {
                    (whiteSpaceBeforeQuest as LeafPsiElement).rawRemove()
                }
            }
    }
}

public val NULLABLE_TYPE_SPACING_RULE_ID: RuleId = NullableTypeSpacingRule().ruleId
