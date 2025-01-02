package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint.Status.EXPERIMENTAL
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint.Status.STABLE
import com.pinterest.ktlint.rule.engine.core.api.TokenSets
import com.pinterest.ktlint.rule.engine.core.api.ifAutocorrectAllowed
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpace
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpaceWithNewline
import com.pinterest.ktlint.rule.engine.core.api.prevCodeSibling
import com.pinterest.ktlint.rule.engine.core.api.prevLeaf
import com.pinterest.ktlint.rule.engine.core.api.prevSibling
import com.pinterest.ktlint.ruleset.standard.StandardRule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafElement
import org.jetbrains.kotlin.psi.stubs.elements.KtTokenSets

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
            .takeIf { node.prevLeaf().isWhiteSpaceWithNewline() }
            .takeUnless { node.startOffset > (node.treeParent?.startOffset ?: node.startOffset) }
            ?.takeIf { it.treeParent.isDeclaration() }
            ?.takeIf { it.treeParent.prevCodeSibling().isDeclaration() }
            ?.let { visitCommentBeforeDeclaration(node, emit) }
    }

    private fun visitCommentBeforeDeclaration(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        node
            .treeParent
            .prevSibling()
            .takeIf { it.isWhiteSpace() && it.text.count { it == '\n' } < 2 }
            ?.let { whiteSpace ->
                emit(
                    node.startOffset,
                    "Declarations and declarations with comments should have an empty space between.",
                    true,
                ).ifAutocorrectAllowed {
                    val indent = node.prevLeaf()?.text?.trim('\n') ?: ""
                    (whiteSpace as LeafElement).rawReplaceWithText("\n\n$indent")
                }
            }
    }

    private fun ASTNode?.isDeclaration() = this != null && elementType in KtTokenSets.DECLARATION_TYPES
}

public val SPACING_BETWEEN_DECLARATIONS_WITH_COMMENTS_RULE_ID: RuleId = SpacingBetweenDeclarationsWithCommentsRule().ruleId
