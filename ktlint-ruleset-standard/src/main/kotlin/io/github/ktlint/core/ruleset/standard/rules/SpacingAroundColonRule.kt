package io.github.ktlint.core.ruleset.standard.rules

import io.github.ktlint.core.rule.engine.core.api.ElementType.ANNOTATION
import io.github.ktlint.core.rule.engine.core.api.ElementType.ANNOTATION_ENTRY
import io.github.ktlint.core.rule.engine.core.api.ElementType.BLOCK
import io.github.ktlint.core.rule.engine.core.api.ElementType.CLASS
import io.github.ktlint.core.rule.engine.core.api.ElementType.COLON
import io.github.ktlint.core.rule.engine.core.api.ElementType.EQ
import io.github.ktlint.core.rule.engine.core.api.ElementType.FUN
import io.github.ktlint.core.rule.engine.core.api.ElementType.OBJECT_DECLARATION
import io.github.ktlint.core.rule.engine.core.api.ElementType.PROPERTY
import io.github.ktlint.core.rule.engine.core.api.ElementType.SECONDARY_CONSTRUCTOR
import io.github.ktlint.core.rule.engine.core.api.ElementType.TYPE_CONSTRAINT
import io.github.ktlint.core.rule.engine.core.api.ElementType.TYPE_PARAMETER_LIST
import io.github.ktlint.core.rule.engine.core.api.RuleId
import io.github.ktlint.core.rule.engine.core.api.SinceKtlint
import io.github.ktlint.core.rule.engine.core.api.SinceKtlint.Status.STABLE
import io.github.ktlint.core.rule.engine.core.api.isCode
import io.github.ktlint.core.rule.engine.core.api.isPartOfComment
import io.github.ktlint.core.rule.engine.core.api.isWhiteSpace
import io.github.ktlint.core.rule.engine.core.api.isWhiteSpaceWithNewline
import io.github.ktlint.core.rule.engine.core.api.isWhiteSpaceWithoutNewline
import io.github.ktlint.core.rule.engine.core.api.nextLeaf
import io.github.ktlint.core.rule.engine.core.api.nextSibling
import io.github.ktlint.core.rule.engine.core.api.parent
import io.github.ktlint.core.rule.engine.core.api.prevLeaf
import io.github.ktlint.core.rule.engine.core.api.prevSibling
import io.github.ktlint.core.rule.engine.core.api.remove
import io.github.ktlint.core.rule.engine.core.api.replaceTextWith
import io.github.ktlint.core.rule.engine.core.api.upsertWhitespaceAfterMe
import io.github.ktlint.core.rule.engine.core.api.upsertWhitespaceBeforeMe
import io.github.ktlint.core.ruleset.standard.StandardRule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.psi.psiUtil.siblings

@SinceKtlint("0.1", STABLE)
public class SpacingAroundColonRule : StandardRule("colon-spacing") {
    override fun beforeVisitChildNodes(node: ASTNode) {
        if (node.elementType == COLON) {
            removeUnexpectedNewlineBefore(node)
            removeUnexpectedSpacingAround(node)
            addMissingSpacingAround(node)
        }
    }

    private fun removeUnexpectedNewlineBefore(node: ASTNode) {
        node
            .prevLeaf
            ?.takeIf { it.isWhiteSpaceWithNewline }
            ?.let { prevLeaf ->
                emit(prevLeaf.startOffset, "Unexpected newline before \":\"") {
                    val parentType = node.parent?.elementType
                    when {
                        parentType == PROPERTY || parentType == FUN -> {
                            removeUnexpectedNewlineBeforeColonInPropertyOrFunctionDeclaration(node)
                        }

                        prevLeaf.prevLeaf?.isPartOfComment == true -> {
                            node.removeUnexpectedNewlineBetweenCommentAndColon()
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

    private fun removeUnexpectedNewlineBeforeColonInPropertyOrFunctionDeclaration(node: ASTNode) {
        node
            .siblings(forward = true)
            .firstOrNull { it.elementType == EQ }
            ?.nextSibling
            ?.let { nextSibling ->
                previousNonCodeSiblings(node).forEach {
                    node.parent?.addChild(it, nextSibling)
                }
                if (nextSibling.isWhiteSpace) {
                    nextSibling.remove()
                }
            }
        val blockElement =
            node
                .siblings(forward = true)
                .firstOrNull { it.elementType == BLOCK }
        if (blockElement != null) {
            val before =
                blockElement
                    .firstChildNode
                    .nextSibling
            previousNonCodeSiblings(node)
                .let {
                    if (it.first().isWhiteSpace) {
                        it.first().remove()
                        it.drop(1)
                    }
                    if (it.last().isWhiteSpaceWithNewline) {
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

    private fun previousNonCodeSiblings(node: ASTNode): List<ASTNode> =
        node
            .siblings(forward = false)
            .takeWhile { !it.isCode }
            .toList()
            .reversed()

    private fun ASTNode.removeUnexpectedNewlineBetweenCommentAndColon() {
        require(elementType == COLON)
        val beforeAnchor = nextLeaf
        previousNonCodeSiblings(this)
            .forEach { parent?.addChild(it, beforeAnchor) }
        beforeAnchor.takeIf { it.isWhiteSpace }?.remove()
    }

    private fun removeUnexpectedSpacingAround(node: ASTNode) {
        if (node.prevSibling.isWhiteSpaceWithoutNewline && node.noSpacingBefore) {
            emit(node.startOffset, "Unexpected spacing before \":\"") {
                node
                    .prevSibling
                    ?.remove()
            }
        }
        if (node.nextSibling.isWhiteSpaceWithoutNewline && node.spacingAfter) {
            emit(node.startOffset, "Unexpected spacing after \":\"") {
                node
                    .nextSibling
                    ?.remove()
            }
        }
    }

    private fun addMissingSpacingAround(node: ASTNode) {
        val missingSpacingBefore = !node.prevSibling.isWhiteSpace && node.spacingBefore
        val missingSpacingAfter = !node.nextSibling.isWhiteSpace && node.noSpacingAfter
        when {
            missingSpacingBefore && missingSpacingAfter -> {
                emit(node.startOffset, "Missing spacing around \":\"") {
                    node.upsertWhitespaceBeforeMe(" ")
                    node.upsertWhitespaceAfterMe(" ")
                }
            }

            missingSpacingBefore -> {
                emit(node.startOffset, "Missing spacing before \":\"") {
                    node.upsertWhitespaceBeforeMe(" ")
                }
            }

            missingSpacingAfter -> {
                emit(node.startOffset + 1, "Missing spacing after \":\"") {
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
