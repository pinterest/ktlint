package io.github.ktlint.core.ruleset.standard.rules

import io.github.ktlint.core.rule.engine.core.api.AutocorrectDecision
import io.github.ktlint.core.rule.engine.core.api.ElementType.RBRACE
import io.github.ktlint.core.rule.engine.core.api.RuleId
import io.github.ktlint.core.rule.engine.core.api.SinceKtlint
import io.github.ktlint.core.rule.engine.core.api.SinceKtlint.Status.STABLE
import io.github.ktlint.core.rule.engine.core.api.ifAutocorrectAllowed
import io.github.ktlint.core.rule.engine.core.api.isWhiteSpaceWithNewline
import io.github.ktlint.core.rule.engine.core.api.nextLeaf
import io.github.ktlint.core.rule.engine.core.api.replaceTextWith
import io.github.ktlint.core.ruleset.standard.StandardRule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode

@SinceKtlint("0.10", STABLE)
public class NoBlankLineBeforeRbraceRule : StandardRule("no-blank-line-before-rbrace") {
    override fun beforeVisitChildNodes(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        if (node.isWhiteSpaceWithNewline &&
            node.nextLeaf?.elementType == RBRACE
        ) {
            val split = node.getText().split("\n")
            if (split.size > 2) {
                emit(
                    node.startOffset + split[0].length + split[1].length + 1,
                    "Unexpected blank line(s) before \"}\"",
                    true,
                ).ifAutocorrectAllowed {
                    node.replaceTextWith("${split.first()}\n${split.last()}")
                }
            }
        }
    }
}

public val NO_BLANK_LINE_BEFORE_RBRACE_RULE_ID: RuleId = NoBlankLineBeforeRbraceRule().ruleId
