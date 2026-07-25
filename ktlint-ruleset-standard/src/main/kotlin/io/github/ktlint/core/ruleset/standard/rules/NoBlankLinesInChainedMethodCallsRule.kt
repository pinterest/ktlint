package io.github.ktlint.core.ruleset.standard.rules

import io.github.ktlint.core.rule.engine.core.api.AutocorrectDecision
import io.github.ktlint.core.rule.engine.core.api.ElementType.DOT_QUALIFIED_EXPRESSION
import io.github.ktlint.core.rule.engine.core.api.RuleId
import io.github.ktlint.core.rule.engine.core.api.SinceKtlint
import io.github.ktlint.core.rule.engine.core.api.ifAutocorrectAllowed
import io.github.ktlint.core.rule.engine.core.api.isWhiteSpace
import io.github.ktlint.core.rule.engine.core.api.parent
import io.github.ktlint.core.rule.engine.core.api.replaceTextWith
import io.github.ktlint.core.ruleset.standard.StandardRule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode

@SinceKtlint("0.46", SinceKtlint.Status.STABLE)
public class NoBlankLinesInChainedMethodCallsRule : StandardRule("no-blank-lines-in-chained-method-calls") {
    override fun beforeVisitChildNodes(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        val isBlankLine = node.isWhiteSpace && node.text.contains("\n\n")
        if (isBlankLine && node.parent?.elementType == DOT_QUALIFIED_EXPRESSION) {
            emit(node.startOffset + 1, "Needless blank line(s)", true)
                .ifAutocorrectAllowed {
                    node.replaceTextWith("\n" + node.getText().split("\n\n")[1])
                }
        }
    }
}

public val NO_BLANK_LINES_IN_CHAINED_METHOD_CALLS_RULE_ID: RuleId = NoBlankLinesInChainedMethodCallsRule().ruleId
