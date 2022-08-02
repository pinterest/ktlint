package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.ast.ElementType.ANNOTATION
import com.pinterest.ktlint.core.ast.ElementType.ANNOTATION_ENTRY
import com.pinterest.ktlint.core.ast.ElementType.EQ
import com.pinterest.ktlint.core.ast.isPartOf
import com.pinterest.ktlint.core.ast.isPartOfComment
import com.pinterest.ktlint.core.ast.isPartOfString
import com.pinterest.ktlint.core.ast.isWhiteSpace
import com.pinterest.ktlint.core.ast.isWhiteSpaceWithNewline
import com.pinterest.ktlint.core.ast.nextLeaf
import com.pinterest.ktlint.core.ast.prevLeaf
import com.pinterest.ktlint.core.ast.upsertWhitespaceAfterMe
import com.pinterest.ktlint.core.ast.upsertWhitespaceBeforeMe
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtConstructor
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtTypeConstraint
import org.jetbrains.kotlin.psi.KtTypeParameterList
import org.jetbrains.kotlin.psi.psiUtil.siblings
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull

class SpacingAroundColonRule : Rule("colon-spacing") {

    override fun beforeVisitChildNodes(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit
    ) {
        if (node is LeafPsiElement && node.textMatches(":") && !node.isPartOfString() && !node.isPartOfComment()) {
            if (node.isPartOf(ANNOTATION) || node.isPartOf(ANNOTATION_ENTRY)) {
                // todo: enforce "no spacing"
                return
            }
            val removeSpacingBefore =
                node.parent !is KtClassOrObject &&
                    node.parent !is KtConstructor<*> && // constructor : this/super
                    node.parent !is KtTypeConstraint && // where T : S
                    node.parent?.parent !is KtTypeParameterList
            val prevLeaf = node.prevLeaf()
            if (prevLeaf != null && prevLeaf.isWhiteSpaceWithNewline()) {
                emit(prevLeaf.startOffset, "Unexpected newline before \":\"", true)
                if (autoCorrect) {
                    val parent = node.parent
                    val prevNonCodeElements = node.siblings(forward = false, withItself = false)
                        .takeWhile { it.node.isWhiteSpace() || it.node.isPartOfComment() }.toList()
                    when {
                        parent is KtProperty || parent is KtNamedFunction -> {
                            val equalsSignElement = node.siblings(forward = true, withItself = false)
                                .firstOrNull { it.node.elementType == EQ }
                            if (equalsSignElement != null) {
                                equalsSignElement.nextSibling?.takeIf { it.node.isWhiteSpace() }?.delete()
                                prevNonCodeElements.forEach { parent.addAfter(it, equalsSignElement) }
                            }
                            val blockElement = node.siblings(forward = true, withItself = false)
                                .firstIsInstanceOrNull<KtBlockExpression>()
                            if (blockElement != null) {
                                prevNonCodeElements
                                    .let { if (it.first().node.isWhiteSpace()) it.drop(1) else it }
                                    .forEach { blockElement.addAfter(it, blockElement.lBrace) }
                            }
                            parent.deleteChildRange(prevNonCodeElements.last(), prevNonCodeElements.first())
                        }
                        prevLeaf.prevLeaf()?.isPartOfComment() == true -> {
                            val nextLeaf = node.nextLeaf()
                            prevNonCodeElements.reversed().forEach {
                                node.treeParent.addChild(it.node, nextLeaf)
                            }
                            if (nextLeaf != null && nextLeaf.isWhiteSpace()) {
                                node.treeParent.removeChild(nextLeaf)
                            }
                        }
                        else -> {
                            val text = prevLeaf.text
                            if (removeSpacingBefore) {
                                prevLeaf.treeParent.removeChild(prevLeaf)
                            } else {
                                (prevLeaf as LeafPsiElement).rawReplaceWithText(" ")
                            }
                            node.upsertWhitespaceAfterMe(text)
                        }
                    }
                }
            }
            if (node.prevSibling is PsiWhiteSpace && removeSpacingBefore && !prevLeaf.isWhiteSpaceWithNewline()) {
                emit(node.startOffset, "Unexpected spacing before \":\"", true)
                if (autoCorrect) {
                    node.prevSibling.node.treeParent.removeChild(node.prevSibling.node)
                }
            }
            val missingSpacingBefore =
                node.prevSibling !is PsiWhiteSpace &&
                    (
                        node.parent is KtClassOrObject || node.parent is KtConstructor<*> ||
                            node.parent is KtTypeConstraint || node.parent.parent is KtTypeParameterList
                        )
            val missingSpacingAfter = node.nextSibling !is PsiWhiteSpace
            when {
                missingSpacingBefore && missingSpacingAfter -> {
                    emit(node.startOffset, "Missing spacing around \":\"", true)
                    if (autoCorrect) {
                        node.upsertWhitespaceBeforeMe(" ")
                        node.upsertWhitespaceAfterMe(" ")
                    }
                }
                missingSpacingBefore -> {
                    emit(node.startOffset, "Missing spacing before \":\"", true)
                    if (autoCorrect) {
                        node.upsertWhitespaceBeforeMe(" ")
                    }
                }
                missingSpacingAfter -> {
                    emit(node.startOffset + 1, "Missing spacing after \":\"", true)
                    if (autoCorrect) {
                        node.upsertWhitespaceAfterMe(" ")
                    }
                }
            }
        }
    }
}
