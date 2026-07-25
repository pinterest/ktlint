package io.github.ktlint.core.ruleset.standard.rules

import io.github.ktlint.core.rule.engine.core.api.AutocorrectDecision
import io.github.ktlint.core.rule.engine.core.api.ElementType.THEN
import io.github.ktlint.core.rule.engine.core.api.RuleId
import io.github.ktlint.core.rule.engine.core.api.SinceKtlint
import io.github.ktlint.core.rule.engine.core.api.SinceKtlint.Status.STABLE
import io.github.ktlint.core.rule.engine.core.api.endOffset
import io.github.ktlint.core.rule.engine.core.api.ifAutocorrectAllowed
import io.github.ktlint.core.rule.engine.core.api.isWhiteSpace
import io.github.ktlint.core.rule.engine.core.api.lastChildLeafOrSelf
import io.github.ktlint.core.rule.engine.core.api.nextSibling
import io.github.ktlint.core.rule.engine.core.api.prevLeaf
import io.github.ktlint.core.rule.engine.core.api.prevSibling
import io.github.ktlint.core.rule.engine.core.api.upsertWhitespaceAfterMe
import io.github.ktlint.core.ruleset.standard.StandardRule
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
