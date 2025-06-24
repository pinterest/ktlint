package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.ElementType.DOT_QUALIFIED_EXPRESSION
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint
import com.pinterest.ktlint.rule.engine.core.api.ifAutocorrectAllowed
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpace20
import com.pinterest.ktlint.ruleset.standard.StandardRule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement

@SinceKtlint("0.46", SinceKtlint.Status.STABLE)
public class NoBlankLinesInChainedMethodCallsRule : StandardRule("no-blank-lines-in-chained-method-calls") {
    override fun beforeVisitChildNodes(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        val isBlankLine = node.isWhiteSpace20 && node.text.contains("\n\n")
        if (isBlankLine && node.treeParent.elementType == DOT_QUALIFIED_EXPRESSION) {
            emit(node.startOffset + 1, "Needless blank line(s)", true)
                .ifAutocorrectAllowed {
                    (node as LeafPsiElement).rawReplaceWithText("\n" + node.getText().split("\n\n")[1])
                }
        }
    }
}

public val NO_BLANK_LINES_IN_CHAINED_METHOD_CALLS_RULE_ID: RuleId = NoBlankLinesInChainedMethodCallsRule().ruleId
