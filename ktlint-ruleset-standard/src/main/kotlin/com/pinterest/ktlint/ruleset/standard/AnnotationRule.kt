package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.ast.ElementType.MODIFIER_LIST
import com.pinterest.ktlint.core.ast.children
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.psi.KtAnnotationEntry

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
        val root =
            node.children().firstOrNull { it.elementType == MODIFIER_LIST }
                ?: return

        val annotations =
            root.children()
                .mapNotNull { it.psi as? KtAnnotationEntry }
                .toList()
        check(!annotations.isEmpty()) { "Annotations list should not be empty" }

        // Join the nodes that immediately follow the annotations (whitespace), then add the final whitespace
        // if it's not a child of root. This happens when a new line separates the annotations from the annotated
        // construct. In the following example, there are no whitespace children of root, but root's next sibling is the
        // new line whitespace.
        //
        //      @JvmField
        //      val s: Any
        //
        val whiteSpaces = (annotations.asSequence().map { it.nextSibling } + root.treeNext)
            .filterIsInstance<PsiWhiteSpace>()
            .take(annotations.size)
            .toList()

        val multipleAnnotationsOnSameLineAsAnnotatedConstruct =
            annotations.size > 1 && !whiteSpaces.last().textContains('\n')
        val annotationsWithParametersAreNotOnSeparateLines =
            annotations.any { it.valueArgumentList != null } &&
                !whiteSpaces.all { it.textContains('\n') }

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
            val nodeBeforeAnnotations = root.treeParent.treePrev as? PsiWhiteSpace
            // If there is no whitespace before the annotation, the annotation is the first
            // text in the file
            val newLineWithIndent = nodeBeforeAnnotations?.text ?: "\n"

            if (annotationsWithParametersAreNotOnSeparateLines) {
                whiteSpaces.forEach {
                    (it as LeafPsiElement).rawReplaceWithText(newLineWithIndent)
                }
            } else if (multipleAnnotationsOnSameLineAsAnnotatedConstruct) {
                (whiteSpaces.last() as LeafPsiElement).rawReplaceWithText(newLineWithIndent)
            }
        }
    }
}
