package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.core.Issue
import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.ast.ElementType.BLOCK
import com.pinterest.ktlint.core.ast.ElementType.IDENTIFIER
import com.pinterest.ktlint.core.ast.parent
import org.jetbrains.kotlin.com.intellij.lang.ASTNode

class NoItParamInMultilineLambdaRule : Rule("no-it-in-multiline-lambda") {

    override fun visit(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (issue: Issue) -> Unit
    ) {
        // fixme: we are not accounting for "it" variable that can be defined in the same scope
        if (node.elementType == IDENTIFIER && node.text == "it") {
            val block = node.parent(BLOCK)
            if (block != null && block.textContains('\n')) {
                emit(Issue(node.startOffset, "Multiline lambda must explicitly name \"it\" parameter", false))
            }
        }
    }
}
