package com.pinterest.ktlint.ruleset.experimental

import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.ast.ElementType
import com.pinterest.ktlint.core.ast.ElementType.FUN_KEYWORD
import com.pinterest.ktlint.core.ast.nextLeaf
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement

/**
 * Lints and formats the spacing after the fun keyword
 */
public class FunKeywordSpacingRule : Rule("$experimentalRulesetId:fun-keyword-spacing") {
    override fun beforeVisitChildNodes(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit
    ) {
        node
            .takeIf { it.elementType == FUN_KEYWORD }
            ?.nextLeaf(includeEmpty = true)
            ?.takeIf { it.elementType == ElementType.WHITE_SPACE && it.text != " " }
            ?.let { whiteSpaceAfterFunKeyword ->
                emit(
                    whiteSpaceAfterFunKeyword.startOffset,
                    "Single space expected after the fun keyword",
                    true
                )
                if (autoCorrect) {
                    (whiteSpaceAfterFunKeyword as LeafPsiElement).rawReplaceWithText(" ")
                }
            }
    }
}
