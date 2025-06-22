package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.ElementType.ANNOTATION_ENTRY
import com.pinterest.ktlint.rule.engine.core.api.ElementType.MODIFIER_LIST
import com.pinterest.ktlint.rule.engine.core.api.ElementType.PROPERTY_ACCESSOR
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint.Status.EXPERIMENTAL
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint.Status.STABLE
import com.pinterest.ktlint.rule.engine.core.api.children20
import com.pinterest.ktlint.rule.engine.core.api.ifAutocorrectAllowed
import com.pinterest.ktlint.rule.engine.core.api.indent20
import com.pinterest.ktlint.rule.engine.core.api.isDeclaration
import com.pinterest.ktlint.rule.engine.core.api.isPartOfComment20
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpace20
import com.pinterest.ktlint.rule.engine.core.api.nextLeaf
import com.pinterest.ktlint.rule.engine.core.api.prevCodeLeaf
import com.pinterest.ktlint.rule.engine.core.api.prevCodeSibling
import com.pinterest.ktlint.rule.engine.core.api.upsertWhitespaceBeforeMe
import com.pinterest.ktlint.ruleset.standard.StandardRule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.psi.psiUtil.leaves

/**
 * @see https://youtrack.jetbrains.com/issue/KT-35106
 */
@SinceKtlint("0.37", EXPERIMENTAL)
@SinceKtlint("0.46", STABLE)
public class SpacingBetweenDeclarationsWithAnnotationsRule : StandardRule("spacing-between-declarations-with-annotations") {
    override fun beforeVisitChildNodes(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        if ((node.isDeclarationOrPropertyAccessor()) && node.isAnnotated()) {
            visitDeclaration(node, emit)
        }
    }

    private fun ASTNode.isDeclarationOrPropertyAccessor() = isDeclaration() || elementType == PROPERTY_ACCESSOR

    private fun visitDeclaration(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        node
            .prevCodeSibling()
            ?.takeIf { it.isDeclarationOrPropertyAccessor() }
            ?.takeIf { prevDeclaration -> hasNoBlankLineBetweenDeclarations(node, prevDeclaration) }
            ?.let {
                val prevLeaf = node.prevCodeLeaf()?.nextLeaf { it.isWhiteSpace20 }!!
                emit(
                    prevLeaf.startOffset + 1,
                    "Declarations and declarations with annotations should have an empty space between.",
                    true,
                ).ifAutocorrectAllowed {
                    prevLeaf.upsertWhitespaceBeforeMe("\n".plus(node.indent20))
                }
            }
    }

    private fun ASTNode.isAnnotated(): Boolean =
        findChildByType(MODIFIER_LIST)
            ?.children20
            ?.any { it.elementType == ANNOTATION_ENTRY }
            ?: false

    private fun hasNoBlankLineBetweenDeclarations(
        node: ASTNode,
        prevDeclaration: ASTNode,
    ) = node
        .leaves(false)
        .takeWhile { it.isWhiteSpace20 || it.isPartOfComment20 }
        .takeWhile { it != prevDeclaration }
        .none { it.isBlankLine() }

    private fun ASTNode.isBlankLine() = isWhiteSpace20 && text.count { it == '\n' } > 1
}

public val SPACING_BETWEEN_DECLARATIONS_WITH_ANNOTATIONS_RULE_ID: RuleId = SpacingBetweenDeclarationsWithAnnotationsRule().ruleId
