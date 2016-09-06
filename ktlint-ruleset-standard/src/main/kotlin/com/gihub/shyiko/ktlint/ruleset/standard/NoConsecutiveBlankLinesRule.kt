package com.gihub.shyiko.ktlint.ruleset.standard

import com.github.shyiko.ktlint.core.Rule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement

class NoConsecutiveBlankLinesRule : Rule("no-consecutive-blank-lines") {

    override fun visit(node: ASTNode, autoCorrect: Boolean,
            emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit) {
        if (node is PsiWhiteSpace) {
            val split = node.getText().split("\n")
            if (split.size > 3) {
                emit(node.startOffset + split[0].length + split[1].length + 2, "Needless blank line(s)", true)
                if (autoCorrect) {
                    (node as LeafPsiElement).replaceWithText("${split.first()}\n\n${split.last()}")
                }
            }
        }
    }

}
