package com.pinterest.ktlint.ruleset.experimental

import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.ast.ElementType.MODIFIER_LIST
import com.pinterest.ktlint.core.ast.children
import com.pinterest.ktlint.core.ast.upsertWhitespaceBeforeMe
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.PsiComment
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.psiUtil.endOffset
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
    }

    override fun visit(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit
    ) {
        val modifierListRoot =
            node.children().firstOrNull { it.elementType == MODIFIER_LIST }
                ?: return

        val annotations =
            modifierListRoot.children()
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
        val whiteSpaces = (annotations.asSequence().map { it.nextSibling } + modifierListRoot.treeNext)
            .filterIsInstance<PsiWhiteSpace>()
            .take(annotations.size)
            .toList()

        val noWhiteSpaceAfterAnnotation = whiteSpaces.isEmpty() || whiteSpaces.last().nextSibling is KtAnnotationEntry
        if (noWhiteSpaceAfterAnnotation) {
            emit(
                annotations.last().endOffset - 1,
                "Missing spacing after ${annotations.last().text}",
                true
            )
        }

        val multipleAnnotationsOnSameLineAsAnnotatedConstruct =
            annotations.size > 1 && !whiteSpaces.last().textContains('\n')
        val annotationsWithParametersAreNotOnSeparateLines =
            annotations.any { it.valueArgumentList != null } &&
                !whiteSpaces.all { it.textContains('\n') } &&
                doesNotEndWithAComment(whiteSpaces)

        if (multipleAnnotationsOnSameLineAsAnnotatedConstruct) {
            emit(
                annotations.first().node.startOffset,
                multipleAnnotationsOnSameLineAsAnnotatedConstructErrorMessage,
                true
            )
        }
        if (annotationsWithParametersAreNotOnSeparateLines) {
            emit(
                annotations.first().node.startOffset,
                annotationsWithParametersAreNotOnSeparateLinesErrorMessage,
                true
            )
        }

        if (autoCorrect) {
            val nodeBeforeAnnotations = modifierListRoot.treeParent.treePrev as? PsiWhiteSpace
            // If there is no whitespace before the annotation, the annotation is the first
            // text in the file
            val newLineWithIndent = (nodeBeforeAnnotations?.text ?: "\n").let {
                // Make sure we only insert a single newline
                if (it.contains('\n')) it.substring(it.lastIndexOf('\n'))
                else it
            }

            if (noWhiteSpaceAfterAnnotation) {
                (annotations.last().nextLeaf() as LeafPsiElement).upsertWhitespaceBeforeMe(" ")
            }
            if (annotationsWithParametersAreNotOnSeparateLines) {
                whiteSpaces.forEach {
                    (it as LeafPsiElement).rawReplaceWithText(newLineWithIndent)
                }
            }
            if (multipleAnnotationsOnSameLineAsAnnotatedConstruct) {
                (whiteSpaces.last() as LeafPsiElement).rawReplaceWithText(newLineWithIndent)
            }
        }
    }

    private fun doesNotEndWithAComment(whiteSpaces: List<PsiWhiteSpace>): Boolean {
        val lastNode = whiteSpaces.lastOrNull()?.nextLeaf()
        return lastNode !is PsiComment || lastNode.nextLeaf()?.textContains('\n') == false
    }
}
