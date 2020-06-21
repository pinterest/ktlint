package com.pinterest.ktlint.ruleset.experimental

import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.ast.ElementType.FILE_ANNOTATION_LIST
import com.pinterest.ktlint.core.ast.ElementType.MODIFIER_LIST
import com.pinterest.ktlint.core.ast.ElementType.TYPE_ARGUMENT_LIST
import com.pinterest.ktlint.core.ast.ElementType.VALUE_ARGUMENT
import com.pinterest.ktlint.core.ast.ElementType.VALUE_PARAMETER
import com.pinterest.ktlint.core.ast.isPartOf
import com.pinterest.ktlint.core.ast.isPartOfComment
import com.pinterest.ktlint.core.ast.isWhiteSpace
import com.pinterest.ktlint.core.ast.isWhiteSpaceWithNewline
import com.pinterest.ktlint.core.ast.lineNumber
import com.pinterest.ktlint.core.ast.nextLeaf
import com.pinterest.ktlint.core.ast.nextSibling
import com.pinterest.ktlint.core.ast.prevSibling
import com.pinterest.ktlint.core.ast.upsertWhitespaceBeforeMe
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.PsiComment
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.PsiWhiteSpaceImpl
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.psiUtil.children
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.getNextSiblingIgnoringWhitespaceAndComments
import org.jetbrains.kotlin.psi.psiUtil.nextLeaf

/**
 * Ensures multiple annotations are not on the same line as the annotated declaration. Also ensures that annotations
 * with parameters are placed on separate lines.
 *
 * https://kotlinlang.org/docs/reference/coding-conventions.html#annotation-formatting
 */
class AnnotationRule : Rule("annotation") {

    companion object {
        const val multipleAnnotationsOnSameLineAsAnnotatedConstructErrorMessage =
            "Multiple annotations should not be placed on the same line as the annotated construct"
        const val annotationsWithParametersAreNotOnSeparateLinesErrorMessage =
            "Annotations with parameters should all be placed on separate lines prior to the annotated construct"
        const val fileAnnotationsShouldBeSeparated =
            "File annotations should be separated from file contents with a blank line"
        const val fileAnnotationsLineBreaks =
            "There should not be empty lines between an annotation and the object that it's annotating"
    }

