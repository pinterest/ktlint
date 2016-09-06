package com.gihub.shyiko.ktlint.ruleset.standard

import com.github.shyiko.ktlint.core.Rule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.PsiComment
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace

class IndentationRule : Rule("indent") {

    override fun visit(node: ASTNode, autoCorrect: Boolean,
            emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit) {
        if (node is PsiWhiteSpace && !node.isPartOf(PsiComment::class)) {
            val split = node.getText().split("\n")
            if (split.size > 1) {
                var offset = node.startOffset + split.first().length + 1
                split.tail().forEach {
                    if (it.length % 4 != 0) {
                        emit(offset, "Unexpected indentation (${it.length})", false)
                    }
                    offset += it.length + 1
                }
            }
        }
    }
}
