package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.ElementType.THEN
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint.Status.STABLE
import com.pinterest.ktlint.rule.engine.core.api.endOffset
import com.pinterest.ktlint.rule.engine.core.api.ifAutocorrectAllowed
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpace
import com.pinterest.ktlint.rule.engine.core.api.lastChildLeafOrSelf
import com.pinterest.ktlint.rule.engine.core.api.nextSibling
import com.pinterest.ktlint.rule.engine.core.api.prevLeaf
import com.pinterest.ktlint.rule.engine.core.api.prevSibling
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
            .takeUnless { it.prevSibling.isWhiteSpace }
            ?.let {
                emit(node.startOffset, "Expected a whitespace before 'then' block", true)
                    .ifAutocorrectAllowed { node.prevLeaf?.upsertWhitespaceAfterMe(" ") }
            }
        node
            .takeUnless { it.nextSibling == null || it.nextSibling.isWhiteSpace }
            ?.let {
                node
                    .lastChildLeafOrSelf
                    .let { lastLeafInThen ->
                        emit(lastLeafInThen.endOffset, "Expected a whitespace after 'then' block", true)
                            .ifAutocorrectAllowed { lastLeafInThen.upsertWhitespaceAfterMe(" ") }
                    }
            }
    }
}

public val THEN_SPACING_RULE_ID: RuleId = ThenSpacingRule().ruleId
