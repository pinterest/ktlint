package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.ast.ElementType.ANNOTATION_ENTRY
import com.pinterest.ktlint.core.ast.ElementType.FILE_ANNOTATION_LIST
import com.pinterest.ktlint.core.ast.ElementType.TYPE_ARGUMENT_LIST
import com.pinterest.ktlint.core.ast.ElementType.VALUE_ARGUMENT
import com.pinterest.ktlint.core.ast.ElementType.VALUE_ARGUMENT_LIST
import com.pinterest.ktlint.core.ast.ElementType.VALUE_PARAMETER
import com.pinterest.ktlint.core.ast.ElementType.WHITE_SPACE
import com.pinterest.ktlint.core.ast.children
import com.pinterest.ktlint.core.ast.firstChildLeafOrSelf
import com.pinterest.ktlint.core.ast.isPartOf
import com.pinterest.ktlint.core.ast.isPartOfComment
import com.pinterest.ktlint.core.ast.isWhiteSpace
import com.pinterest.ktlint.core.ast.isWhiteSpaceWithNewline
import com.pinterest.ktlint.core.ast.lastChildLeafOrSelf
import com.pinterest.ktlint.core.ast.lineNumber
import com.pinterest.ktlint.core.ast.nextCodeLeaf
import com.pinterest.ktlint.core.ast.nextLeaf
import com.pinterest.ktlint.core.ast.nextSibling
import com.pinterest.ktlint.core.ast.prevSibling
import com.pinterest.ktlint.core.ast.upsertWhitespaceBeforeMe
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.PsiWhiteSpaceImpl
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtScript
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.getNextSiblingIgnoringWhitespaceAndComments
import org.jetbrains.kotlin.psi.psiUtil.leaves
import org.jetbrains.kotlin.psi.psiUtil.siblings
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

/**
 * Ensures multiple annotations are not on the same line as the annotated declaration. Also ensures that annotations
 * with parameters are placed on separate lines.
 *
 * https://kotlinlang.org/docs/reference/coding-conventions.html#annotation-formatting
 *
 * @see [AnnotationSpacingRule] for white space rules. Moved since
 */
public class AnnotationRule : Rule("annotation") {
    override fun beforeVisitChildNodes(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        when (node.elementType) {
            FILE_ANNOTATION_LIST -> {
                visitFileAnnotationList(node, emit, autoCorrect)
            }
            ANNOTATION_ENTRY ->
                visitAnnotationEntry(node, emit, autoCorrect)
        }
    }

    private fun visitAnnotationEntry(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
        autoCorrect: Boolean,
    ) {
        require(node.elementType == ANNOTATION_ENTRY)

        if (node.isAnnotationEntryWithValueArgumentList() &&
            node.treeParent.treeParent.elementType != VALUE_PARAMETER && // fun fn(@Ann("blah") a: String)
            node.treeParent.treeParent.elementType != VALUE_ARGUMENT && // fn(@Ann("blah") "42")
            !node.isPartOf(TYPE_ARGUMENT_LIST) && // val property: Map<@Ann("blah") String, Int>
            node.isNotReceiverTargetAnnotation()
        ) {
            checkForAnnotationWithParameterToBePlacedOnSeparateLine(node, emit, autoCorrect)
        }

        if ((node.isFollowedByOtherAnnotationEntry() && node.isOnSameLineAsNextAnnotationEntry()) ||
            (node.isPrecededByOtherAnnotationEntry() && node.isOnSameLineAsAnnotatedConstruct())
        ) {
            checkForAnnotationToBePlacedOnSeparateLine(node, emit, autoCorrect)
        }

        if (node.isPrecededByOtherAnnotationEntry() && node.isOnSameLineAsAnnotatedConstruct()) {
            checkForMultipleAnnotationsOnSameLineAsAnnotatedConstruct(node, emit, autoCorrect)
        }
    }

    private fun checkForAnnotationWithParameterToBePlacedOnSeparateLine(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
        autoCorrect: Boolean,
    ) {
        if (node.isPrecededByOtherAnnotationEntry() && node.isOnSameLineAsPreviousAnnotationEntry()) {
            emit(
                node.startOffset,
                "Annotation with parameter(s) should be placed on a separate line prior to the annotated construct",
                true,
            )
            if (autoCorrect) {
                node
                    .firstChildLeafOrSelf()
                    .safeAs<LeafPsiElement>()
                    ?.upsertWhitespaceBeforeMe(" ")
            }
        }

        if (node.isOnSameLineAsNextAnnotationEntryOrAnnotatedConstruct()) {
            emit(
                node.startOffset,
                "Annotation with parameter(s) should be placed on a separate line prior to the annotated construct",
                true,
            )
            if (autoCorrect) {
                node
                    .lastChildLeafOrSelf()
                    .nextLeaf()
                    .safeAs<LeafPsiElement>()
                    ?.let {
                        if (it.elementType == WHITE_SPACE) {
                            it.replaceWithText(getNewlineWithIndent(node.treeParent))
                        } else {
                            it.rawInsertBeforeMe(
                                PsiWhiteSpaceImpl(getNewlineWithIndent(node.treeParent)),
                            )
                        }
                    }
            }
        }
    }

