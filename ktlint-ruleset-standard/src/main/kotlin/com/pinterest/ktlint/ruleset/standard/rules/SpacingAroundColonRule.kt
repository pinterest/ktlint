package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.rule.engine.core.api.ElementType.ANNOTATION
import com.pinterest.ktlint.rule.engine.core.api.ElementType.ANNOTATION_ENTRY
import com.pinterest.ktlint.rule.engine.core.api.ElementType.COLON
import com.pinterest.ktlint.rule.engine.core.api.ElementType.EQ
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.isPartOf
import com.pinterest.ktlint.rule.engine.core.api.isPartOfComment
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpace
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpaceWithNewline
import com.pinterest.ktlint.rule.engine.core.api.nextLeaf
import com.pinterest.ktlint.rule.engine.core.api.nextSibling
import com.pinterest.ktlint.rule.engine.core.api.prevLeaf
import com.pinterest.ktlint.rule.engine.core.api.prevSibling
import com.pinterest.ktlint.rule.engine.core.api.upsertWhitespaceAfterMe
import com.pinterest.ktlint.rule.engine.core.api.upsertWhitespaceBeforeMe
import com.pinterest.ktlint.ruleset.standard.StandardRule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
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

public class SpacingAroundColonRule : StandardRule("colon-spacing") {
    override fun beforeVisitChildNodes(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        if (node.elementType == COLON) {
            val psiParent = node.psi.parent
            if (node.isPartOf(ANNOTATION) || node.isPartOf(ANNOTATION_ENTRY)) {
                // TODO: https://github.com/pinterest/ktlint/issues/2093 Enforce no spacing
                return
            }
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
                            if (node.removeSpacingBefore) {
                                prevLeaf.treeParent.removeChild(prevLeaf)
                            } else {
                                (prevLeaf as LeafPsiElement).rawReplaceWithText(" ")
                            }
                            node.upsertWhitespaceAfterMe(text)
                        }
                    }
                }
            }
            if (node.prevSibling().isWhiteSpace() && node.removeSpacingBefore && !prevLeaf.isWhiteSpaceWithNewline()) {
                emit(node.startOffset, "Unexpected spacing before \":\"", true)
                if (autoCorrect) {
                    node
                        .prevSibling()
                        ?.let { prevSibling ->
                            prevSibling.treeParent.removeChild(prevSibling)
                        }
                }
            }
            val missingSpacingBefore =
                !node.prevSibling().isWhiteSpace() &&
                    (
                        psiParent is KtClassOrObject || psiParent is KtConstructor<*> ||
                            psiParent is KtTypeConstraint || psiParent.parent is KtTypeParameterList
                    )
            val missingSpacingAfter = !node.nextSibling().isWhiteSpace()
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

    private inline val ASTNode.removeSpacingBefore: Boolean
        get() =
            psi
                .parent
                .let { psiParent ->
                    psiParent !is KtClassOrObject &&
                        psiParent !is KtConstructor<*> && // constructor : this/super
                        psiParent !is KtTypeConstraint && // where T : S
                        psiParent?.parent !is KtTypeParameterList
                }
}

public val SPACING_AROUND_COLON_RULE_ID: RuleId = SpacingAroundColonRule().ruleId
