package com.github.shyiko.ktlint.ruleset.standard

import com.github.shyiko.ktlint.core.Rule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType

class NoItParamInMultilineLambdaRule : Rule("no-it-in-multiline-lambda") {

    override fun visit(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit
    ) {
        // fixme: we are not accounting for "it" variable that can be defined in the same scope
        if (node.elementType == KtTokens.IDENTIFIER && node.text == "it") {
            val block = node.psi.getNonStrictParentOfType(KtBlockExpression::class.java)
            if (block != null && block.textContains('\n')) {
                emit(node.startOffset, "Multiline lambda must explicitly name \"it\" parameter", false)
            }
        }
    }
}
