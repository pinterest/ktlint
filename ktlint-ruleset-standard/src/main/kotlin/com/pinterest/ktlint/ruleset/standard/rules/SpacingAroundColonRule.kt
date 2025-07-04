package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.ElementType.ANNOTATION
import com.pinterest.ktlint.rule.engine.core.api.ElementType.ANNOTATION_ENTRY
import com.pinterest.ktlint.rule.engine.core.api.ElementType.BLOCK
import com.pinterest.ktlint.rule.engine.core.api.ElementType.CLASS
import com.pinterest.ktlint.rule.engine.core.api.ElementType.COLON
import com.pinterest.ktlint.rule.engine.core.api.ElementType.EQ
import com.pinterest.ktlint.rule.engine.core.api.ElementType.FUN
import com.pinterest.ktlint.rule.engine.core.api.ElementType.OBJECT_DECLARATION
import com.pinterest.ktlint.rule.engine.core.api.ElementType.PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.ElementType.SECONDARY_CONSTRUCTOR
import com.pinterest.ktlint.rule.engine.core.api.ElementType.TYPE_CONSTRAINT
import com.pinterest.ktlint.rule.engine.core.api.ElementType.TYPE_PARAMETER_LIST
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint.Status.STABLE
import com.pinterest.ktlint.rule.engine.core.api.ifAutocorrectAllowed
import com.pinterest.ktlint.rule.engine.core.api.isCode
import com.pinterest.ktlint.rule.engine.core.api.isPartOfComment20
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpace20
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpaceWithNewline20
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpaceWithoutNewline20
import com.pinterest.ktlint.rule.engine.core.api.nextLeaf
import com.pinterest.ktlint.rule.engine.core.api.nextSibling20
import com.pinterest.ktlint.rule.engine.core.api.parent
import com.pinterest.ktlint.rule.engine.core.api.prevLeaf
import com.pinterest.ktlint.rule.engine.core.api.prevSibling20
import com.pinterest.ktlint.rule.engine.core.api.remove
import com.pinterest.ktlint.rule.engine.core.api.replaceTextWith
import com.pinterest.ktlint.rule.engine.core.api.upsertWhitespaceAfterMe
import com.pinterest.ktlint.rule.engine.core.api.upsertWhitespaceBeforeMe
import com.pinterest.ktlint.ruleset.standard.StandardRule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.psi.psiUtil.siblings

@SinceKtlint("0.1", STABLE)
public class SpacingAroundColonRule : StandardRule("colon-spacing") {
    override fun beforeVisitChildNodes(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        if (node.elementType == COLON) {
            removeUnexpectedNewlineBefore(node, emit)
            removeUnexpectedSpacingAround(node, emit)
            addMissingSpacingAround(node, emit)
        }
    }

