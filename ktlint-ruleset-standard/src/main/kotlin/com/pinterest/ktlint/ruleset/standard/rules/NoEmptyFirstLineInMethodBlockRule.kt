package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.rule.engine.core.api.ElementType.CLASS_BODY
import com.pinterest.ktlint.rule.engine.core.api.ElementType.FUN
import com.pinterest.ktlint.rule.engine.core.api.ElementType.LBRACE
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint.Status.EXPERIMENTAL
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint.Status.STABLE
import com.pinterest.ktlint.rule.engine.core.api.isPartOf
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpaceWithNewline
import com.pinterest.ktlint.rule.engine.core.api.prevLeaf
import com.pinterest.ktlint.ruleset.standard.StandardRule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement

@SinceKtlint("0.34", EXPERIMENTAL)
@SinceKtlint("0.46", STABLE)
public class NoEmptyFirstLineInMethodBlockRule : StandardRule("no-empty-first-line-in-method-block") {
    override fun beforeVisitChildNodes(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        if (node.isWhiteSpaceWithNewline() &&
            node.prevLeaf()?.elementType == LBRACE &&
            node.isPartOf(FUN) &&
            // Allow:
            //     fun fn() = object : Builder {\n\n fun stuff() = Unit }
            node.treeParent.elementType != CLASS_BODY
        ) {
            val split = node.text.split("\n")
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
