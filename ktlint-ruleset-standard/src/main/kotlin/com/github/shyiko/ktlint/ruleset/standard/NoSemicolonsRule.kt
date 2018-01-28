package com.github.shyiko.ktlint.ruleset.standard

import com.github.shyiko.ktlint.core.Rule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.PsiWhiteSpaceImpl
import org.jetbrains.kotlin.com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.psi.KtEnumEntry

class NoSemicolonsRule : Rule("no-semi") {

    override fun visit(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit
    ) {
        if (node is LeafPsiElement && node.textMatches(";") && !node.isPartOfString() &&
                !node.isPartOf(KtEnumEntry::class)) {
            val nextLeaf = PsiTreeUtil.nextLeaf(node, true)
            if (nextLeaf == null /* eof */ ||
                (nextLeaf is PsiWhiteSpace && (nextLeaf.text.contains("\n") ||
                    PsiTreeUtil.nextLeaf(nextLeaf, true) == null /* \s+ and then eof */))
                ) {
                emit(node.startOffset, "Unnecessary semicolon", true)
                if (autoCorrect) {
                    node.treeParent.removeChild(node)
                }
            } else if (nextLeaf !is PsiWhiteSpace) {
                // todo: move to a separate rule
                emit(node.startOffset + 1, "Missing spacing after \";\"", true)
                if (autoCorrect) {
                    node.rawInsertAfterMe(PsiWhiteSpaceImpl(" "))
                }
            }
        }
    }
}
