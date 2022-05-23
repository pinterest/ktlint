package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.core.Rule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode

public class NoBlankLinesInChainedMethodCallsRule : Rule("no-blank-lines-in-chained-method-calls") {
    override fun visit(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit
    ) {
    }
}
