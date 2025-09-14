package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.ElementType.THEN
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint.Status.STABLE
import com.pinterest.ktlint.rule.engine.core.api.endOffset20
import com.pinterest.ktlint.rule.engine.core.api.ifAutocorrectAllowed
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpace20
import com.pinterest.ktlint.rule.engine.core.api.lastChildLeafOrSelf20
import com.pinterest.ktlint.rule.engine.core.api.nextSibling20
import com.pinterest.ktlint.rule.engine.core.api.prevLeaf
import com.pinterest.ktlint.rule.engine.core.api.prevSibling20
import com.pinterest.ktlint.rule.engine.core.api.upsertWhitespaceAfterMe
import com.pinterest.ktlint.ruleset.standard.StandardRule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode

/**
 * Checks spacing around then block in an if-statement
 */
@SinceKtlint("1.8", STABLE)
public class ThenSpacingRule : StandardRule(id = "then-spacing") {
    override fun beforeVisitChildNodes(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        if (node.elementType == THEN) {
            visitThen(node, emit)
        }
    }

    private fun visitThen(
        node: ASTNode,
        emit: (Int, String, Boolean) -> AutocorrectDecision,
    ) {
        node
            .takeUnless { it.prevSibling20.isWhiteSpace20 }
            ?.let {
                emit(node.startOffset, "Expected a whitespace before 'then' block", true)
                    .ifAutocorrectAllowed { node.prevLeaf?.upsertWhitespaceAfterMe(" ") }
            }
        node
            .takeUnless { it.nextSibling20 == null || it.nextSibling20.isWhiteSpace20 }
            ?.let {
                node
                    .lastChildLeafOrSelf20
                    .let { lastLeafInThen ->
                        emit(lastLeafInThen.endOffset20, "Expected a whitespace after 'then' block", true)
                            .ifAutocorrectAllowed { lastLeafInThen.upsertWhitespaceAfterMe(" ") }
                    }
            }
    }
}

public val THEN_SPACING_RULE_ID: RuleId = ThenSpacingRule().ruleId
