package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.ElementType.CLASS_BODY
import com.pinterest.ktlint.rule.engine.core.api.ElementType.FUN
import com.pinterest.ktlint.rule.engine.core.api.ElementType.LBRACE
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint.Status.EXPERIMENTAL
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint.Status.STABLE
import com.pinterest.ktlint.rule.engine.core.api.ifAutocorrectAllowed
import com.pinterest.ktlint.rule.engine.core.api.isPartOf
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpaceWithNewline20
import com.pinterest.ktlint.rule.engine.core.api.parent
import com.pinterest.ktlint.rule.engine.core.api.prevLeaf
import com.pinterest.ktlint.rule.engine.core.api.replaceTextWith
import com.pinterest.ktlint.ruleset.standard.StandardRule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode

@SinceKtlint("0.34", EXPERIMENTAL)
@SinceKtlint("0.46", STABLE)
public class NoEmptyFirstLineInMethodBlockRule : StandardRule("no-empty-first-line-in-method-block") {
    override fun beforeVisitChildNodes(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        if (node.isWhiteSpaceWithNewline20 &&
            node.prevLeaf?.elementType == LBRACE &&
            node.isPartOf(FUN) &&
            // Allow:
            //     fun fn() = object : Builder {\n\n fun stuff() = Unit }
            node.parent?.elementType != CLASS_BODY
        ) {
            val split = node.text.split("\n")
            if (split.size > 2) {
                emit(
                    node.startOffset + 1,
                    "First line in a method block should not be empty",
                    true,
                ).ifAutocorrectAllowed {
                    node.replaceTextWith("${split.first()}\n${split.last()}")
                }
            }
        }
    }
}

public val NO_EMPTY_FIRST_LINE_IN_METHOD_BLOCK_RULE_ID: RuleId = NoEmptyFirstLineInMethodBlockRule().ruleId
