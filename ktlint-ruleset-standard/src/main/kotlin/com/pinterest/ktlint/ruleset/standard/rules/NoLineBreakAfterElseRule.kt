package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.ElementType.ELSE_KEYWORD
import com.pinterest.ktlint.rule.engine.core.api.ElementType.IF_KEYWORD
import com.pinterest.ktlint.rule.engine.core.api.ElementType.LBRACE
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint.Status.STABLE
import com.pinterest.ktlint.rule.engine.core.api.ifAutocorrectAllowed
import com.pinterest.ktlint.rule.engine.core.api.nextLeaf
import com.pinterest.ktlint.rule.engine.core.api.prevLeaf
import com.pinterest.ktlint.ruleset.standard.StandardRule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement

@SinceKtlint("0.15", STABLE)
public class NoLineBreakAfterElseRule : StandardRule("no-line-break-after-else") {
    override fun beforeVisitChildNodes(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        if (node is PsiWhiteSpace &&
            node.textContains('\n')
        ) {
            if (node.prevLeaf()?.elementType == ELSE_KEYWORD &&
                node.nextLeaf()?.elementType.let { it == IF_KEYWORD || it == LBRACE }
            ) {
                emit(node.startOffset + 1, "Unexpected line break after \"else\"", true)
                    .ifAutocorrectAllowed {
                        (node as LeafPsiElement).rawReplaceWithText(" ")
                    }
            }
        }
    }
}

public val NO_LINE_BREAK_AFTER_ELSE_RULE_ID: RuleId = NoLineBreakAfterElseRule().ruleId
