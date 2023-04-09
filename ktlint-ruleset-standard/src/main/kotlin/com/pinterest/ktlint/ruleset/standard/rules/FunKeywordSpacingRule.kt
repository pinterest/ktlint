package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.rule.engine.core.api.ElementType
import com.pinterest.ktlint.rule.engine.core.api.ElementType.FUN_KEYWORD
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.nextLeaf
import com.pinterest.ktlint.ruleset.standard.StandardRule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement

/**
 * Lints and formats the spacing after the fun keyword
 */
public class FunKeywordSpacingRule : StandardRule("fun-keyword-spacing") {
    override fun beforeVisitChildNodes(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        node
            .takeIf { it.elementType == FUN_KEYWORD }
            ?.nextLeaf(includeEmpty = true)
            ?.takeIf { it.elementType == ElementType.WHITE_SPACE && it.text != " " }
            ?.let { whiteSpaceAfterFunKeyword ->
                emit(
                    whiteSpaceAfterFunKeyword.startOffset,
                    "Single space expected after the fun keyword",
                    true,
                )
                if (autoCorrect) {
                    (whiteSpaceAfterFunKeyword as LeafPsiElement).rawReplaceWithText(" ")
                }
            }
    }
}

public val FUN_KEYWORD_SPACING_RULE: RuleId = FunKeywordSpacingRule().ruleId
