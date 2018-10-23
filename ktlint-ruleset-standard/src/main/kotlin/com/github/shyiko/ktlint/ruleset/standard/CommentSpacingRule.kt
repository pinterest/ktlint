package com.github.shyiko.ktlint.ruleset.standard

import com.github.shyiko.ktlint.core.Rule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.PsiComment
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.PsiWhiteSpaceImpl
import org.jetbrains.kotlin.com.intellij.psi.util.PsiTreeUtil

class CommentSpacingRule : Rule("comment-spacing") {

    private val allForwardSlashesRegex = Regex("\\/+")

    override fun visit(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit
    ) {
        if (node is PsiComment && node is LeafPsiElement && node.getText().startsWith("//")) {
            val prevLeaf = PsiTreeUtil.prevLeaf(node)
            if (prevLeaf !is PsiWhiteSpace && prevLeaf is LeafPsiElement) {
                emit(node.startOffset, "Missing space before //", true)
                if (autoCorrect) {
                    node.rawInsertBeforeMe(PsiWhiteSpaceImpl(" "))
                }
            }
            val text = node.getText()
            if (text.length != 2 &&
                !text.startsWith("// ") &&
                !text.startsWith("//noinspection") &&
                !text.startsWith("//region") &&
                !text.startsWith("//endregion") &&
                !allForwardSlashesRegex.matches(text)
            ) {
                emit(node.startOffset, "Missing space after //", true)
                if (autoCorrect) {
                    node.rawReplaceWithText("// " + text.removePrefix("//"))
                }
            }
        }
    }
}