    private fun checkForMultipleAnnotationsOnSameLineAsAnnotatedConstruct(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
        autoCorrect: Boolean,
    ) {
        if (node.isLastAnnotationEntry()) {
            val noAnnotationWithParameters =
                node
                    .siblings(forward = false)
                    .none { it.isAnnotationEntryWithValueArgumentList() }
            if (noAnnotationWithParameters) {
                emit(
                    node.treeParent.startOffset,
                    "Multiple annotations should not be placed on the same line as the annotated construct",
                    true,
                )
                if (autoCorrect) {
                    node
                        .lastChildLeafOrSelf()
                        .nextCodeLeaf()
                        .safeAs<LeafPsiElement>()
                        ?.upsertWhitespaceBeforeMe(getNewlineWithIndent(node.treeParent))
                }
            }
        }
    }

    private fun checkForAnnotationToBePlacedOnSeparateLine(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
        autoCorrect: Boolean,
    ) {
        val isFollowedWithAnnotationHavingValueArgumentList =
            node
                .siblings(forward = true)
                .any { it.isAnnotationEntryWithValueArgumentList() }
        val isPrecededWithAnnotationOnOtherLine =
            node
                .siblings(forward = false)
                .any { it.isWhiteSpaceWithNewline() }
        if (isFollowedWithAnnotationHavingValueArgumentList || isPrecededWithAnnotationOnOtherLine) {
            emit(
                node.startOffset,
                "Annotation must be placed on separate line",
                true,
            )
            if (autoCorrect) {
                node
                    .lastChildLeafOrSelf()
                    .nextLeaf()
                    .safeAs<LeafPsiElement>()
                    ?.let {
                        if (it.elementType == WHITE_SPACE) {
                            it.replaceWithText(getNewlineWithIndent(node.treeParent))
                        } else {
                            it.upsertWhitespaceBeforeMe(
                                getNewlineWithIndent(node.treeParent),
                            )
                        }
                    }
            }
        }
    }

    private fun ASTNode.isNotReceiverTargetAnnotation() =
        getAnnotationUseSiteTarget() != AnnotationUseSiteTarget.RECEIVER

    private fun ASTNode.getAnnotationUseSiteTarget() =
        psi
            .safeAs<KtAnnotationEntry>()
            ?.useSiteTarget
            ?.getAnnotationUseSiteTarget()

    private fun ASTNode.isAnnotationEntryWithValueArgumentList() =
        getAnnotationEntryValueArgumentList() != null

    private fun ASTNode.getAnnotationEntryValueArgumentList() =
        takeIf { it.elementType == ANNOTATION_ENTRY }
            ?.findChildByType(VALUE_ARGUMENT_LIST)

    private fun ASTNode.isLastAnnotationEntry() =
        treeParent
            .children()
            .lastOrNull { it.elementType == ANNOTATION_ENTRY }
            .let { it == this }

    private fun ASTNode.isPrecededByOtherAnnotationEntry() =
        siblings(forward = false).any { it.elementType == ANNOTATION_ENTRY }

    private fun ASTNode.isOnSameLineAsPreviousAnnotationEntry() =
        siblings(forward = false)
            .takeWhile { it.elementType != ANNOTATION_ENTRY }
            .none { it.isWhiteSpaceWithNewline() }

    private fun ASTNode.isFollowedByOtherAnnotationEntry() =
        siblings(forward = true).any { it.elementType == ANNOTATION_ENTRY }

    private fun ASTNode.isOnSameLineAsNextAnnotationEntry() =
        siblings(forward = true)
            .takeWhile { it.elementType != ANNOTATION_ENTRY }
            .none { it.isWhiteSpaceWithNewline() }

    private fun ASTNode.isOnSameLineAsAnnotatedConstruct() =
        lastChildLeafOrSelf()
            .leaves(forward = true)
            .takeWhile { it.isWhiteSpace() || it.isPartOfComment() }
            .none { it.isWhiteSpaceWithNewline() }

    private fun ASTNode.isOnSameLineAsNextAnnotationEntryOrAnnotatedConstruct() =
        if (isFollowedByOtherAnnotationEntry()) {
            isOnSameLineAsNextAnnotationEntry()
        } else {
            isOnSameLineAsAnnotatedConstruct()
        }

    private fun visitFileAnnotationList(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
        autoCorrect: Boolean,
    ) {
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
                emit(
                    psi.endOffset - 1,
                    "File annotations should be separated from file contents with a blank line",
                    true,
                )
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
}
