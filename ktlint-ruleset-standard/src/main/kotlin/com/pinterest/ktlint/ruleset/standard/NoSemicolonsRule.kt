package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.ast.ElementType.KDOC_TEXT
import com.pinterest.ktlint.core.ast.ElementType.OBJECT_KEYWORD
import com.pinterest.ktlint.core.ast.isPartOf
import com.pinterest.ktlint.core.ast.isPartOfString
import com.pinterest.ktlint.core.ast.nextLeaf
import com.pinterest.ktlint.core.ast.prevCodeLeaf
import com.pinterest.ktlint.core.ast.prevLeaf
import com.pinterest.ktlint.core.ast.upsertWhitespaceAfterMe
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.PsiComment
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.kdoc.psi.api.KDoc
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtEnumEntry
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType

class NoSemicolonsRule : Rule("no-semi") {

    override fun visit(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit
    ) {
        if (node.elementType == KDOC_TEXT) {
            return
        }
        if (node is LeafPsiElement && node.textMatches(";") && !node.isPartOfString() &&
            !node.isPartOf(KtEnumEntry::class)
        ) {
            val nextLeaf = node.nextLeaf()
            if (doesNotRequirePreSemi(nextLeaf)) {
                if (node.prevCodeLeaf()?.elementType == OBJECT_KEYWORD) {
                    // https://github.com/shyiko/ktlint/issues/281
                    return
                }
                emit(node.startOffset, "Unnecessary semicolon", true)
                if (autoCorrect) {
                    node.treeParent.removeChild(node)
                }
            } else if (nextLeaf !is PsiWhiteSpace) {
                val prevLeaf = node.prevLeaf()
                if (prevLeaf is PsiWhiteSpace && prevLeaf.textContains('\n')) { // \n;{
                    return
                }
                // todo: move to a separate rule
                emit(node.startOffset + 1, "Missing spacing after \";\"", true)
                if (autoCorrect) {
                    node.upsertWhitespaceAfterMe(" ")
                }
            }
        }
    }

    private fun doesNotRequirePreSemi(nextLeaf: ASTNode?): Boolean {
        if (nextLeaf is PsiWhiteSpace) {
            val nextNextLeaf = nextLeaf.nextLeaf {
                val psi = it.psi
                it !is PsiWhiteSpace && it !is PsiComment && psi.getStrictParentOfType<KDoc>() == null &&
                    psi.getStrictParentOfType<KtAnnotationEntry>() == null
            }
            return (
                nextNextLeaf == null || // \s+ and then eof
                    nextLeaf.textContains('\n') && nextNextLeaf.elementType != KtTokens.LBRACE
                )
        }
        return nextLeaf == null /* eof */
    }
}
