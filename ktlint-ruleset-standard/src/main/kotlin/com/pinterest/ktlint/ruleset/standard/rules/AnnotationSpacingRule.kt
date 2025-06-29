package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.ElementType
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint.Status.EXPERIMENTAL
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint.Status.STABLE
import com.pinterest.ktlint.rule.engine.core.api.endOffset20
import com.pinterest.ktlint.rule.engine.core.api.ifAutocorrectAllowed
import com.pinterest.ktlint.rule.engine.core.api.isPartOf
import com.pinterest.ktlint.rule.engine.core.api.isPartOfComment20
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpace20
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpaceWithNewline20
import com.pinterest.ktlint.rule.engine.core.api.nextCodeSibling20
import com.pinterest.ktlint.rule.engine.core.api.nextLeaf
import com.pinterest.ktlint.rule.engine.core.api.nextSibling
import com.pinterest.ktlint.rule.engine.core.api.nextSibling20
import com.pinterest.ktlint.rule.engine.core.api.parent
import com.pinterest.ktlint.rule.engine.core.api.prevSibling
import com.pinterest.ktlint.rule.engine.core.api.remove
import com.pinterest.ktlint.rule.engine.core.api.replaceTextWith
import com.pinterest.ktlint.ruleset.standard.StandardRule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.PsiWhiteSpaceImpl
import org.jetbrains.kotlin.psi.psiUtil.children
import org.jetbrains.kotlin.psi.psiUtil.leaves

/**
 * Ensures annotations occur immediately prior to the annotated construct
 *
 * https://kotlinlang.org/docs/reference/coding-conventions.html#annotation-formatting
 */
@SinceKtlint("0.39", EXPERIMENTAL)
@SinceKtlint("0.46", STABLE)
public class AnnotationSpacingRule : StandardRule("annotation-spacing") {
    private companion object {
        const val ERROR_MESSAGE = "Annotations should occur immediately before the annotated construct"
    }

    override fun beforeVisitChildNodes(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        if (node.elementType != ElementType.MODIFIER_LIST && node.elementType != ElementType.FILE_ANNOTATION_LIST) {
            return
        }

        val annotations =
            node
                .children()
                .filter { it.elementType == ElementType.ANNOTATION_ENTRY }
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
        val whiteSpaces =
            (annotations.asSequence().map { it.nextSibling20 } + node.nextSibling20)
                .filterNotNull()
                .filter { it.isWhiteSpace20 }
                .take(annotations.size)
                .toList()

        val next =
            node.nextSiblingWithAtLeastOneOf(
                {
                    !it.isWhiteSpace20 &&
                        it.textLength > 0 &&
                        !it.isPartOf(ElementType.FILE_ANNOTATION_LIST) &&
                        !it.isCommentOnSameLineAsPrevLeaf()
                },
                {
                    // Disallow multiple white spaces as well as comments
                    if (it.isWhiteSpace20) {
                        val s = it.text
                        // Ensure at least one occurrence of two line breaks
                        s.indexOf("\n") != s.lastIndexOf("\n")
                    } else {
                        it.isPartOfComment20 && !it.isCommentOnSameLineAsPrevLeaf()
                    }
                },
            )
        if (next != null) {
            if (node.elementType != ElementType.FILE_ANNOTATION_LIST && next.isPartOfComment20) {
                emit(node.endOffset20, ERROR_MESSAGE, true)
                    .ifAutocorrectAllowed {
                        // Special-case autocorrection when the annotation is separated from the annotated construct
                        // by a comment: we need to swap the order of the comment and the annotation
                        // Remove the annotation and the following whitespace
                        val eolComment = node.nextSibling { it.isCommentOnSameLineAsPrevLeaf() }
                        if (eolComment != null) {
                            eolComment.prevSibling { it.isWhiteSpace20 }?.remove()
                            eolComment.nextSibling { it.isWhiteSpace20 }?.remove()
                            eolComment.remove()
                        } else {
                            node.nextSibling { it.isWhiteSpace20 }?.remove()
                        }
                        node.remove()

                        // Insert the annotation prior to the annotated construct
                        val beforeAnchor = next.nextCodeSibling20
                        next
                            .parent!!
                            .apply {
                                addChild(node, beforeAnchor)
                                if (eolComment != null) {
                                    addChild(PsiWhiteSpaceImpl(" "), beforeAnchor)
                                    addChild(eolComment, beforeAnchor)
                                }
                                addChild(PsiWhiteSpaceImpl("\n"), beforeAnchor)
                            }
                    }
            }
        }
        if (node.elementType != ElementType.FILE_ANNOTATION_LIST && whiteSpaces.any { it.containsMultipleNewlines() }) {
            emit(node.endOffset20, ERROR_MESSAGE, true)
                .ifAutocorrectAllowed {
                    removeIntraLineBreaks(node, annotations.last())
                    removeExtraLineBreaks(node)
                }
        }
    }

    private fun ASTNode.containsMultipleNewlines() = text.count { it == '\n' } > 1

    private inline fun ASTNode.nextSiblingWithAtLeastOneOf(
        p: (ASTNode) -> Boolean,
        needsToOccur: (ASTNode) -> Boolean,
    ): ASTNode? {
        var node = this.nextSibling20
        var occurrenceCount = 0
        while (node != null) {
            if (needsToOccur(node)) {
                occurrenceCount++
            }
            if (p(node)) {
                return if (occurrenceCount > 0) {
                    node
                } else {
                    null
                }
            }
            node = node.nextSibling20
        }
        return null
    }

    private fun removeExtraLineBreaks(node: ASTNode) {
        val next =
            node.nextSibling {
                it.isWhiteSpaceWithNewline20
            } as? LeafPsiElement
        if (next != null) {
            rawReplaceExtraLineBreaks(next)
        }
    }

    private fun rawReplaceExtraLineBreaks(node: ASTNode) {
        // Replace the extra white space with a single break
        val text = node.text
        val firstIndex = text.indexOf("\n") + 1
        val replacementText =
            text.substring(0, firstIndex) +
                text.substringAfter("\n").replace("\n", "")

        node.replaceTextWith(replacementText)
    }

    private fun removeIntraLineBreaks(
        fromNode: ASTNode,
        lastAnnotationEntryNode: ASTNode,
    ) {
        // Pull the next before raw replace, or it will blow up
        val nextLeaf = fromNode.nextLeaf
        if (fromNode.isWhiteSpace20) {
            if (fromNode.text.toCharArray().count { it == '\n' } > 1) {
                rawReplaceExtraLineBreaks(fromNode)
            }
        }

        if (nextLeaf != null && !lastAnnotationEntryNode.text.endsWith(nextLeaf.text)) {
            removeIntraLineBreaks(nextLeaf, lastAnnotationEntryNode)
        }
    }

    private fun ASTNode.isCommentOnSameLineAsPrevLeaf() =
        isPartOfComment20 && leaves(forward = false).takeWhile { it.isWhiteSpace20 }.none { "\n" in it.text }
}

public val ANNOTATION_SPACING_RULE_ID: RuleId = AnnotationSpacingRule().ruleId
