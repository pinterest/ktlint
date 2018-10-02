package com.github.shyiko.ktlint.ruleset.standard

import com.github.shyiko.ktlint.core.Rule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.PsiWhiteSpaceImpl
import org.jetbrains.kotlin.com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.lexer.KtTokens
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
            if (doesNotRequirePreSemi(nextLeaf)) {
                if (node.psi.prevLeafIgnoringWhitespaceAndComments()?.node?.elementType == KtTokens.OBJECT_KEYWORD) {
                    // https://github.com/shyiko/ktlint/issues/281
                    return
                }
                emit(node.startOffset, "Unnecessary semicolon", true)
                if (autoCorrect) {
                    node.treeParent.removeChild(node)
                }
            } else if (nextLeaf !is PsiWhiteSpace) {
                val prevLeaf = PsiTreeUtil.prevLeaf(node, true)
                if (prevLeaf is PsiWhiteSpace && prevLeaf.textContains('\n')) { // \n;{
                    return
                }
                // todo: move to a separate rule
                emit(node.startOffset + 1, "Missing spacing after \";\"", true)
                if (autoCorrect) {
                    node.rawInsertAfterMe(PsiWhiteSpaceImpl(" "))
                }
            }
        }
    }

    private fun doesNotRequirePreSemi(nextLeaf: PsiElement?): Boolean {
        if (nextLeaf is PsiWhiteSpace) {
            val nextNextLeaf = PsiTreeUtil.nextLeaf(nextLeaf, true)
            return (
                nextNextLeaf == null || // \s+ and then eof
                nextLeaf.textContains('\n') && !nextNextLeaf.textMatches("{")
            )
        }
        return nextLeaf == null /* eof */
    }
}
