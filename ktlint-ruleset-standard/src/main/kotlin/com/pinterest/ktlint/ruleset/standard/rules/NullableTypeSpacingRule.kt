package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.ruleset.core.api.Rule
import com.pinterest.ktlint.ruleset.core.api.ElementType.QUEST
import com.pinterest.ktlint.ruleset.core.api.ElementType.WHITE_SPACE
import com.pinterest.ktlint.ruleset.core.api.prevLeaf
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement

public class NullableTypeSpacingRule :
    Rule("nullable-type-spacing"),
    Rule.Experimental {
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
