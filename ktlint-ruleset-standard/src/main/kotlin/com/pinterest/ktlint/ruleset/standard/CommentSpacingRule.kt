package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.ast.prevLeaf
import com.pinterest.ktlint.core.ast.upsertWhitespaceBeforeMe
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.PsiComment
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement

public class CommentSpacingRule : Rule("comment-spacing") {

    override fun beforeVisitChildNodes(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit
    ) {
        if (node is PsiComment && node is LeafPsiElement && node.getText().startsWith("//")) {
            val prevLeaf = node.prevLeaf()
            if (prevLeaf !is PsiWhiteSpace && prevLeaf is LeafPsiElement) {
                emit(node.startOffset, "Missing space before //", true)
                if (autoCorrect) {
                    node.upsertWhitespaceBeforeMe(" ")
                }
            }
            val text = node.getText()
            if (text.length != 2 &&
                !text.startsWith("// ") &&
                !text.startsWith("//noinspection") &&
                !text.startsWith("//region") &&
                !text.startsWith("//endregion") &&
                !text.startsWith("//language=")
            ) {
                emit(node.startOffset, "Missing space after //", true)
                if (autoCorrect) {
                    node.rawReplaceWithText("// " + text.removePrefix("//"))
                }
            }
        }
    }
}
