package io.github.ktlint.core.ruleset.standard.rules

import io.github.ktlint.core.rule.engine.core.api.AutocorrectDecision
import io.github.ktlint.core.rule.engine.core.api.ElementType.ANNOTATION_ENTRY
import io.github.ktlint.core.rule.engine.core.api.ElementType.MODIFIER_LIST
import io.github.ktlint.core.rule.engine.core.api.RuleId
import io.github.ktlint.core.rule.engine.core.api.SinceKtlint
import io.github.ktlint.core.rule.engine.core.api.SinceKtlint.Status.EXPERIMENTAL
import io.github.ktlint.core.rule.engine.core.api.SinceKtlint.Status.STABLE
import io.github.ktlint.core.rule.engine.core.api.endOffset
import io.github.ktlint.core.rule.engine.core.api.ifAutocorrectAllowed
import io.github.ktlint.core.rule.engine.core.api.isPartOfComment
import io.github.ktlint.core.rule.engine.core.api.isWhiteSpace
import io.github.ktlint.core.rule.engine.core.api.isWhiteSpaceWithNewline
import io.github.ktlint.core.rule.engine.core.api.nextCodeSibling
import io.github.ktlint.core.rule.engine.core.api.nextLeaf
import io.github.ktlint.core.rule.engine.core.api.nextSibling
import io.github.ktlint.core.rule.engine.core.api.parent
import io.github.ktlint.core.rule.engine.core.api.prevSibling
import io.github.ktlint.core.rule.engine.core.api.remove
import io.github.ktlint.core.rule.engine.core.api.replaceTextWith
import io.github.ktlint.core.ruleset.standard.StandardRule
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
    override fun beforeVisitChildNodes(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        if (node.elementType != MODIFIER_LIST) {
            return
        }

        val annotations =
            node
                .children()
                .filter { it.elementType == ANNOTATION_ENTRY }
                .toList()
                .ifEmpty { return }

        fixBlankLinesBetweenAnnotationsAndAnnotatedConstruct(annotations, node, emit)

        val next =
            node.nextSiblingWithAtLeastOneOf(
                {
                    !it.isWhiteSpace &&
                        it.textLength > 0 &&
                        !it.isCommentOnSameLineAsPrevLeaf()
                },
                {
                    // Disallow multiple white spaces as well as comments
                    if (it.isWhiteSpace) {
                        val s = it.text
                        // Ensure at least one occurrence of two line breaks
                        s.indexOf("\n") != s.lastIndexOf("\n")
                    } else {
                        it.isPartOfComment && !it.isCommentOnSameLineAsPrevLeaf()
                    }
                },
            )
        if (next != null && next.isPartOfComment) {
            emit(node.endOffset, "Annotations should occur immediately before the annotated construct", true)
                .ifAutocorrectAllowed {
                    // Special-case autocorrection when the annotation is separated from the annotated construct
                    // by a comment: we need to swap the order of the comment and the annotation
                    // Remove the annotation and the following whitespace
                    val eolComment = node.nextSibling { it.isCommentOnSameLineAsPrevLeaf() }
                    if (eolComment != null) {
                        eolComment.prevSibling { it.isWhiteSpace }?.remove()
                        eolComment.nextSibling { it.isWhiteSpace }?.remove()
                        eolComment.remove()
                    } else {
                        node.nextSibling { it.isWhiteSpace }?.remove()
                    }
                    node.remove()

                    // Insert the annotation prior to the annotated construct
                    val beforeAnchor = next.nextCodeSibling
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

    private fun fixBlankLinesBetweenAnnotationsAndAnnotatedConstruct(
        annotations: List<ASTNode>,
        node: ASTNode,
        emit: (Int, String, Boolean) -> AutocorrectDecision,
    ) {
        // Join the nodes that immediately follow the annotations (whitespace), then add the final whitespace
        // if it's not a child of root. This happens when a new line separates the annotations from the annotated
        // construct. In the following example, there are no whitespace children of root, but root's next sibling is the
        // new line whitespace.
        //
        //      @JvmField
        //      val s: Any
        //
        val whiteSpaces =
            (annotations.asSequence().map { it.nextSibling } + node.nextSibling)
                .filterNotNull()
                .filter { it.isWhiteSpace }
                .take(annotations.size)
                .toList()
        whiteSpaces
            .filter { it.containsMultipleNewlines() }
            .forEach {
                emit(it.startOffset + 1, "No blank line(s) expected between annotation and annotated construct", true)
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
        var node = this.nextSibling
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
            node = node.nextSibling
        }
        return null
    }

    private fun removeExtraLineBreaks(node: ASTNode) {
        val next =
            node.nextSibling {
                it.isWhiteSpaceWithNewline
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
        if (fromNode.isWhiteSpace) {
            if (fromNode.text.toCharArray().count { it == '\n' } > 1) {
                rawReplaceExtraLineBreaks(fromNode)
            }
        }

        if (nextLeaf != null && !lastAnnotationEntryNode.text.endsWith(nextLeaf.text)) {
            removeIntraLineBreaks(nextLeaf, lastAnnotationEntryNode)
        }
    }

    private fun ASTNode.isCommentOnSameLineAsPrevLeaf() =
        isPartOfComment && leaves(forward = false).takeWhile { it.isWhiteSpace }.none { "\n" in it.text }
}

public val ANNOTATION_SPACING_RULE_ID: RuleId = AnnotationSpacingRule().ruleId
