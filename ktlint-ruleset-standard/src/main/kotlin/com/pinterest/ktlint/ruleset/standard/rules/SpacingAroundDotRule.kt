package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint.Status.STABLE
import com.pinterest.ktlint.rule.engine.core.api.ifAutocorrectAllowed
import com.pinterest.ktlint.rule.engine.core.api.isPartOfComment
import com.pinterest.ktlint.rule.engine.core.api.isPartOfString20
import com.pinterest.ktlint.rule.engine.core.api.nextLeaf
import com.pinterest.ktlint.rule.engine.core.api.prevLeaf
import com.pinterest.ktlint.rule.engine.core.api.remove
import com.pinterest.ktlint.ruleset.standard.StandardRule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement

@SinceKtlint("0.30", STABLE)
public class SpacingAroundDotRule : StandardRule("dot-spacing") {
    override fun beforeVisitChildNodes(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        if (node is LeafPsiElement && node.textMatches(".") && !node.isPartOfString20 && !node.isPartOfComment()) {
            val prevLeaf = node.prevLeaf()
            if (prevLeaf is PsiWhiteSpace && !prevLeaf.textContains('\n')) {
                emit(prevLeaf.startOffset, "Unexpected spacing before \"${node.text}\"", true)
                    .ifAutocorrectAllowed {
                        prevLeaf.node.remove()
                    }
            }
            val nextLeaf = node.nextLeaf()
            if (nextLeaf is PsiWhiteSpace) {
                emit(nextLeaf.startOffset, "Unexpected spacing after \"${node.text}\"", true)
                    .ifAutocorrectAllowed {
                        nextLeaf.node.remove()
                    }
            }
        }
    }
}

public val SPACING_AROUND_DOT_RULE_ID: RuleId = SpacingAroundDotRule().ruleId