    private fun removeUnexpectedNewlineBefore(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        node
            .prevLeaf
            ?.takeIf { it.isWhiteSpaceWithNewline20 }
            ?.let { prevLeaf ->
                emit(prevLeaf.startOffset, "Unexpected newline before \":\"", true)
                    .ifAutocorrectAllowed {
                        val parentType = node.parent?.elementType
                        val prevNonCodeElements =
                            node
                                .siblings(forward = false)
                                .takeWhile { !it.isCode }
                                .toList()
                                .reversed()
                        when {
                            parentType == PROPERTY || parentType == FUN -> {
                                node
                                    .siblings(forward = true)
                                    .firstOrNull { it.elementType == EQ }
                                    ?.nextSibling20
                                    ?.let { nextSibling ->
                                        prevNonCodeElements.forEach {
                                            node.parent?.addChild(it, nextSibling)
                                        }
                                        if (nextSibling.isWhiteSpace20) {
                                            nextSibling.remove()
                                        }
                                        Unit
                                    }
                                val blockElement =
                                    node
                                        .siblings(forward = true)
                                        .firstOrNull { it.elementType == BLOCK }
                                if (blockElement != null) {
                                    val before =
                                        blockElement
                                            .firstChildNode
                                            .nextSibling20
                                    prevNonCodeElements
                                        .let {
                                            if (it.first().isWhiteSpace20) {
                                                it.first().remove()
                                                it.drop(1)
                                            }
                                            if (it.last().isWhiteSpaceWithNewline20) {
                                                it.last().remove()
                                                it.dropLast(1)
                                            } else {
                                                it
                                            }
                                        }.forEach {
                                            blockElement.addChild(it, before)
                                        }
                                }
                            }

                            prevLeaf.prevLeaf?.isPartOfComment20 == true -> {
                                val nextLeaf = node.nextLeaf
                                prevNonCodeElements.forEach {
                                    node.parent?.addChild(it, nextLeaf)
                                }
                                if (nextLeaf != null && nextLeaf.isWhiteSpace20) {
                                    nextLeaf.remove()
                                }
                            }

                            else -> {
                                val text = prevLeaf.text
                                if (node.spacingBefore) {
                                    prevLeaf.replaceTextWith(" ")
                                } else {
                                    prevLeaf.remove()
                                }
                                node.upsertWhitespaceAfterMe(text)
                            }
                        }
                    }
            }
    }

    private fun removeUnexpectedSpacingAround(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        if (node.prevSibling20.isWhiteSpaceWithoutNewline20 && node.noSpacingBefore) {
            emit(node.startOffset, "Unexpected spacing before \":\"", true)
                .ifAutocorrectAllowed {
                    node
                        .prevSibling20
                        ?.remove()
                }
        }
        if (node.nextSibling20.isWhiteSpaceWithoutNewline20 && node.spacingAfter) {
            emit(node.startOffset, "Unexpected spacing after \":\"", true)
                .ifAutocorrectAllowed {
                    node
                        .nextSibling20
                        ?.remove()
                }
        }
    }

    private fun addMissingSpacingAround(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        val missingSpacingBefore = !node.prevSibling20.isWhiteSpace20 && node.spacingBefore
        val missingSpacingAfter = !node.nextSibling20.isWhiteSpace20 && node.noSpacingAfter
        when {
            missingSpacingBefore && missingSpacingAfter -> {
                emit(node.startOffset, "Missing spacing around \":\"", true)
                    .ifAutocorrectAllowed {
                        node.upsertWhitespaceBeforeMe(" ")
                        node.upsertWhitespaceAfterMe(" ")
                    }
            }

            missingSpacingBefore -> {
                emit(node.startOffset, "Missing spacing before \":\"", true)
                    .ifAutocorrectAllowed {
                        node.upsertWhitespaceBeforeMe(" ")
                    }
            }

            missingSpacingAfter -> {
                emit(node.startOffset + 1, "Missing spacing after \":\"", true)
                    .ifAutocorrectAllowed {
                        node.upsertWhitespaceAfterMe(" ")
                    }
            }
        }
    }

    private inline val ASTNode.spacingBefore: Boolean
        get() =
            when (parent?.elementType) {

                CLASS, OBJECT_DECLARATION -> {
                    true
                }

                SECONDARY_CONSTRUCTOR -> {
                    // constructor : this/super
                    true
                }

                TYPE_CONSTRAINT -> {
                    // where T : S
                    true
                }

                else -> {
                    parent?.parent?.elementType == TYPE_PARAMETER_LIST
                }
            }

    private inline val ASTNode.noSpacingBefore: Boolean
        get() = !spacingBefore

    private inline val ASTNode.spacingAfter: Boolean
        get() =
            when (parent?.elementType) {
                ANNOTATION, ANNOTATION_ENTRY -> true
                else -> false
            }

    private inline val ASTNode.noSpacingAfter: Boolean
        get() = !spacingAfter
}

public val SPACING_AROUND_COLON_RULE_ID: RuleId = SpacingAroundColonRule().ruleId
