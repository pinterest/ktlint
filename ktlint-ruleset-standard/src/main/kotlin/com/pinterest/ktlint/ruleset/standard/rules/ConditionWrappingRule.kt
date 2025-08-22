package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint.Status.EXPERIMENTAL
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint.Status.STABLE
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.EditorConfig
import com.pinterest.ktlint.ruleset.standard.StandardRule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode

@Deprecated("Will be removed in Ktlint 2.0. This rule is replaced by ExpressionOperandWrappingRule")
@Suppress("RedundantOverride")
@SinceKtlint("1.1", EXPERIMENTAL)
@SinceKtlint("1.3", STABLE)
public class ConditionWrappingRule : StandardRule(id = "condition-wrapping") {
    override fun beforeFirstNode(editorConfig: EditorConfig) {
        super.beforeFirstNode(editorConfig)
    }

    override fun beforeVisitChildNodes(
        node: ASTNode,
        emit: (Int, String, Boolean) -> AutocorrectDecision,
    ) {
        super.beforeVisitChildNodes(node, emit)
    }
}

@Deprecated("Will be removed in Ktlint 2.0. This rule is replaced by ExpressionOperandWrappingRule")
public val CONDITION_WRAPPING_RULE_ID: RuleId = ConditionWrappingRule().ruleId
