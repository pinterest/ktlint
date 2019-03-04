package com.github.shyiko.ktlint.ruleset.standard

import com.github.shyiko.ktlint.core.Rule
import com.github.shyiko.ktlint.core.ast.ElementType.BLOCK
import com.github.shyiko.ktlint.core.ast.ElementType.IDENTIFIER
import com.github.shyiko.ktlint.core.ast.parent
import org.jetbrains.kotlin.com.intellij.lang.ASTNode

class NoItParamInMultilineLambdaRule : Rule("no-it-in-multiline-lambda") {

    override fun visit(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit
    ) {
        // fixme: we are not accounting for "it" variable that can be defined in the same scope
        if (node.elementType == IDENTIFIER && node.text == "it") {
            val block = node.parent(BLOCK)
            if (block != null && block.textContains('\n')) {
                emit(node.startOffset, "Multiline lambda must explicitly name \"it\" parameter", false)
            }
        }
    }
}
