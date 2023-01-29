package com.example.ktlint.api.consumer.rules

import com.pinterest.ktlint.ruleset.core.api.Rule
import com.pinterest.ktlint.ruleset.core.api.ElementType
import org.jetbrains.kotlin.com.intellij.lang.ASTNode

public class NoVarRule : Rule("$CUSTOM_RULE_SET_ID:no-var") {
    override fun beforeVisitChildNodes(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        if (node.elementType == ElementType.VAR_KEYWORD) {
            emit(node.startOffset, "Unexpected var, use val instead", false)
        }
    }
}
