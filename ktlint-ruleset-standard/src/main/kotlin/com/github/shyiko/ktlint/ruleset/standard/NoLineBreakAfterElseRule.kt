package com.github.shyiko.ktlint.ruleset.standard

import com.github.shyiko.ktlint.core.Rule
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.lexer.KtKeywordToken

class NoLineBreakAfterElseRule : Rule(RULE_ID) {
    override fun visit(node: ASTNode, autoCorrect: Boolean, emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit) {
        if (node is PsiWhiteSpace) {
            if (
            node.nextSibling?.node?.elementType == KtNodeTypes.ELSE
                && node.prevSibling?.node?.elementType is KtKeywordToken
                && node.prevSibling.text == "else"
                && node.getText().contains("\n")) {
                emit(node.startOffset + 1,
                    "Unexpected line break after \"else\"",
                    true)
                if (autoCorrect) {
                    (node as LeafPsiElement).rawReplaceWithText(" ")
                }
            }
        }
    }

    companion object {
        const val RULE_ID = "no-line-break-after-else"
    }
}
