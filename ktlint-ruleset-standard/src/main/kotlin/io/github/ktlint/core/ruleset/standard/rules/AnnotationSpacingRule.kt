package io.github.ktlint.core.ruleset.standard.rules

import io.github.ktlint.core.rule.engine.core.api.AutocorrectDecision
import io.github.ktlint.core.rule.engine.core.api.ElementType.ANNOTATION_ENTRY
import io.github.ktlint.core.rule.engine.core.api.ElementType.MODIFIER_LIST
import io.github.ktlint.core.rule.engine.core.api.RuleId
import io.github.ktlint.core.rule.engine.core.api.SinceKtlint
import io.github.ktlint.core.rule.engine.core.api.SinceKtlint.Status.EXPERIMENTAL
import io.github.ktlint.core.rule.engine.core.api.SinceKtlint.Status.STABLE
import io.github.ktlint.core.rule.engine.core.api.ifAutocorrectAllowed
import io.github.ktlint.core.rule.engine.core.api.isCode
import io.github.ktlint.core.rule.engine.core.api.isPartOfComment
import io.github.ktlint.core.rule.engine.core.api.isWhiteSpace
import io.github.ktlint.core.rule.engine.core.api.isWhiteSpaceWithNewline
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
import org.jetbrains.kotlin.psi.psiUtil.siblings

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
        fixCommentsBetweenAnnotationsAndAnnotatedConstruct(node, emit)
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

    private fun fixCommentsBetweenAnnotationsAndAnnotatedConstruct(
        node: ASTNode,
        emit: (Int, String, Boolean) -> AutocorrectDecision,
    ) {
        require(node.elementType == MODIFIER_LIST)
        // Collect all modifiers from the list itself
        val modifiers = node.children()
        val noneCodeElementsBetweenModifierListAndAnnotatedConstruct = node.siblings().takeWhile { !it.isCode }
        (modifiers + noneCodeElementsBetweenModifierListAndAnnotatedConstruct)
            .filter { it.isPartOfComment && !it.isCommentOnSameLineAsPrevLeaf() }
            .toList()
            .forEach { comment ->
                emit(comment.startOffset, "No comment(s) expected between annotation and the annotated construct", true)
                    .ifAutocorrectAllowed {
                        // Remove the comment and whitespace preceding it from the modifier list
                        comment.prevSibling { it.isWhiteSpace }?.remove()
                        comment.remove()

                        // Re-insert the comment and newline prior to the modifier list
                        val beforeAnchor = node
                        node.parent!!
                            .apply {
                                addChild(comment, beforeAnchor)
                                addChild(PsiWhiteSpaceImpl("\n"), beforeAnchor)
                            }
                    }
            }
    }

    private fun ASTNode.containsMultipleNewlines() = text.count { it == '\n' } > 1

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
