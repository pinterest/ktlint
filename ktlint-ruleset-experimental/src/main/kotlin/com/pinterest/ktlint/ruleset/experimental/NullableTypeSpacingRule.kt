package com.pinterest.ktlint.ruleset.experimental

import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.ast.ElementType.QUEST
import com.pinterest.ktlint.core.ast.ElementType.WHITE_SPACE
import com.pinterest.ktlint.core.ast.prevLeaf
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement

public class NullableTypeSpacingRule : Rule("$experimentalRulesetId:nullable-type-spacing") {
    override fun beforeVisitChildNodes(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit
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
