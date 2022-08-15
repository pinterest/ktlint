package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.ast.ElementType
import com.pinterest.ktlint.core.ast.isPartOf
import com.pinterest.ktlint.core.ast.isPartOfComment
import com.pinterest.ktlint.core.ast.isWhiteSpace
import com.pinterest.ktlint.core.ast.isWhiteSpaceWithNewline
import com.pinterest.ktlint.core.ast.nextCodeSibling
import com.pinterest.ktlint.core.ast.nextLeaf
import com.pinterest.ktlint.core.ast.nextSibling
import com.pinterest.ktlint.core.ast.prevSibling
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.PsiWhiteSpaceImpl
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.psiUtil.children
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.leaves

/**
 * Ensures annotations occur immediately prior to the annotated construct
 *
 * https://kotlinlang.org/docs/reference/coding-conventions.html#annotation-formatting
 */
public class AnnotationSpacingRule : Rule("annotation-spacing") {

    private companion object {
        const val ERROR_MESSAGE = "Annotations should occur immediately before the annotated construct"
    }

    override fun beforeVisitChildNodes(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        if (node.elementType != ElementType.MODIFIER_LIST && node.elementType != ElementType.FILE_ANNOTATION_LIST) {
            return
        }

        val annotations =
            node.children()
                .mapNotNull { it.psi as? KtAnnotationEntry }
                .toList()
        if (annotations.isEmpty()) {
            return
        }

        // Join the nodes that immediately follow the annotations (whitespace), then add the final whitespace
        // if it's not a child of root. This happens when a new line separates the annotations from the annotated
        // construct. In the following example, there are no whitespace children of root, but root's next sibling is the
        // new line whitespace.
        //
        //      @JvmField
        //      val s: Any
        //
        val whiteSpaces = (annotations.asSequence().map { it.nextSibling } + node.treeNext)
            .filterIsInstance<PsiWhiteSpace>()
            .take(annotations.size)
            .toList()

        val next = node.nextSiblingWithAtLeastOneOf(
            {
                !it.isWhiteSpace() &&
                    it.textLength > 0 &&
                    !it.isPartOf(ElementType.FILE_ANNOTATION_LIST) &&
                    !it.isCommentOnSameLineAsPrevLeaf()
            },
            {
                // Disallow multiple white spaces as well as comments
                if (it.psi is PsiWhiteSpace) {
                    val s = it.text
                    // Ensure at least one occurrence of two line breaks
                    s.indexOf("\n") != s.lastIndexOf("\n")
                } else {
                    it.isPartOfComment() && !it.isCommentOnSameLineAsPrevLeaf()
                }
            },
        )
        if (next != null) {
            if (node.elementType != ElementType.FILE_ANNOTATION_LIST) {
                val psi = node.psi
                emit(psi.endOffset - 1, ERROR_MESSAGE, true)
                if (autoCorrect) {
                    // Special-case autocorrection when the annotation is separated from the annotated construct
                    // by a comment: we need to swap the order of the comment and the annotation
                    if (next.isPartOfComment()) {
                        // Remove the annotation and the following whitespace
                        val eolComment = node.nextSibling { it.isCommentOnSameLineAsPrevLeaf() }
                        if (eolComment != null) {
                            eolComment.prevSibling { it.isWhiteSpace() }?.let { it.treeParent.removeChild(it) }
                            eolComment.nextSibling { it.isWhiteSpace() }?.let { it.treeParent.removeChild(it) }
                            eolComment.treeParent?.removeChild(eolComment)
                        } else {
                            node.nextSibling { it.isWhiteSpace() }?.let { it.treeParent?.removeChild(it) }
                        }
                        node.treeParent.removeChild(node)

                        // Insert the annotation prior to the annotated construct
                        val beforeAnchor = next.nextCodeSibling()
                        val treeParent = next.treeParent
                        treeParent.addChild(node, beforeAnchor)
                        if (eolComment != null) {
                            treeParent.addChild(PsiWhiteSpaceImpl(" "), beforeAnchor)
                            treeParent.addChild(eolComment, beforeAnchor)
                        }
                        treeParent.addChild(PsiWhiteSpaceImpl("\n"), beforeAnchor)
                    } else {
                        removeExtraLineBreaks(node)
                    }
                }
            }
        }
        if (whiteSpaces.isNotEmpty() && annotations.size > 1 && node.elementType != ElementType.FILE_ANNOTATION_LIST) {
            // Check to make sure there are multi breaks between annotations
            if (whiteSpaces.any { psi -> psi.textToCharArray().count { it == '\n' } > 1 }) {
                val psi = node.psi
                emit(psi.endOffset - 1, ERROR_MESSAGE, true)
                if (autoCorrect) {
                    removeIntraLineBreaks(node, annotations.last())
                }
            }
        }
    }

    private inline fun ASTNode.nextSiblingWithAtLeastOneOf(
        p: (ASTNode) -> Boolean,
        needsToOccur: (ASTNode) -> Boolean,
    ): ASTNode? {
        var n = this.treeNext
        var occurrenceCount = 0
        while (n != null) {
            if (needsToOccur(n)) {
                occurrenceCount++
            }
            if (p(n)) {
                return if (occurrenceCount > 0) {
                    n
                } else {
                    null
                }
            }
            n = n.treeNext
        }
        return null
    }

    private fun removeExtraLineBreaks(node: ASTNode) {
        val next = node.nextSibling {
            it.isWhiteSpaceWithNewline()
        } as? LeafPsiElement
        if (next != null) {
            rawReplaceExtraLineBreaks(next)
        }
    }

    private fun rawReplaceExtraLineBreaks(leaf: LeafPsiElement) {
        // Replace the extra white space with a single break
        val text = leaf.text
        val firstIndex = text.indexOf("\n") + 1
        val replacementText = text.substring(0, firstIndex) +
            text.substringAfter("\n").replace("\n", "")

        leaf.rawReplaceWithText(replacementText)
    }

    private fun removeIntraLineBreaks(
        node: ASTNode,
        last: KtAnnotationEntry,
    ) {
        val txt = node.text
        // Pull the next before raw replace or it will blow up
        val lNext = node.nextLeaf()
        if (node is PsiWhiteSpaceImpl) {
            if (txt.toCharArray().count { it == '\n' } > 1) {
                rawReplaceExtraLineBreaks(node)
            }
        }

        if (lNext != null && !last.text.endsWith(lNext.text)) {
            removeIntraLineBreaks(lNext, last)
        }
    }

    private fun ASTNode.isCommentOnSameLineAsPrevLeaf() =
        isPartOfComment() && leaves(forward = false).takeWhile { it.isWhiteSpace() }.none { "\n" in it.text }
}
