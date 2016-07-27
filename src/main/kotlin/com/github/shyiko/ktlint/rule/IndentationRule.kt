package com.github.shyiko.ktlint.rule

import com.github.shyiko.ktlint.tail
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.PsiComment
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace

class IndentationRule : Rule {

    override fun visit(node: ASTNode, correct: Boolean, emit: (e: RuleViolation) -> Unit) {
        if (node is PsiWhiteSpace && !node.isPartOf(PsiComment::class)) {
            val split = node.getText().split("\n")
            if (split.size > 1) {
                var offset = node.startOffset + split.first().length + 1
                split.tail().forEach {
                    if (it.length % 4 != 0) {
                        emit(RuleViolation(offset, "Unexpected indentation (${it.length})"))
                    }
                    offset += it.length + 1
                }
            }
        }
    }
}
