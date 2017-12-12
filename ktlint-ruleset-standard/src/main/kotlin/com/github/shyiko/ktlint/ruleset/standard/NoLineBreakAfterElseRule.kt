package com.github.shyiko.ktlint.ruleset.standard

import com.github.shyiko.ktlint.core.Rule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.lexer.KtTokens

class NoLineBreakAfterElseRule : Rule("no-line-break-after-else") {

    override fun visit(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit
    ) {
        if (node is PsiWhiteSpace &&
            node.textContains('\n') &&
            node.prevSibling?.node?.elementType == KtTokens.ELSE_KEYWORD) {
            emit(node.startOffset + 1, "Unexpected line break after \"else\"", true)
            if (autoCorrect) {
                (node as LeafPsiElement).rawReplaceWithText(" ")
            }
        }
    }
}
