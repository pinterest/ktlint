package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint.Status.EXPERIMENTAL
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint.Status.STABLE
import com.pinterest.ktlint.rule.engine.core.api.TokenSets
import com.pinterest.ktlint.rule.engine.core.api.ifAutocorrectAllowed
import com.pinterest.ktlint.rule.engine.core.api.isDeclaration20
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpace20
import com.pinterest.ktlint.rule.engine.core.api.prevCodeSibling20
import com.pinterest.ktlint.rule.engine.core.api.prevLeaf
import com.pinterest.ktlint.rule.engine.core.api.prevSibling
import com.pinterest.ktlint.rule.engine.core.api.prevSibling20
import com.pinterest.ktlint.ruleset.standard.StandardRule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafElement

/**
 * @see https://youtrack.jetbrains.com/issue/KT-35088
 */
@SinceKtlint("0.37", EXPERIMENTAL)
@SinceKtlint("0.46", STABLE)
public class SpacingBetweenDeclarationsWithCommentsRule : StandardRule("spacing-between-declarations-with-comments") {
    override fun beforeVisitChildNodes(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        node
            .takeIf { it.elementType in TokenSets.COMMENTS }
            ?.takeUnless { it.isTailComment() }
            ?.treeParent
            ?.takeIf { it.isDeclaration20 }
            ?.takeIf { it.prevCodeSibling20.isDeclaration20 }
            ?.let { visitCommentedDeclaration(it, emit) }
    }

    private fun ASTNode.isTailComment() = startOffset > treeParent.startOffset

    private fun visitCommentedDeclaration(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        node
            .prevSibling20
            ?.takeUnless { it.isBlankLine() }
            ?.let { prevSibling ->
                emit(node.startOffset, "Declarations and declarations with comments should have an empty space between.", true)
                    .ifAutocorrectAllowed {
                        val indent =
                            node
                                .prevLeaf
                                ?.text
                                ?.trim('\n')
                                ?: ""
                        (prevSibling as LeafElement).rawReplaceWithText("\n\n$indent")
                    }
            }
    }

    private fun ASTNode.isBlankLine() = isWhiteSpace20 && text.startsWith("\n\n")
}

public val SPACING_BETWEEN_DECLARATIONS_WITH_COMMENTS_RULE_ID: RuleId = SpacingBetweenDeclarationsWithCommentsRule().ruleId
