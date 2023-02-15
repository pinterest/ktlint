package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.rule.engine.core.api.ElementType
import com.pinterest.ktlint.rule.engine.core.api.ElementType.CLASS_BODY
import com.pinterest.ktlint.rule.engine.core.api.ElementType.FUN
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.isPartOf
import com.pinterest.ktlint.rule.engine.core.api.prevLeaf
import com.pinterest.ktlint.ruleset.standard.StandardRule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement

public class NoEmptyFirstLineInMethodBlockRule : StandardRule("no-empty-first-line-in-method-block") {
    override fun beforeVisitChildNodes(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        if (node is PsiWhiteSpace && node.textContains('\n') &&
            node.prevLeaf()?.elementType == ElementType.LBRACE && node.isPartOf(FUN) &&
            node.treeParent.elementType != CLASS_BODY // fun fn() = object : Builder {\n\n fun stuff() = Unit }
        ) {
            val split = node.getText().split("\n")
            if (split.size > 2) {
                emit(
                    node.startOffset + 1,
                    "First line in a method block should not be empty",
                    true,
                )
                if (autoCorrect) {
                    (node as LeafPsiElement).rawReplaceWithText("${split.first()}\n${split.last()}")
                }
            }
        }
    }
}

public val NO_EMPTY_FIRST_LINE_IN_METHOD_BLOCK_RULE_ID: RuleId = NoEmptyFirstLineInMethodBlockRule().ruleId
