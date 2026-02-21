package com.example.ktlint.api.consumer.rules

import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.ElementType.VAR_KEYWORD
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.RuleV2
import org.jetbrains.kotlin.com.intellij.lang.ASTNode

public class NoVarRule :
    RuleV2(
        ruleId = RuleId("$CUSTOM_RULE_SET_ID:no-var"),
        about = About(),
    ) {
    override fun beforeVisitChildNodes(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        if (node.elementType == VAR_KEYWORD) {
            emit(node.startOffset, "Unexpected var, use val instead", false)
            // In case that LintError can be autocorrected, use syntax below
            //   .ifAutocorrectAllowed {
            //       // Fix
            //   }
        }
    }
}
