package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.ast.ElementType.CLASS_BODY
import com.pinterest.ktlint.core.ast.ElementType.ENUM_ENTRY
import com.pinterest.ktlint.core.ast.ElementType.OBJECT_KEYWORD
import com.pinterest.ktlint.core.ast.ElementType.SEMICOLON
import com.pinterest.ktlint.core.ast.isWhiteSpace
import com.pinterest.ktlint.core.ast.lastChildLeafOrSelf
import com.pinterest.ktlint.core.ast.nextLeaf
import com.pinterest.ktlint.core.ast.parent
import com.pinterest.ktlint.core.ast.prevCodeLeaf
import com.pinterest.ktlint.core.ast.prevLeaf
import com.pinterest.ktlint.core.ast.upsertWhitespaceAfterMe
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.PsiComment
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.kdoc.psi.api.KDoc
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtDoWhileExpression
import org.jetbrains.kotlin.psi.KtIfExpression
import org.jetbrains.kotlin.psi.KtLoopExpression
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType

public class NoSemicolonsRule : Rule("no-semi") {
    override fun beforeVisitChildNodes(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        if (node.elementType != SEMICOLON) {
            return
        }
        val nextLeaf = node.nextLeaf()
        val prevCodeLeaf = node.prevCodeLeaf()
        if (nextLeaf.doesNotRequirePreSemi() && isNoSemicolonRequiredAfter(node)) {
            emit(node.startOffset, "Unnecessary semicolon", true)
            if (autoCorrect) {
                val prevLeaf = node.prevLeaf(true)
                node.treeParent.removeChild(node)
                if (prevLeaf.isWhiteSpace() && (nextLeaf == null || nextLeaf.isWhiteSpace())) {
                    node.treeParent.removeChild(prevLeaf!!)
                }
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

    private fun ASTNode?.doesNotRequirePreSemi(): Boolean {
        if (this == null) {
            return true
        }
        if (this is PsiWhiteSpace) {
            val nextLeaf = nextLeaf {
                val psi = it.psi
                it !is PsiWhiteSpace && it !is PsiComment && psi.getStrictParentOfType<KDoc>() == null &&
                    psi.getStrictParentOfType<KtAnnotationEntry>() == null
            }
            return (
                nextLeaf == null || // \s+ and then eof
                    textContains('\n') && nextLeaf.elementType != KtTokens.LBRACE
                )
        }
        return false
    }

    private fun isNoSemicolonRequiredAfter(node: ASTNode): Boolean {
        val prevCodeLeaf =
            node.prevCodeLeaf()
                ?: return true
        if (prevCodeLeaf.elementType == OBJECT_KEYWORD) {
            // https://github.com/pinterest/ktlint/issues/281
            return false
        }

        val parent = prevCodeLeaf.treeParent?.psi
        if (parent is KtLoopExpression && parent !is KtDoWhileExpression && parent.body == null) {
            // https://github.com/pinterest/ktlint/issues/955
            return false
        }
        if (parent is KtIfExpression && parent.then == null) {
            return false
        }
        // In case of an enum entry the semicolon (e.g. the node) is a direct child node of enum entry
        if (node.treeParent.elementType == ENUM_ENTRY) {
            return node.isLastCodeLeafBeforeClosingOfClassBody()
        }

        return true
    }

    private fun ASTNode?.isLastCodeLeafBeforeClosingOfClassBody() =
        getLastCodeLeafBeforeClosingOfClassBody() == this

    private fun ASTNode?.getLastCodeLeafBeforeClosingOfClassBody() =
        this
            ?.parent(CLASS_BODY)
            ?.lastChildLeafOrSelf()
            ?.prevCodeLeaf()
}
