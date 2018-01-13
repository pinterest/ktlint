package com.github.shyiko.ktlint.ruleset.standard

import com.github.shyiko.ktlint.core.Rule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.lexer.KtTokens

class NoBlankLineBeforeRbraceRule : Rule("no-blank-line-before-rbrace") {

    override fun visit(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit
    ) {
        if (node is PsiWhiteSpace &&
            node.textContains('\n') &&
            PsiTreeUtil.nextLeaf(node, true)?.node?.elementType == KtTokens.RBRACE) {
            val split = node.getText().split("\n")
            if (split.size > 2) {
                emit(node.startOffset + split[0].length + split[1].length + 1,
                    "Unexpected blank line(s) before \"}\"", true)
                if (autoCorrect) {
                    (node as LeafPsiElement).rawReplaceWithText("${split.first()}\n${split.last()}")
                }
            }
        }
    }
}
