package com.github.shyiko.ktlint.ruleset.standard

import com.github.shyiko.ktlint.core.Rule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.psiUtil.siblings

class NoSpaceBetweenParentheses : Rule("no-space-between-parentheses") {

    override fun visit(node: ASTNode, autoCorrect: Boolean, emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit) {
        if (node.elementType == KtTokens.LPAR) {
            val iterator = node.siblings().iterator()
            var foundEnclosingPar = false
            var hasSpaces = false
            while (iterator.hasNext() && !foundEnclosingPar) {
                val nextNode = iterator.next()
                if (nextNode is PsiWhiteSpace) {
                    hasSpaces = true
                } else if (nextNode.elementType == KtTokens.RPAR) {
                    foundEnclosingPar = true
                } else {
                    // the case when there are parameters or multiline declaration
                    break
                }
            }
            if (hasSpaces && foundEnclosingPar) {
                emit(node.startOffset + 1, "Unexpected space between parentheses", true)
                if (autoCorrect) {
                    val enclosingPar = node.siblings().first { it.elementType == KtTokens.RPAR }
                    node.treeParent.removeRange(node.siblings().first(), enclosingPar)
                }
            }
        }
    }
}