    override fun visit(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit
    ) {
        if (node.elementType != MODIFIER_LIST && node.elementType != FILE_ANNOTATION_LIST) {
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

        val noWhiteSpaceAfterAnnotation = node.elementType != FILE_ANNOTATION_LIST &&
            (whiteSpaces.isEmpty() || whiteSpaces.last().nextSibling is KtAnnotationEntry)
        if (noWhiteSpaceAfterAnnotation) {
            emit(
                annotations.last().endOffset - 1,
                "Missing spacing after ${annotations.last().text}",
                true
            )
            if (autoCorrect) {
                (annotations.last().nextLeaf() as? LeafPsiElement)?.upsertWhitespaceBeforeMe(" ")
            }
        }

        val multipleAnnotationsOnSameLineAsAnnotatedConstruct =
            annotations.size > 1 && !whiteSpaces.last().textContains('\n') && doesNotEndWithAComment(whiteSpaces)
        if (multipleAnnotationsOnSameLineAsAnnotatedConstruct) {
            emit(
                annotations.first().node.startOffset,
                multipleAnnotationsOnSameLineAsAnnotatedConstructErrorMessage,
                true
            )
            if (autoCorrect) {
                (whiteSpaces.last() as LeafPsiElement).rawReplaceWithText(getNewlineWithIndent(node))
            }
        }

        val annotationsWithParametersAreNotOnSeparateLines =
            annotations.any { it.valueArgumentList != null } &&
                !whiteSpaces.all { it.textContains('\n') } &&
                doesNotEndWithAComment(whiteSpaces) &&
                node.treeParent.elementType != VALUE_PARAMETER && // fun fn(@Ann("blah") a: String)
                node.treeParent.elementType != VALUE_ARGUMENT && // fn(@Ann("blah") "42")
                !node.isPartOf(TYPE_ARGUMENT_LIST) // val property: Map<@Ann("blah") String, Int>
        if (annotationsWithParametersAreNotOnSeparateLines) {
            emit(
                annotations.first().node.startOffset,
                annotationsWithParametersAreNotOnSeparateLinesErrorMessage,
                true
            )
            if (autoCorrect) {
                whiteSpaces.forEach {
                    (it as LeafPsiElement).rawReplaceWithText(getNewlineWithIndent(node))
                }
            }
        }

        if (node.elementType == FILE_ANNOTATION_LIST) {
            val lineNumber = node.lineNumber()
            val next = node.nextSibling {
                !it.isWhiteSpace() && it.textLength > 0 && !(it.isPartOfComment() && it.lineNumber() == lineNumber)
            }
            val nextLineNumber = next?.lineNumber()
            if (lineNumber != null && nextLineNumber != null) {
                val diff = nextLineNumber - lineNumber
                if (diff < 2) {
                    val psi = node.psi
                    emit(psi.endOffset - 1, fileAnnotationsShouldBeSeparated, true)
                    if (autoCorrect) {
                        if (diff == 0) {
                            psi.getNextSiblingIgnoringWhitespaceAndComments(withItself = false)?.node
                                ?.prevSibling { it.isWhiteSpace() }
                                ?.let { (it as? LeafPsiElement)?.delete() }
                            next.treeParent.addChild(PsiWhiteSpaceImpl("\n"), next)
                        }
                        next.treeParent.addChild(PsiWhiteSpaceImpl("\n"), next)
                    }
                }
            }
        }

        // Check to make sure no trailing line breaks between annotation and object
        val lineNumber = node.lineNumber()
        val next = node.nextSiblingWithAtLeastOneOf(
            {
                !it.isWhiteSpace() &&
                    it.textLength > 0 &&
                    !(it.isPartOfComment() && it.lineNumber() == lineNumber) &&
                    !it.isPartOf(FILE_ANNOTATION_LIST)
            },
            {
                val s = it.text
                // Ensure at least one occurrence of two line breaks
                s.indexOf("\n") != s.lastIndexOf("\n")
            }
        )
        val nextLineNumber = next?.lineNumber()
        if (lineNumber != null && nextLineNumber != null) {
            val diff = nextLineNumber - lineNumber
            // Ensure declaration is not on the same line, there is a line break in between, and it is not an
            // annotation we explicitly want to have a line break between
            if (diff > 1 && node.elementType != FILE_ANNOTATION_LIST) {
                val psi = node.psi
                emit(psi.endOffset - 1, fileAnnotationsLineBreaks, true)
                if (autoCorrect) {
                    removeExtraLineBreaks(node)
                }
            }
        }
        if (whiteSpaces.isNotEmpty() && annotations.size > 1 && node.elementType != FILE_ANNOTATION_LIST) {
            // Check to make sure there are multi breaks between annotations
            if (whiteSpaces.any { psi -> psi.textToCharArray().filter { it == '\n' }.count() > 1 }) {
                val psi = node.psi
                emit(psi.endOffset - 1, fileAnnotationsLineBreaks, true)
                if (autoCorrect) {
                    removeIntraLineBreaks(node, annotations.last())
                }
            }
        }
    }

    private inline fun ASTNode.nextSiblingWithAtLeastOneOf(
        p: (ASTNode) -> Boolean,
        needsToOccur: (ASTNode) -> Boolean
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

    private fun getNewlineWithIndent(modifierListRoot: ASTNode): String {
        val nodeBeforeAnnotations = modifierListRoot.treeParent.treePrev as? PsiWhiteSpace
        // If there is no whitespace before the annotation, the annotation is the first
        // text in the file
        val newLineWithIndent = nodeBeforeAnnotations?.text ?: "\n"
        return if (newLineWithIndent.contains('\n')) {
            // Make sure we only insert a single newline
            newLineWithIndent.substring(newLineWithIndent.lastIndexOf('\n'))
        } else {
            newLineWithIndent
        }
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

    private fun doesNotEndWithAComment(whiteSpaces: List<PsiWhiteSpace>): Boolean {
        val lastNode = whiteSpaces.lastOrNull()?.nextLeaf()
        return lastNode !is PsiComment || lastNode.nextLeaf()?.textContains('\n') == false
    }

    private fun removeIntraLineBreaks(
        node: ASTNode,
        last: KtAnnotationEntry
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
}
