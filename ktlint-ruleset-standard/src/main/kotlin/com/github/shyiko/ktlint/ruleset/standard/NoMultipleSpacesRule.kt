package com.github.shyiko.ktlint.ruleset.standard

import com.github.shyiko.ktlint.core.Rule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.openapi.util.TextRange
import org.jetbrains.kotlin.com.intellij.psi.PsiComment
import org.jetbrains.kotlin.com.intellij.psi.PsiFile
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.diagnostics.DiagnosticUtils
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes

typealias Offset = Int

class NoMultipleSpacesRule : Rule("no-multi-spaces") {

    data class CommentRelativeLocation(
        val prevCommentOffset: Offset,
        val line: Int,
        val column: Int,
        val nextCommentOffset: Offset
    )

    private lateinit var fileNode: ASTNode

    // todo: do not recalculate tree below node.startOffset
    private val commentMap: Map<Offset, CommentRelativeLocation>
        get() {
            val comments = mutableListOf<PsiComment>()
            fileNode.visit { node ->
                val psi = node.psi
                if (psi is PsiComment) { comments.add(psi) }
            }
            return comments.foldIndexed(mutableMapOf()) { i, acc, comment ->
                // todo: get rid of DiagnosticUtils (IndexOutOfBoundsException)
                val pos = DiagnosticUtils.getLineAndColumnInPsiFile(fileNode.psi as PsiFile,
                    TextRange(comment.startOffset, comment.startOffset))
                acc.put(comment.startOffset, CommentRelativeLocation(
                    prevCommentOffset = comments.getOrNull(i - 1)?.startOffset ?: -1,
                    line = pos.line,
                    column = pos.column,
                    nextCommentOffset = comments.getOrNull(i + 1)?.startOffset ?: -1
                ))
                acc
            }
        }

    override fun visit(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit
    ) {
        if (node.elementType == KtStubElementTypes.FILE) {
            fileNode = node
        } else if (node is PsiWhiteSpace && !node.textContains('\n') && node.getTextLength() > 1) {
            val nextLeaf = PsiTreeUtil.nextLeaf(node, true)
            if (nextLeaf is PsiComment) {
                val positionMap = commentMap
                val commentRL = commentMap[nextLeaf.startOffset]!! // NPE here (or anywhere below) would mean that TARFU
                if (commentRL.prevCommentOffset != -1) {
                    val prevCommentRL = positionMap[commentRL.prevCommentOffset]!!
                    if (commentRL.line - 1 == prevCommentRL.line && commentRL.column == prevCommentRL.column) {
                        return
                    }
                }
                if (commentRL.nextCommentOffset != -1) {
                    val nextCommentRL = positionMap[commentRL.nextCommentOffset]!!
                    if (commentRL.line + 1 == nextCommentRL.line && commentRL.column == nextCommentRL.column) {
                        return
                    }
                }
            }
            emit(node.startOffset + 1, "Unnecessary space(s)", true)
            if (autoCorrect) {
                (node as LeafPsiElement).rawReplaceWithText(" ")
            }
        }
    }
}
