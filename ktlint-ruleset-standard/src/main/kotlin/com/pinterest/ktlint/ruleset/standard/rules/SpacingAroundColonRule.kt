package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.rule.engine.core.api.ElementType.COLON
import com.pinterest.ktlint.rule.engine.core.api.ElementType.EQ
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint.Status.STABLE
import com.pinterest.ktlint.rule.engine.core.api.isPartOfComment
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpace
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpaceWithNewline
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpaceWithoutNewline
import com.pinterest.ktlint.rule.engine.core.api.nextLeaf
import com.pinterest.ktlint.rule.engine.core.api.nextSibling
import com.pinterest.ktlint.rule.engine.core.api.prevLeaf
import com.pinterest.ktlint.rule.engine.core.api.prevSibling
import com.pinterest.ktlint.rule.engine.core.api.upsertWhitespaceAfterMe
import com.pinterest.ktlint.rule.engine.core.api.upsertWhitespaceBeforeMe
import com.pinterest.ktlint.ruleset.standard.StandardRule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.psi.KtAnnotation
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtConstructor
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtTypeConstraint
import org.jetbrains.kotlin.psi.KtTypeParameterList
import org.jetbrains.kotlin.psi.psiUtil.siblings
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull

@SinceKtlint("0.1", STABLE)
public class SpacingAroundColonRule : StandardRule("colon-spacing") {
    override fun beforeVisitChildNodes(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        if (node.elementType == COLON) {
            removeUnexpectedNewlineBefore(node, emit, autoCorrect)
            removeUnexpectedSpacingAround(node, emit, autoCorrect)
            addMissingSpacingAround(node, emit, autoCorrect)
        }
    }

    private fun removeUnexpectedNewlineBefore(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
        autoCorrect: Boolean,
    ) {
        val psiParent = node.psi.parent
        val prevLeaf = node.prevLeaf()
        if (prevLeaf != null && prevLeaf.isWhiteSpaceWithNewline()) {
            emit(prevLeaf.startOffset, "Unexpected newline before \":\"", true)
            if (autoCorrect) {
                val prevNonCodeElements =
                    node
                        .siblings(forward = false)
                        .takeWhile { it.isWhiteSpace() || it.isPartOfComment() }
                        .toList()
                        .reversed()
                when {
                    psiParent is KtProperty || psiParent is KtNamedFunction -> {
                        val equalsSignElement =
                            node
                                .siblings(forward = true)
                                .firstOrNull { it.elementType == EQ }
                        if (equalsSignElement != null) {
                            equalsSignElement
                                .treeNext
                                ?.let { treeNext ->
                                    prevNonCodeElements.forEach {
                                        node.treeParent.addChild(it, treeNext)
                                    }
                                    if (treeNext.isWhiteSpace()) {
                                        equalsSignElement.treeParent.removeChild(treeNext)
                                    }
                                    Unit
                                }
                        }
                        val blockElement =
                            node
                                .siblings(forward = true)
                                .firstIsInstanceOrNull<KtBlockExpression>()
                        if (blockElement != null) {
                            val before =
                                blockElement
                                    .firstChildNode
                                    .nextSibling()
                            prevNonCodeElements
                                .let {
                                    if (it.first().isWhiteSpace()) {
                                        blockElement.treeParent.removeChild(it.first())
                                        it.drop(1)
                                    }
                                    if (it.last().isWhiteSpaceWithNewline()) {
                                        blockElement.treeParent.removeChild(it.last())
                                        it.dropLast(1)
                                    } else {
                                        it
                                    }
                                }.forEach {
                                    blockElement.addChild(it, before)
                                }
                        }
                    }

                    prevLeaf.prevLeaf()?.isPartOfComment() == true -> {
                        val nextLeaf = node.nextLeaf()
                        prevNonCodeElements.forEach {
                            node.treeParent.addChild(it, nextLeaf)
                        }
                        if (nextLeaf != null && nextLeaf.isWhiteSpace()) {
                            node.treeParent.removeChild(nextLeaf)
                        }
                    }

                    else -> {
                        val text = prevLeaf.text
                        if (node.spacingBefore) {
                            (prevLeaf as LeafPsiElement).rawReplaceWithText(" ")
                        } else {
                            prevLeaf.treeParent.removeChild(prevLeaf)
                        }
                        node.upsertWhitespaceAfterMe(text)
                    }
                }
            }
        }
    }

    private fun removeUnexpectedSpacingAround(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
        autoCorrect: Boolean,
    ) {
        if (node.prevSibling().isWhiteSpaceWithoutNewline() && node.noSpacingBefore) {
            emit(node.startOffset, "Unexpected spacing before \":\"", true)
            if (autoCorrect) {
                node
                    .prevSibling()
                    ?.let { prevSibling ->
                        prevSibling.treeParent.removeChild(prevSibling)
                    }
            }
        }
        if (node.nextSibling().isWhiteSpaceWithoutNewline() && node.spacingAfter) {
            emit(node.startOffset, "Unexpected spacing after \":\"", true)
            if (autoCorrect) {
                node
                    .nextSibling()
                    ?.let { nextSibling ->
                        nextSibling.treeParent.removeChild(nextSibling)
                    }
            }
        }
    }

    private fun addMissingSpacingAround(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
        autoCorrect: Boolean,
    ) {
        val missingSpacingBefore = !node.prevSibling().isWhiteSpace() && node.spacingBefore
        val missingSpacingAfter = !node.nextSibling().isWhiteSpace() && node.noSpacingAfter
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

    private inline val ASTNode.spacingBefore: Boolean
        get() =
            when {
                psi.parent is KtClassOrObject -> true

                psi.parent is KtConstructor<*> -> {
                    // constructor : this/super
                    true
                }

                psi.parent is KtTypeConstraint -> {
                    // where T : S
                    true
                }

                psi.parent.parent is KtTypeParameterList ->
                    true

                else -> false
            }

    private inline val ASTNode.noSpacingBefore: Boolean
        get() = !spacingBefore

    private inline val ASTNode.spacingAfter: Boolean
        get() =
            when (psi.parent) {
                is KtAnnotation, is KtAnnotationEntry -> true
                else -> false
            }

    private inline val ASTNode.noSpacingAfter: Boolean
        get() = !spacingAfter
}

public val SPACING_AROUND_COLON_RULE_ID: RuleId = SpacingAroundColonRule().ruleId
