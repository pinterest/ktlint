package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint.Status.STABLE
import com.pinterest.ktlint.rule.engine.core.api.ifAutocorrectAllowed
import com.pinterest.ktlint.rule.engine.core.api.isCode
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpace20
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpaceWithoutNewline20
import com.pinterest.ktlint.rule.engine.core.api.nextLeaf
import com.pinterest.ktlint.rule.engine.core.api.prevLeaf
import com.pinterest.ktlint.rule.engine.core.api.remove
import com.pinterest.ktlint.ruleset.standard.StandardRule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement

@SinceKtlint("0.30", STABLE)
public class SpacingAroundDotRule : StandardRule("dot-spacing") {
    override fun beforeVisitChildNodes(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        if (node is LeafPsiElement && node.isCode && node.textMatches(".")) {
            node
                .prevLeaf
                .takeIf { it.isWhiteSpaceWithoutNewline20 }
                ?.let { prevLeaf ->
                    emit(prevLeaf.startOffset, "Unexpected spacing before \"${node.text}\"", true)
                        .ifAutocorrectAllowed { prevLeaf.remove() }
                }
            node.nextLeaf
                .takeIf { it.isWhiteSpace20 }
                ?.let { nextLeaf ->
                    emit(nextLeaf.startOffset, "Unexpected spacing after \"${node.text}\"", true)
                        .ifAutocorrectAllowed { nextLeaf.remove() }
                }
        }
    }
}

public val SPACING_AROUND_DOT_RULE_ID: RuleId = SpacingAroundDotRule().ruleId
