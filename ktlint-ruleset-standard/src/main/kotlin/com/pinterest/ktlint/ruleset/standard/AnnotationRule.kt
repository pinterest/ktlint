package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.ast.ElementType.FILE_ANNOTATION_LIST
import com.pinterest.ktlint.core.ast.ElementType.MODIFIER_LIST
import com.pinterest.ktlint.core.ast.ElementType.TYPE_ARGUMENT_LIST
import com.pinterest.ktlint.core.ast.ElementType.VALUE_ARGUMENT
import com.pinterest.ktlint.core.ast.ElementType.VALUE_PARAMETER
import com.pinterest.ktlint.core.ast.isPartOf
import com.pinterest.ktlint.core.ast.isPartOfComment
import com.pinterest.ktlint.core.ast.isWhiteSpace
import com.pinterest.ktlint.core.ast.lineNumber
import com.pinterest.ktlint.core.ast.nextSibling
import com.pinterest.ktlint.core.ast.prevSibling
import com.pinterest.ktlint.core.ast.upsertWhitespaceBeforeMe
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.PsiComment
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.PsiWhiteSpaceImpl
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtScript
import org.jetbrains.kotlin.psi.psiUtil.children
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.getNextSiblingIgnoringWhitespaceAndComments
import org.jetbrains.kotlin.psi.psiUtil.nextLeaf

/**
 * Ensures multiple annotations are not on the same line as the annotated declaration. Also ensures that annotations
 * with parameters are placed on separate lines.
 *
 * https://kotlinlang.org/docs/reference/coding-conventions.html#annotation-formatting
 *
 * @see [AnnotationSpacingRule] for white space rules. Moved since
 */
class AnnotationRule : Rule("annotation") {

    companion object {
        const val multipleAnnotationsOnSameLineAsAnnotatedConstructErrorMessage =
            "Multiple annotations should not be placed on the same line as the annotated construct"
        const val annotationsWithParametersAreNotOnSeparateLinesErrorMessage =
            "Annotations with parameters should all be placed on separate lines prior to the annotated construct"
        const val fileAnnotationsShouldBeSeparated =
            "File annotations should be separated from file contents with a blank line"
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
                !node.isPartOf(TYPE_ARGUMENT_LIST) && // val property: Map<@Ann("blah") String, Int>
                annotations.none { it.useSiteTarget?.getAnnotationUseSiteTarget() == AnnotationUseSiteTarget.RECEIVER }
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
            val next = node.nextSibling { it.textLength > 0 }?.let { next ->
                val psi = next.psi
                ((psi as? KtScript)?.blockExpression?.firstChildNode ?: next).nextSibling {
                    !it.isWhiteSpace() && !(it.isPartOfComment() && it.lineNumber() == lineNumber)
                }
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

    private fun doesNotEndWithAComment(whiteSpaces: List<PsiWhiteSpace>): Boolean {
        val lastNode = whiteSpaces.lastOrNull()?.nextLeaf()
        return lastNode !is PsiComment || lastNode.nextLeaf()?.textContains('\n') == false
    }
}
