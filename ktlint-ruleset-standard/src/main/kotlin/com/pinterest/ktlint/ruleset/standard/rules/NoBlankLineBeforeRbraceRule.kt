package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.ElementType.RBRACE
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint.Status.STABLE
import com.pinterest.ktlint.rule.engine.core.api.ifAutocorrectAllowed
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpaceWithNewline20
import com.pinterest.ktlint.rule.engine.core.api.nextLeaf
import com.pinterest.ktlint.ruleset.standard.StandardRule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement

@SinceKtlint("0.10.2", STABLE)
public class NoBlankLineBeforeRbraceRule : StandardRule("no-blank-line-before-rbrace") {
    override fun beforeVisitChildNodes(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        if (node.isWhiteSpaceWithNewline20 &&
            node.nextLeaf?.elementType == RBRACE
        ) {
            val split = node.getText().split("\n")
            if (split.size > 2) {
                emit(
                    node.startOffset + split[0].length + split[1].length + 1,
                    "Unexpected blank line(s) before \"}\"",
                    true,
                ).ifAutocorrectAllowed {
                    (node as LeafPsiElement).rawReplaceWithText("${split.first()}\n${split.last()}")
                }
            }
        }
    }
}

public val NO_BLANK_LINE_BEFORE_RBRACE_RULE_ID: RuleId = NoBlankLineBeforeRbraceRule().ruleId
