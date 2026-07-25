package io.github.ktlint.core.ruleset.standard.rules

import io.github.ktlint.core.rule.engine.core.api.AutocorrectDecision
import io.github.ktlint.core.rule.engine.core.api.ElementType.ELSE_KEYWORD
import io.github.ktlint.core.rule.engine.core.api.ElementType.IF_KEYWORD
import io.github.ktlint.core.rule.engine.core.api.ElementType.LBRACE
import io.github.ktlint.core.rule.engine.core.api.RuleId
import io.github.ktlint.core.rule.engine.core.api.SinceKtlint
import io.github.ktlint.core.rule.engine.core.api.SinceKtlint.Status.STABLE
import io.github.ktlint.core.rule.engine.core.api.ifAutocorrectAllowed
import io.github.ktlint.core.rule.engine.core.api.isWhiteSpaceWithNewline
import io.github.ktlint.core.rule.engine.core.api.nextLeaf
import io.github.ktlint.core.rule.engine.core.api.prevLeaf
import io.github.ktlint.core.rule.engine.core.api.replaceTextWith
import io.github.ktlint.core.ruleset.standard.StandardRule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode

@SinceKtlint("0.15", STABLE)
public class NoLineBreakAfterElseRule : StandardRule("no-line-break-after-else") {
    override fun beforeVisitChildNodes(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        if (node.isWhiteSpaceWithNewline &&
            node.prevLeaf?.elementType == ELSE_KEYWORD &&
            node.nextLeaf?.elementType.let { it == IF_KEYWORD || it == LBRACE }
        ) {
            emit(node.startOffset + 1, "Unexpected line break after \"else\"", true)
                .ifAutocorrectAllowed { node.replaceTextWith(" ") }
        }
    }
}

public val NO_LINE_BREAK_AFTER_ELSE_RULE_ID: RuleId = NoLineBreakAfterElseRule().ruleId
