package com.pinterest.ktlint.ruleset.experimental

import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.ast.ElementType
import com.pinterest.ktlint.core.ast.children
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.psi.psiUtil.endOffset

class NoTrailingCommaRule : Rule("no-trailing-comma") {

    override fun visit(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit
    ) {
        if (node.elementType == ElementType.VALUE_ARGUMENT_LIST || node.elementType == ElementType.VALUE_PARAMETER_LIST) {
            val lastNode = node
                .children()
                .filter { it.elementType != ElementType.WHITE_SPACE }
                .filter { it.elementType != ElementType.EOL_COMMENT }
                .filter { it.elementType != ElementType.RPAR }
                .last()
            if (lastNode.elementType == ElementType.COMMA) {
                emit(lastNode.psi.endOffset - 1, "Trailing command in argument list is redundant", true)
                if (autoCorrect) {
                    node.removeChild(lastNode)
                }
            }
        }
    }
}
